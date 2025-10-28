(ns ezand.retro2mqtt.launchbox.mqtt
  (:require [ezand.retro2mqtt.mqtt.core :as mqtt]
            [ezand.retro2mqtt.mqtt.multi-topic :as topics]
            [ezand.retro2mqtt.utils :as util]))

;;;;;;;;;;;;;;;;;;
;; State Topics ;;
;;;;;;;;;;;;;;;;;;
(defn launchbox-topics
  "Build launchbox topic map from configurable prefix"
  [prefix]
  {:content-loaded (str prefix "/content/loaded")
   :content-running (str prefix "/content/running")
   :content (str prefix "/content")
   :content-last-played (str prefix "/content/last_played")
   :content-details (str prefix "/content/details")
   :emulator-loaded (str prefix "/emulator/loaded")
   :emulator-running (str prefix "/emulator/running")
   :emulator (str prefix "/emulator")
   :emulator-last-loaded (str prefix "/emulator/last_loaded")
   :emulator-details (str prefix "/emulator/details")
   :running (str prefix "/running")
   :details (str prefix "/details")
   :bigbox-running (str prefix "/bigbox/running")
   :bigbox-locked (str prefix "/bigbox/locked")
   :system-event (str prefix "/system/event")})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HomeAssistant Discovery ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Devices
(defn- launchbox-device-config
  [launchbox-version]
  {:identifiers ["launchbox"]
   :name "LaunchBox"
   :manufacturer "Unbroken Software, LLC"
   :model "LaunchBox"
   :sw_version launchbox-version})

;; Topics
(def ^:private ^:const topic-homeassistant-launchbox-attributes "homeassistant/sensor/launchbox_attributes")
(def ^:private ^:const topic-homeassistant-emulator-attributes "homeassistant/sensor/retroarch_emulator_attributes")
(def ^:private ^:const topic-homeassistant-content-attributes "homeassistant/sensor/launchbox_content_attributes")

;; Entity Configuration
(defn- entity-configurations
  "Build entity configurations with dynamic topics"
  [launchbox-topics]
  [{:unique_id "launchbox"
    :name "LaunchBox"
    :state_topic (:running launchbox-topics)
    :json_attributes_topic topic-homeassistant-launchbox-attributes
    :attribute-state-topics {(:details launchbox-topics) {:data-type :map}}
    :retain-attributes? true
    :icon "mdi:monitor-star"}
   {:unique_id "launchbox_bigbox_running"
    :name "Is BigBox Running"
    :state_topic (:bigbox-running launchbox-topics)
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_bigbox_locked"
    :name "Is BigBox Locked"
    :state_topic (:bigbox-locked launchbox-topics)
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_emulator"
    :name "Loaded Emulator"
    :state_topic (:emulator launchbox-topics)
    :json_attributes_topic topic-homeassistant-emulator-attributes
    :attribute-state-topics {(:emulator-details launchbox-topics) {:data-type :map}}
    :icon "mdi:monitor-star"}
   {:unique_id "launchbox_emulator_last_loaded"
    :name "Last Loaded Emulator"
    :state_topic (:emulator-last-loaded launchbox-topics)
    :json_attributes_topic topic-homeassistant-emulator-attributes
    :attribute-state-topics {(:emulator-details launchbox-topics) {:data-type :map}}
    :icon "mdi:monitor-star"}
   {:unique_id "launchbox_emulator_loaded"
    :name "Is Emulator Loaded"
    :state_topic (:emulator-loaded launchbox-topics)
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_emulator_running"
    :name "Is Emulator Running"
    :state_topic (:emulator-running launchbox-topics)
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_content"
    :name "Current Game"
    :state_topic (:content launchbox-topics)
    :json_attributes_topic topic-homeassistant-content-attributes
    :attribute-state-topics {(:content-details launchbox-topics) {:data-type :map}}
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_content_last_played"
    :name "Last Played Game"
    :state_topic (:content-last-played launchbox-topics)
    :json_attributes_topic topic-homeassistant-content-attributes
    :attribute-state-topics {(:content-details launchbox-topics) {:data-type :map}}
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_content_loaded"
    :name "Is Game Loaded"
    :state_topic (:content-loaded launchbox-topics)
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_content_running"
    :name "Is Game Running"
    :state_topic (:content-running launchbox-topics)
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_last_system_event"
    :name "Last LaunchBox System Event"
    :state_topic (:system-event launchbox-topics)
    :icon "mdi:gamepad-variant"}])

;; Publish Configurations
(defn update-main-entity!
  [mqtt-client launchbox-topics launchbox-version]
  (let [device (launchbox-device-config launchbox-version)
        {:keys [unique_id] :as main-entity} (first (entity-configurations launchbox-topics))]
    (mqtt/publish! mqtt-client (util/homeassistant-config-topic unique_id)
                   (assoc main-entity :device device)
                   {:retain true})))

(defn publish-homeassistant-discovery!
  ([mqtt-client launchbox-topics launchbox-version]
   (publish-homeassistant-discovery! mqtt-client launchbox-topics launchbox-version false))
  ([mqtt-client launchbox-topics launchbox-version remove-entities?]
   (let [device (launchbox-device-config launchbox-version)
         entities (map
                    (fn [{:keys [unique_id json_attributes_topic attribute-state-topics retain-attributes?] :as config}]
                      (when (and json_attributes_topic attribute-state-topics)
                        (topics/subscribe-topics!
                          mqtt-client unique_id attribute-state-topics json_attributes_topic retain-attributes?))
                      (-> (assoc config :topic (util/homeassistant-config-topic unique_id)
                                        :device device)
                          (dissoc :attribute-state-topics :retain-attributes?)))
                    (entity-configurations launchbox-topics))]
     (doseq [{:keys [topic] :as entity-config} entities]
       (mqtt/publish! mqtt-client topic
                      (when-not remove-entities? (dissoc entity-config :topic))
                      {:retain true})))))
