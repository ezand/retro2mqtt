(ns ezand.retro2mqtt.retroarch.core
  (:require [clojure.java.io :as io]
            [ezand.retro2mqtt.mqtt.core :as mqtt]
            [ezand.retro2mqtt.printer :as printer]
            [ezand.retro2mqtt.provider :as provider]
            [ezand.retro2mqtt.retroarch.udp-connector :as retro-udp]
            [ezand.retro2mqtt.retroarch.config-extractor :as config-extractor]
            [ezand.retro2mqtt.retroarch.log-tailer :as log]
            [ezand.retro2mqtt.retroarch.mqtt :as retro-mqtt])
  (:import (java.io File)))

;;;;;;;;;;;
;; State ;;
;;;;;;;;;;;
(defonce listening? (atom false))

;;;;;;;;;;;;;;;;;;;;
;; Event Handling ;;
;;;;;;;;;;;;;;;;;;;;
(defn- publish-event!
  [mqtt-client topic data retain?]
  (mqtt/publish! mqtt-client topic data {:retain? (boolean retain?)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Provider Implementation ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- -start-listening!
  [mqtt-client {:keys [^File log-dir ^File config-dir] :as retroarch-config}]
  (when-not @listening?
    (println (str printer/yellow "♻️ Listening for RetroArch events" printer/reset))
    (reset! listening? true)

    (when-let [config-details (config-extractor/config-details config-dir)]
      (publish-event! mqtt-client retro-mqtt/topic-retroarch-details config-details true))

    (when-let [udp-config (:udp retroarch-config)]
      (when-let [retroarch-version (retro-udp/retroarch-version udp-config)]
        (publish-event! mqtt-client retro-mqtt/topic-retroarch-version retroarch-version true)))

    (-> (log/tail-log-file! listening? log-dir {:publish-fn (partial publish-event! mqtt-client)})
        (future))))

(defn- -stop-listening!
  []
  (reset! listening? false)
  (println (str printer/green "✅ Stopped listening for RetroArch events" printer/reset)))

(defrecord RetroarchProvider [mqtt-client config]
  provider/RetroProvider
  (start-listening! [this] (-start-listening! mqtt-client config))
  (stop-listening! [this] (-stop-listening!)))

(defn retroarch-provider
  [mqtt-client {{{:keys [discovery?]} :home-assistant} :integrations :as config}]
  (let [retroarch-version (when-let [udp-config (-> config :retroarch :udp)]
                            (retro-udp/retroarch-version udp-config))]
    (publish-event! mqtt-client retro-mqtt/topic-retroarch-version retroarch-version true)
    (when discovery?
      (retro-mqtt/publish-homeassistant-discovery! mqtt-client retroarch-version)))

  (let [config (-> (update-in config [:retroarch :log-dir] io/file)
                   (update-in [:retroarch :config-dir] io/file))]
    (->RetroarchProvider mqtt-client (:retroarch config))))

;;;;;;;;;;;;;
;; Testing ;;
;;;;;;;;;;;;;
(comment
  (do (require '[config.core :as cfg])
      (def config* (:retro2mqtt cfg/env))
      (def mqtt-client* (-> (mqtt/create-client (:mqtt config*))
                            (mqtt/connect! (:mqtt config*))))
      (def retroarch-config* {:log-dir (io/file (System/getenv "RETROARCH_LOG_DIR"))
                              :config-dir (io/file (System/getenv "RETROARCH_CONFIG_DIR"))}))

  (-start-listening! mqtt-client* retroarch-config*)

  (-stop-listening!))
