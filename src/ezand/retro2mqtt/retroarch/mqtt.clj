(ns ezand.retro2mqtt.retroarch.mqtt)

;;;;;;;;;;;;;;;;;;
;; State Topcis ;;
;;;;;;;;;;;;;;;;;;
(def ^:const topic-retroarch-core "retroarch/core")
(def ^:const topic-retroarch-content "retroarch/content")
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
; TODO