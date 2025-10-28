(ns ezand.retro2mqtt.audio.mqtt
  (:require [ezand.retro2mqtt.mqtt.core :as mqtt]
            [ezand.retro2mqtt.mqtt.multi-topic :as topics]
            [ezand.retro2mqtt.utils :as util]))

;;;;;;;;;;;;;;;;;;
;; State Topics ;;
;;;;;;;;;;;;;;;;;;
(def ^:const topic-audio-system-running? "audio_events/system/running")
(def ^:const topic-audio-system-details "audio_events/system/details")
(def ^:const topic-audio-event "audio_events/event")
(def ^:const topic-audio-event-last-song "audio_events/event/last_song")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HomeAssistant Discovery ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Devices
(defn- audio-device-config
  [{:keys [version] :as system-details}]
  {:identifiers ["audio2mqtt"]
   :name "Audio Events"
   :manufacturer "Eirik Sand"
   :model "audio2mqtt"
   :sw_version (or version "Unknown")})

;; Topics
(def ^:private ^:const topic-homeassistant-audio-attributes "homeassistant/sensor/audio2mqtt_attributes")
(def ^:private ^:const topic-homeassistant-audio-event-attributes "homeassistant/sensor/audio2mqtt_event_attributes")

;; Entity Configuration
(def ^:private ^:const entity-configurations
  [{:unique_id "audio2mqtt"
    :name "audio2mqtt"
    :state_topic topic-audio-system-running?
    :json_attributes_topic topic-homeassistant-audio-attributes
    :attribute-state-topics {topic-audio-system-details {:data-type :map}}
    :retain-attributes? true
    :icon "mdi:volume-high"}
   {:unique_id "audio2mqtt_last_audio_event"
    :name "Last Audio Event"
    :state_topic topic-audio-event-last-song
    :json_attributes_topic topic-homeassistant-audio-event-attributes
    :attribute-state-topics {topic-audio-event
                             {:data-type :map
                              :transform-fn (fn [{:keys [metadata] :as audio-event}]
                                              (-> (dissoc audio-event :metadata)
                                                  (merge metadata)))}}
    :retain-attributes? true
    :icon "mdi:volume-high"}])

;; Publish Configurations
(defn update-main-entity!
  [mqtt-client system-details]
  (let [device (audio-device-config system-details)
        {:keys [unique_id] :as main-entity} (first entity-configurations)]
    (mqtt/publish! mqtt-client (util/homeassistant-config-topic unique_id)
                   (assoc main-entity :device device)
                   {:retain true})))

(defn publish-homeassistant-discovery!
  ([mqtt-client system-details]
   (publish-homeassistant-discovery! mqtt-client system-details false))
  ([mqtt-client system-details remove-entities?]
   (let [device (audio-device-config system-details)
         entities (map
                    (fn [{:keys [unique_id json_attributes_topic attribute-state-topics retain-attributes?] :as config}]
                      (when (and json_attributes_topic attribute-state-topics)
                        (topics/subscribe-topics!
                          mqtt-client unique_id attribute-state-topics json_attributes_topic retain-attributes?))
                      (-> (assoc config :topic (util/homeassistant-config-topic unique_id)
                                        :device device)
                          (dissoc :attribute-state-topics :retain-attributes?)))
                    entity-configurations)]
     (doseq [{:keys [topic] :as entity-config} entities]
       (mqtt/publish! mqtt-client topic
                      (when-not remove-entities? (dissoc entity-config :topic))
                      {:retain true})))))