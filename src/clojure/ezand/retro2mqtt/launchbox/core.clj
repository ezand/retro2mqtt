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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Provider Implementation                   ;;
;;   launchbox2mqtt handles state publish,   ;;
;;   so nothing to listen for here.          ;;
;;   We only handle HomeAssistant discovery. ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- -start-listening!
  [mqtt-client {{{:keys [discovery?]} :home-assistant} :integrations :as config}]
  (when-not @listening?
    (println (str printer/yellow "♻️ Listening for LaunchBox events" printer/reset))
    (when discovery?
      ; Update HomeAssistant config when LaunchBox version gets published
      (->> (mqtt/subscribe! mqtt-client [launchbox-mqtt/topic-launchbox-details] nil
                            (fn [topic ^bytes payload]
                              (when (= topic launchbox-mqtt/topic-launchbox-details)
                                (let [{:keys [version]} (-> (String. payload StandardCharsets/UTF_8)
                                                            (json/parse-string keyword))]
                                  ;; TODO causes inifite loop??
                                  (launchbox-mqtt/publish-homeassistant-discovery! mqtt-client version false)))))
           (swap! subscriptions conj)))
    (reset! listening? true)))

(defn- -stop-listening!
  [mqtt-client]
  (println (str printer/green "✅ Stopped listening for LaunchBox events" printer/reset))
  (reset! listening? false)
  (when-let [subs (not-empty @subscriptions)]
    (map (partial mqtt/unsubscribe! mqtt-client) subs)))

(defrecord LaunchBoxProvider [mqtt-client config]
  provider/RetroProvider
  (start-listening! [this] (-start-listening! mqtt-client config))
  (stop-listening! [this] (-stop-listening! mqtt-client)))

(defn launchbox-provider
  [mqtt-client {{{:keys [discovery?]} :home-assistant} :integrations :as config}]
  (when discovery?
    ; Start of with version 'Unknown', will be updated by retained message or LaunchBox startup
    (launchbox-mqtt/publish-homeassistant-discovery! mqtt-client "Unknown"))
  (->LaunchBoxProvider mqtt-client config))
