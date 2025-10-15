(ns ezand.retro2mqtt.retroarch.core
  (:require [clojure.java.io :as io]
            [ezand.retro2mqtt.mqtt.core :as mqtt]
            [ezand.retro2mqtt.retroarch.mqtt :as retro-mqtt]
            [ezand.retro2mqtt.printer :as printer]
            [ezand.retro2mqtt.provider :as provider]
            [ezand.retro2mqtt.retroarch.log-tailer :as log])
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
  [mqtt-client {:keys [^File log-dir] :as retroarch-config}]
  (println (str printer/yellow "♻️ Listening for RetroArch events" printer/reset))
  (reset! listening? true)
  (-> (log/tail-log-file! listening? log-dir {:publish-fn (partial publish-event! mqtt-client)})
      (future)))

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
  (when discovery?
    (retro-mqtt/publish-homeassistant-discovery! mqtt-client))
  (let [config (update-in config [:retroarch :log-dir] io/file)]
    (->RetroarchProvider mqtt-client (:retroarch config))))

;;;;;;;;;;;;;
;; Testing ;;
;;;;;;;;;;;;;
(comment
  (do (require '[config.core :as cfg])
      (def config* (:retro2mqtt cfg/env))
      (def mqtt-client* (-> (mqtt/create-client (:mqtt config*))
                            (mqtt/connect! (:mqtt config*))))
      (def retroarch-config* {:log-dir (io/file (System/getenv "RETROARCH_LOG_DIR"))}))

  (-start-listening! mqtt-client* retroarch-config*)

  (-stop-listening!))
