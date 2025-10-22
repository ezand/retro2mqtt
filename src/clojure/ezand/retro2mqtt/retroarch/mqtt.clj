(ns ezand.retro2mqtt.retroarch.mqtt
  (:require [ezand.retro2mqtt.mqtt.core :as mqtt]
            [ezand.retro2mqtt.mqtt.multi-topic :as topics]
            [ezand.retro2mqtt.utils :as util]))

;;;;;;;;;;;;;;;;;;
;; State Topcis ;;
;;;;;;;;;;;;;;;;;;
(def ^:const topic-retroarch-details "retroarch/details")
(def ^:const topic-retroarch-status "retroarch/status")
(def ^:const topic-retroarch-core "retroarch/core")
(def ^:const topic-retroarch-core-details "retroarch/core/details")
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
(def ^:private ^:const topic-homeassistant-retroarch-attributes "homeassistant/sensor/retroarch_attributes")
(def ^:private ^:const topic-homeassistant-core-attributes "homeassistant/sensor/retroarch_core_attributes")
(def ^:private ^:const topic-homeassistant-content-attributes "homeassistant/sensor/retroarch_content_attributes")

;; Entity Configuration
(def ^:private ^:const entity-configurations
  [{:unique_id "retroarch"
    :name "Retroarch"
    :state_topic topic-retroarch-status
    :json_attributes_topic topic-homeassistant-retroarch-attributes
    :attribute-state-topics {topic-retroarch-system-cpu {:key :cpu}
                             topic-retroarch-system-capabilities {:key :capabilities}
                             topic-retroarch-system-display-driver {:key :display-driver}
                             topic-retroarch-system-joypad-driver {:key :joypad-driver}
                             topic-retroarch-system-audio-input-rate {:key :audio-input-rate}
                             topic-retroarch-system-pixel-format {:key :pixel-format}
                             topic-retroarch-libretro-api-version {:key :libretro-api-version}
                             topic-retroarch-version {:key :retroarch-version}
                             topic-retroarch-version-build-date {:key :retroarch-build-date}
                             topic-retroarch-version-git-hash {:key :retroarch-version-git-hash}
                             topic-retroarch-cmd-interface-port {:key :cmd-interface-port}
                             topic-retroarch-details {:key :details :data-type :map}}
    :retain-attributes? true
    :icon "mdi:monitor-star"}
   {:unique_id "retroarch_core"
    :name "Loaded Core"
    :state_topic topic-retroarch-core
    :json_attributes_topic topic-homeassistant-core-attributes
    :attribute-state-topics {topic-retroarch-libretro-core-file {:key :libretro-core-file}
                             topic-retroarch-core-details {:key :details :data-type :map}}
    :icon "mdi:monitor-star"}
   {:unique_id "retroarch_core_last_loaded"
    :name "Last Loaded Core"
    :state_topic topic-retroarch-core-last-loaded
    :json_attributes_topic topic-homeassistant-core-attributes
    :attribute-state-topics {topic-retroarch-libretro-core-file {:key :libretro-core-file}
                             topic-retroarch-core-details {:key :details :data-type :map}}
    :icon "mdi:monitor-star"}
   {:unique_id "retroarch_content"
    :name "Current Game"
    :state_topic topic-retroarch-content
    :json_attributes_topic topic-homeassistant-content-attributes
    :attribute-state-topics {topic-retroarch-content-crc32 {:key :crc32}
                             topic-retroarch-content-video-size {:key :video-size}}
    :icon "mdi:gamepad-variant"}
   {:unique_id "retroarch_content_last_played"
    :name "Last Played Game"
    :state_topic topic-retroarch-content-last-played
    :json_attributes_topic topic-homeassistant-content-attributes
    :attribute-state-topics {topic-retroarch-content-crc32 {:key :crc32}
                             topic-retroarch-content-video-size {:key :video-size}}
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
  ([mqtt-client retroarch-version]
   (publish-homeassistant-discovery! mqtt-client retroarch-version false))
  ([mqtt-client retroarch-version remove-entities?]
   (let [device (retroarch-device-config retroarch-version)
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
