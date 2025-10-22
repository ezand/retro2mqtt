(ns ezand.retro2mqtt.mqtt.multi-topic
  (:require [cheshire.core :as json]
            [ezand.retro2mqtt.logger :as log]
            [ezand.retro2mqtt.mqtt.core :as mqtt])
  (:import (java.nio.charset StandardCharsets)))

(def ^:private logger (log/create-logger! *ns*))

;;;;;;;;;;;;
;; States ;;
;;;;;;;;;;;;
(defonce ^:private multi-topic-states (atom {}))

;;;;;;;;;;;;;;;
;; Subscribe ;;
;;;;;;;;;;;;;;;
(defn subscribe-topics!
  [mqtt-client state-identifier topic->cfg target-topic retain?]
  (let [topics (keys topic->cfg)]
    (log/debug logger "Subscribe to multi-topics" {:identifier state-identifier :topics topics})
    (mqtt/subscribe!
      mqtt-client topics :at-least-once
      (fn [topic ^bytes payload]
        ; Global subscriptions, so we need to filter on topics
        (when (get (set topics) topic)
          (try
            (let [data (String. payload StandardCharsets/UTF_8)
                  topic-cfg (get topic->cfg topic)
                  data-type (get topic-cfg :data-type :string)
                  data-kwd (:key topic-cfg)
                  new-data (case data-type
                             :map (json/parse-string data keyword)
                             {data-kwd data})
                  merged-data (merge (get @multi-topic-states state-identifier)
                                     new-data)]
              (swap! multi-topic-states assoc state-identifier merged-data)
              (mqtt/publish! mqtt-client target-topic merged-data {:retain (boolean retain?)}))
            (catch Throwable t
              (.printStackTrace t)
              (throw t))))))))
