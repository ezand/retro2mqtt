(ns ezand.retro2mqtt.launchbox.core
  (:require [cheshire.core :as json]
            [ezand.retro2mqtt.launchbox.mqtt :as launchbox-mqtt]
            [ezand.retro2mqtt.mqtt.core :as mqtt]
            [ezand.retro2mqtt.printer :as printer]
            [ezand.retro2mqtt.provider :as provider])
  (:import (java.nio.charset StandardCharsets)))

;;;;;;;;;;;
;; State ;;
;;;;;;;;;;;
(defonce listening? (atom false))
(defonce subscriptions (atom []))
(defonce system-details (atom {}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Provider Implementation                   ;;
;;   launchbox2mqtt handles state publish,   ;;
;;   so nothing to listen for here.          ;;
;;   We only handle HomeAssistant discovery. ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- -start-listening!
  [mqtt-client launchbox-topics {{{:keys [discovery?]} :home-assistant} :integrations :as config}]
  (when-not @listening?
    (println (str printer/yellow "♻️ Listening for LaunchBox events" printer/reset))
    (when discovery?
      ; Update HomeAssistant config when LaunchBox version gets published
      (let [details-topic (:details launchbox-topics)]
        (->> (mqtt/subscribe! mqtt-client [details-topic] nil
                              (fn [topic ^bytes payload]
                                (when (= topic details-topic)
                                  (let [{:keys [version] :as details} (-> (String. payload StandardCharsets/UTF_8)
                                                                          (json/parse-string keyword))]
                                    (when-not (= @system-details details)
                                      (reset! system-details details)
                                      (launchbox-mqtt/update-main-entity! mqtt-client launchbox-topics version))))))
             (swap! subscriptions conj))))
    (reset! listening? true)))

(defn- -stop-listening!
  [mqtt-client]
  (println (str printer/green "✅ Stopped listening for LaunchBox events" printer/reset))
  (reset! listening? false)
  (when-let [subs (not-empty @subscriptions)]
    (map (partial mqtt/unsubscribe! mqtt-client) subs)))

(defrecord LaunchBoxProvider [mqtt-client launchbox-topics config]
  provider/RetroProvider
  (start-listening! [this] (-start-listening! mqtt-client launchbox-topics config))
  (stop-listening! [this] (-stop-listening! mqtt-client)))

(defn launchbox-provider
  [mqtt-client {{{:keys [discovery?]} :home-assistant} :integrations
                {:keys [topic-prefix] :or {topic-prefix "launchbox"}} :launchbox
                :as config}]
  (let [launchbox-topics (launchbox-mqtt/launchbox-topics topic-prefix)]
    (when discovery?
      ; Start of with version 'Unknown', will be updated by retained message or LaunchBox startup
      (launchbox-mqtt/publish-homeassistant-discovery! mqtt-client launchbox-topics "Unknown"))
    (->LaunchBoxProvider mqtt-client launchbox-topics config)))
