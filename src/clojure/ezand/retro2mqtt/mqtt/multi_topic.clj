(ns ezand.retro2mqtt.mqtt.multi-topic
  (:require [ezand.retro2mqtt.mqtt.core :as mqtt]
            [superstring.core :as str])
  (:import (java.nio.charset StandardCharsets)))

;;;;;;;;;;;;
;; States ;;
;;;;;;;;;;;;
(defonce ^:private multi-topic-states (atom {}))

;;;;;;;;;;;;;;;
;; Subscribe ;;
;;;;;;;;;;;;;;;
(defn subscribe-topics!
  [mqtt-client state-identifier topics target-topic retain?]
  (println (format "Subscribe to multi-topics [%s]: %s" (name state-identifier) (str/join "," topics)))
  (mqtt/subscribe!
    mqtt-client topics :at-least-once
    (fn [topic ^bytes payload]
      ; Global subscriptions, so we need to filter on topics
      (when (get (set topics) topic)
        (try
          (let [data (String. payload StandardCharsets/UTF_8)
                data-kwd (-> (subs topic (inc (str/last-index-of topic "/")))
                             (str/lisp-case)
                             (keyword))
                new-data (->> {data-kwd data}
                              (merge (get @multi-topic-states state-identifier)))]
            (swap! multi-topic-states assoc state-identifier new-data)
            (mqtt/publish! mqtt-client target-topic new-data {:retain retain?}))
          (catch Throwable t
            (.printStackTrace t)
            (throw t)))))))

;;;;;;;;;;;;;
;; Testing ;;
;;;;;;;;;;;;;
(comment
  (def mqtt-client* (-> (mqtt/create-client {:client-id (str "testing_" (rand-int 10000))})
                        (mqtt/connect! "mosquitto" "mosquitto")))

  (subscribe-topics! mqtt-client* :attributes
                     ["retroarch/content/crc32" "retroarch/content/video_size"]
                     "homeassistant/retroarch/attributes"
                     false))