(ns ezand.retro2mqtt.audio.core
  (:require [cheshire.core :as json]
            [ezand.retro2mqtt.audio.mqtt :as audio-mqtt]
            [ezand.retro2mqtt.logger :as log]
            [ezand.retro2mqtt.mqtt.core :as mqtt]
            [ezand.retro2mqtt.printer :as printer]
            [ezand.retro2mqtt.provider :as provider])
  (:import (java.nio.charset StandardCharsets)))

(def ^:private logger (log/create-logger! *ns*))

;;;;;;;;;;;
;; State ;;
;;;;;;;;;;;
(defonce listening? (atom false))
(defonce subscriptions (atom []))
(defonce system-details (atom {}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Provider Implementation                 ;;
;;   audio2mqtt publishes audio events.    ;;
;;   We need to convert to state messages. ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- -start-listening!
  [mqtt-client audio-topics {{{:keys [discovery?]} :home-assistant} :integrations :as audio-config}]
  (when-not @listening?
    (println (str printer/yellow "♻️ Listening for audio events" printer/reset))
    (when discovery?
      ; Update HomeAssistant config when audio2mqtt version gets published
      (let [system-details-topic (:system-details audio-topics)]
        (->> (mqtt/subscribe! mqtt-client [system-details-topic] nil
                              (fn [topic ^bytes payload]
                                (when (= topic system-details-topic)
                                  (let [{:keys [version] :as details} (-> (String. payload StandardCharsets/UTF_8)
                                                                          (json/parse-string keyword))]
                                    (when-not (= (dissoc @system-details :timestamp)
                                                 (dissoc details :timestamp))
                                      (reset! system-details details)
                                      (log/debug logger "System details changed, updating entity" {:version version})
                                      (audio-mqtt/update-main-entity! mqtt-client audio-topics @system-details))))))
             (swap! subscriptions conj))))
    (reset! listening? true)))

(defn- -stop-listening!
  [mqtt-client]
  (println (str printer/green "✅ Stopped listening for audio events" printer/reset))
  (reset! listening? false)
  (when-let [subs (not-empty @subscriptions)]
    (map (partial mqtt/unsubscribe! mqtt-client) subs)))

(defrecord AudioProvider [mqtt-client audio-topics config]
  provider/RetroProvider
  (start-listening! [this] (-start-listening! mqtt-client audio-topics config))
  (stop-listening! [this] (-stop-listening! mqtt-client)))

(defn audio-provider
  [mqtt-client {{{:keys [discovery?]} :home-assistant} :integrations
                {:keys [topic-prefix] :or {topic-prefix "audio_events"}} :audio
                :as config}]
  (let [audio-topics (audio-mqtt/audio-topics topic-prefix)]
    (when discovery?
      ; Start of with empty system detail, will be updated by retained message or audio2mqtt startup
      (audio-mqtt/publish-homeassistant-discovery! mqtt-client audio-topics {}))
    (->AudioProvider mqtt-client audio-topics config)))
