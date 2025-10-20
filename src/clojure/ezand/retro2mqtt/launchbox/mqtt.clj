(ns ezand.retro2mqtt.launchbox.mqtt
  (:require [ezand.retro2mqtt.mqtt.core :as mqtt]
            [ezand.retro2mqtt.mqtt.multi-topic :as topics]
            [ezand.retro2mqtt.utils :as util]))

;;;;;;;;;;;;;;;;;;
;; State Topcis ;;
;;;;;;;;;;;;;;;;;;
(def ^:const topic-launchbox-content-loaded? "launchbox/content/loaded")
(def ^:const topic-launchbox-content-running? "launchbox/content/running")
(def ^:const topic-launchbox-content "launchbox/content")
(def ^:const topic-launchbox-content-last-played "launchbox/content/last_played")
(def ^:const topic-launchbox-content-details "launchbox/content/details")
(def ^:const topic-launchbox-emulator-loaded? "launchbox/emulator/loaded")
(def ^:const topic-launchbox-emulator-running? "launchbox/emulator/running")
(def ^:const topic-launchbox-emulator "launchbox/emulator")
(def ^:const topic-launchbox-emulator-last-loaded "launchbox/emulator/last_loaded")
(def ^:const topic-launchbox-emulator-details "launchbox/emulator/details")
(def ^:const topic-launchbox-running? "launchbox/running")
(def ^:const topic-launchbox-details "launchbox/details")
(def ^:const topic-launchbox-bigbox-running? "launchbox/bigbox/running")
(def ^:const topic-launchbox-bigbox-locked? "launchbox/bigbox/locked")
(def ^:const topic-launchbox-system-event? "launchbox/system/event")

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
(def ^:private ^:const entity-configurations
  [{:unique_id "launchbox"
    :name "LaunchBox"
    :state_topic topic-launchbox-running?
    :json_attributes_topic topic-homeassistant-launchbox-attributes
    :attribute-state-topics {topic-launchbox-details {:data-type :map}}
    :retain-attributes? true
    :icon "mdi:monitor-star"}
   {:unique_id "launchbox_bigbox_running"
    :name "Is BigBox Running"
    :state_topic topic-launchbox-bigbox-running?
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_bigbox_locked"
    :name "Is BigBox Locked"
    :state_topic topic-launchbox-bigbox-locked?
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_emulator"
    :name "Loaded Emulator"
    :state_topic topic-launchbox-emulator
    :json_attributes_topic topic-homeassistant-emulator-attributes
    :attribute-state-topics {topic-launchbox-emulator-details {:data-type :map}}
    :icon "mdi:monitor-star"}
   {:unique_id "launchbox_emulator_last_loaded"
    :name "Last Loaded Emulator"
    :state_topic topic-launchbox-emulator-last-loaded
    :json_attributes_topic topic-homeassistant-emulator-attributes
    :attribute-state-topics {topic-launchbox-emulator-details {:data-type :map}}
    :icon "mdi:monitor-star"}
   {:unique_id "launchbox_emulator_loaded"
    :name "Is Emulator Loaded"
    :state_topic topic-launchbox-emulator-loaded?
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_emulator_running"
    :name "Is Emulator Running"
    :state_topic topic-launchbox-emulator-running?
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_content"
    :name "Current Game"
    :state_topic topic-launchbox-content
    :json_attributes_topic topic-homeassistant-content-attributes
    :attribute-state-topics {topic-launchbox-content-details {:data-type :map}}
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_content_last_played"
    :name "Last Played Game"
    :state_topic topic-launchbox-content-last-played
    :json_attributes_topic topic-homeassistant-content-attributes
    :attribute-state-topics {topic-launchbox-content-details {:data-type :map}}
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_content_loaded"
    :name "Is Game Loaded"
    :state_topic topic-launchbox-content-loaded?
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_content_running"
    :name "Is Game Running"
    :state_topic topic-launchbox-content-running?
    :icon "mdi:gamepad-variant"}
   {:unique_id "launchbox_last_system_event"
    :name "Last LaunchBox System Event"
    :state_topic topic-launchbox-system-event?
    :icon "mdi:gamepad-variant"}])

;; Publish Configurations
(defn publish-homeassistant-discovery!
  ([mqtt-client launchbox-version]
   (publish-homeassistant-discovery! mqtt-client launchbox-version false))
  ([mqtt-client launchbox-version remove-entities?]
   (let [device (launchbox-device-config launchbox-version)
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

;;;;;;;;;;;;;
;; Testing ;;
;;;;;;;;;;;;;
#_(comment
    (do (require '[config.core :as cfg])
        (def config* (:retro2mqtt cfg/env))
        (def mqtt-client* (-> (mqtt/create-client (:mqtt config*))
                              (mqtt/connect! (:mqtt config*)))))

    (mqtt/publish! mqtt-client* topic-retroarch-content-loaded? true)
    (mqtt/publish! mqtt-client* topic-retroarch-content-loaded? false)

    (do (mqtt/publish! mqtt-client* topic-retroarch-status "running")
        (mqtt/publish! mqtt-client* topic-retroarch-core "snes")
        (mqtt/publish! mqtt-client* topic-retroarch-core-last-loaded "snes")
        (mqtt/publish! mqtt-client* topic-retroarch-content "Super Mario")
        (mqtt/publish! mqtt-client* topic-retroarch-content-last-played "Street Fighter")
        (mqtt/publish! mqtt-client* topic-retroarch-content-loaded? true)
        (mqtt/publish! mqtt-client* topic-retroarch-content-running? true)
        (mqtt/publish! mqtt-client* topic-retroarch-content-crc32 "abc123")
        (mqtt/publish! mqtt-client* topic-retroarch-content-video-size "700x600")
        (mqtt/publish! mqtt-client* topic-retroarch-system-cpu "Apple Max 2")
        (mqtt/publish! mqtt-client* topic-retroarch-system-capabilities ["sna" "fu"])
        (mqtt/publish! mqtt-client* topic-retroarch-system-display-driver "apple")
        (mqtt/publish! mqtt-client* topic-retroarch-system-joypad-driver "acme")
        (mqtt/publish! mqtt-client* topic-retroarch-system-audio-input-rate "44000 hz")
        (mqtt/publish! mqtt-client* topic-retroarch-system-pixel-format "baz")
        (mqtt/publish! mqtt-client* topic-retroarch-libretro-api-version "1")
        (mqtt/publish! mqtt-client* topic-retroarch-libretro-core-file "snes.dylib")
        (mqtt/publish! mqtt-client* topic-retroarch-version "1.2.3")
        (mqtt/publish! mqtt-client* topic-retroarch-version-build-date "1. Aug 2022")
        (mqtt/publish! mqtt-client* topic-retroarch-version-git-hash "abc123")
        (mqtt/publish! mqtt-client* topic-retroarch-cmd-interface-port 53553))

    ; Add entities
    (publish-homeassistant-discovery! mqtt-client*)

    ; Remove entities
    (do (publish-homeassistant-discovery! mqtt-client* true)
        (mqtt/publish! mqtt-client* topic-homeassistant-launchbox-attributes nil {:retain true})
        (mqtt/publish! mqtt-client* topic-homeassistant-emulator-attributes nil {:retain true})
        (mqtt/publish! mqtt-client* topic-homeassistant-content-attributes nil {:retain true})))