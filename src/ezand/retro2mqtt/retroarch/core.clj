(ns ezand.retro2mqtt.retroarch.core
  (:require [ezand.retro2mqtt.mqtt.core :as mqtt]
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
  [mqtt-client config]
  (->RetroarchProvider mqtt-client config))

;;;;;;;;;;;;;
;; Testing ;;
;;;;;;;;;;;;;
(comment
  (do (require '[clojure.java.io :as io])
      (def mqtt-client* (-> (mqtt/create-client {:client-id (str "testing_" (rand-int 10000))})
                            (mqtt/connect! "mosquitto" "mosquitto")))
      (def retroarch-config* {:log-dir (io/file (System/getenv "RETROARCH_LOG_DIR"))}))

  (-start-listening! mqtt-client* retroarch-config*)

  (-stop-listening!))
