(ns ezand.retro2mqtt.retroarch.mqtt
  (:require [ezand.retro2mqtt.mqtt.core :as mqtt]
            [ezand.retro2mqtt.mqtt.multi-topic :as topics]
            [ezand.retro2mqtt.utils :as util]))

;;;;;;;;;;;;;;;;;;
;; State Topcis ;;
;;;;;;;;;;;;;;;;;;
(def ^:const topic-retroarch-status "retroarch/status")
(def ^:const topic-retroarch-core "retroarch/core")
(def ^:const topic-retroarch-core-last-loaded "retroarch/core/last_loaded")
(def ^:const topic-retroarch-content "retroarch/content")
(def ^:const topic-retroarch-content-last-played "retroarch/content/last_played")
(def ^:const topic-retroarch-content-loaded? "retroarch/content/loaded")
(def ^:const topic-retroarch-content-running? "retroarch/content/running")
(def ^:const topic-retroarch-content-crc32 "retroarch/content/crc32")
(def ^:const topic-retroarch-content-video-size "retroarch/content/video_size")
(def ^:const topic-retroarch-system-cpu "retroarch/system/cpu")
(def ^:const topic-retroarch-system-capabilities "retroarch/system/capabilities")
(def ^:const topic-retroarch-system-display-driver "retroarch/system/display/driver")
(def ^:const topic-retroarch-system-joypad-driver "retroarch/system/joypad/driver")
(def ^:const topic-retroarch-system-audio-input-rate "retroarch/system/audio/input_rate")
(def ^:const topic-retroarch-system-pixel-format "retroarch/system/pixel_format")
(def ^:const topic-retroarch-libretro-api-version "retroarch/libretro/version")
(def ^:const topic-retroarch-libretro-core-file "retroarch/libretro/core_file")
(def ^:const topic-retroarch-version "retroarch/version")
(def ^:const topic-retroarch-version-build-date "retroarch/version/build_date")
(def ^:const topic-retroarch-version-git-hash "retroarch/version/git_hash")
(def ^:const topic-retroarch-cmd-interface-port "retroarch/cmd_interface_port")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HomeAssistant Discovery ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Devices
(defn- retroarch-device-config
  [retroarch-version]
  {:identifiers ["retroarch"]
   :name "RetroArch"
   :manufacturer "Libretro"
   :model "RetroArch"
   :sw_version retroarch-version})

;; Topics
(def ^:privat ^:const topic-homeassistant-retroarch-attributes "homeassistant/sensor/retroarch_attributes")
(def ^:privat ^:const topic-homeassistant-core-attributes "homeassistant/sensor/retroarch_core_attributes")
(def ^:privat ^:const topic-homeassistant-content-attributes "homeassistant/sensor/retroarch_content_attributes")

;; Entity Configuration
(def ^:const ^:private entity-configurations
  [{:unique_id "retroarch"
    :name "Retroarch"
    :state_topic topic-retroarch-status
    :json_attributes_topic topic-homeassistant-retroarch-attributes
    :attribute-state-topics [topic-retroarch-system-cpu
                             topic-retroarch-system-capabilities
                             topic-retroarch-system-display-driver
                             topic-retroarch-system-joypad-driver
                             topic-retroarch-system-audio-input-rate
                             topic-retroarch-system-pixel-format
                             topic-retroarch-libretro-api-version
                             topic-retroarch-version
                             topic-retroarch-version-build-date
                             topic-retroarch-version-git-hash
                             topic-retroarch-cmd-interface-port]
    :icon "mdi:monitor-star"}
   {:unique_id "retroarch_core"
    :name "Loaded Core"
    :state_topic topic-retroarch-core
    :json_attributes_topic topic-homeassistant-core-attributes
    :attribute-state-topics [topic-retroarch-libretro-core-file]
    :icon "mdi:monitor-star"}
   {:unique_id "retroarch_core_last_loaded"
    :name "Last Loaded Core"
    :state_topic topic-retroarch-core-last-loaded
    :icon "mdi:monitor-star"}
   {:unique_id "retroarch_content"
    :name "Current Game"
    :state_topic topic-retroarch-content
    :json_attributes_topic topic-homeassistant-content-attributes
    :attribute-state-topics [topic-retroarch-content-crc32
                             topic-retroarch-content-video-size]
    :icon "mdi:gamepad-variant"}
   {:unique_id "retroarch_content_last_played"
    :name "Last Played Game"
    :state_topic topic-retroarch-content-last-played
    :icon "mdi:gamepad-variant"}
   {:unique_id "retroarch_content_loaded"
    :name "Is Game Loaded"
    :state_topic topic-retroarch-content-loaded?
    :icon "mdi:gamepad-variant"}
   {:unique_id "retroarch_content_running"
    :name "Is Game Running"
    :state_topic topic-retroarch-content-running?
    :icon "mdi:gamepad-variant"}])

;; Publish Configurations
(defn publish-homeassistant-discovery!
  ([mqtt-client]
   (publish-homeassistant-discovery! mqtt-client false))
  ([mqtt-client remove-entities?]
   ; TODO get retroarch version initially
   (let [device (retroarch-device-config "")
         entities (map (fn [{:keys [unique_id json_attributes_topic attribute-state-topics] :as config}]
                         (when (and json_attributes_topic attribute-state-topics)
                           (topics/subscribe-topics!
                             mqtt-client unique_id attribute-state-topics json_attributes_topic true))
                         (-> (assoc config :topic (util/homeassistant-config-topic unique_id)
                                       :device device)
                             (dissoc :attribute-state-topics)))
                       entity-configurations)]
     (doseq [{:keys [topic] :as entity-config} entities]
       (mqtt/publish! mqtt-client topic
                      (when-not remove-entities? (dissoc entity-config :topic))
                      {:retain true})))))

;;;;;;;;;;;;;
;; Testing ;;
;;;;;;;;;;;;;
(comment
  (def mqtt-client* (-> (mqtt/create-client {:client-id (str "testing_" (rand-int 10000))})
                        (mqtt/connect! "mosquitto" "mosquitto")))

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
      (mqtt/publish! mqtt-client* topic-homeassistant-retroarch-attributes nil {:retain true})
      (mqtt/publish! mqtt-client* topic-homeassistant-core-attributes nil {:retain true})
      (mqtt/publish! mqtt-client* topic-homeassistant-content-attributes nil {:retain true})))
