(ns ezand.retro2mqtt.retroarch.core
  (:require [ezand.retro2mqtt.provider :as provider]
            [ezand.retro2mqtt.retroarch.log-tailer :as log]
            [ezand.retro2mqtt.mqtt.core :as mqtt]))

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
  [mqtt-client {:keys [log-dir] :as retroarch-config}]
  (reset! listening? true)
  (-> (log/tail-log-file! listening? log-dir {:publish-fn (partial publish-event! mqtt-client)})
      (future)))

(defn- -stop-listening!
  []
  (reset! listening? false))

(defrecord RetroarchProvider [mqtt-client config]
  provider/RetroProvider
  (start-listening! [this] (-start-listening! mqtt-client config))
  (stop-listening! [this] (-stop-listening!)))

(defn retroarch-provider
  [mqtt-client config]
  (->RetroarchProvider mqtt-client config))
