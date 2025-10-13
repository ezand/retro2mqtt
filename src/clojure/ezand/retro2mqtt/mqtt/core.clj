(ns ezand.retro2mqtt.mqtt.core
  (:require [cheshire.core :as json]
            [ezand.retro2mqtt.utils :as util]
            [superstring.core :as str])
  (:import (com.hivemq.client.mqtt MqttClient MqttGlobalPublishFilter)
           (com.hivemq.client.mqtt.datatypes MqttQos)
           (com.hivemq.client.mqtt.mqtt5 Mqtt5BlockingClient)
           (com.hivemq.client.mqtt.mqtt5.message.publish Mqtt5Publish)
           (com.hivemq.client.mqtt.mqtt5.message.subscribe Mqtt5Subscription)
           (com.hivemq.client.mqtt.mqtt5.message.subscribe.suback Mqtt5SubAck)
           (java.nio.charset StandardCharsets)
           (java.util UUID)))

;;;;;;;;;;;
;; Utils ;;
;;;;;;;;;;;
(defn- ensure-string-payload
  [payload]
  (cond-> payload
    (string? payload) identity
    (number? payload) str
    (boolean? payload) util/bool->toggle-str
    (coll? payload) json/generate-string
    :else str))

(defn- parse-payload
  "Parse payload bytes to string and optionally parse JSON."
  [payload-bytes parse-json?]
  (when payload-bytes
    (let [payload-str (String. payload-bytes StandardCharsets/UTF_8)]
      (if parse-json?
        (try
          (json/parse-string payload-str true)
          (catch Exception _
            payload-str))
        payload-str))))

(def ^:private kwd->qos
  {:exactly-once MqttQos/EXACTLY_ONCE
   :at-least-once MqttQos/AT_LEAST_ONCE
   :at-most-once MqttQos/AT_MOST_ONCE})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Client Creation and Connection ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn create-client
  "Create an MQTT client.

  Options:
  - :host - MQTT broker host (default: localhost)
  - :port - MQTT broker port (default: 1883)
  - :client-id - Client identifier (default: auto-generated)"
  [{:keys [host port client-id]
    :or {host "localhost"
         port 1883
         client-id (str "retroarch_listener_" (UUID/randomUUID))}}]
  (-> (MqttClient/builder)
      (.useMqttVersion5)
      (.serverHost ^String host)
      (.serverPort port)
      (.identifier ^String client-id)
      (.buildBlocking)))

(defn connect!
  ([^Mqtt5BlockingClient mqtt-client]
   (.connect mqtt-client)
   mqtt-client)
  ([^Mqtt5BlockingClient mqtt-client ^String username ^String password]
   (if (and username password)
     (-> (.connectWith mqtt-client)
         (.simpleAuth)
         (.username username)
         (.password (.getBytes password StandardCharsets/UTF_8))
         (.applySimpleAuth)
         (.send))
     (.connect mqtt-client))
   mqtt-client))

(defn disconnect!
  "Disconnect from MQTT broker."
  [^Mqtt5BlockingClient mqtt-client]
  (.disconnect mqtt-client))

;;;;;;;;;;;;;
;; Publish ;;
;;;;;;;;;;;;;
(defn publish!
  "Publish a message to a topic.

  Options:
  - :qos - Quality of Service level (0, 1, or 2, default: 0)
  - :retain - Retain message flag (default: false)"
  ([mqtt-client topic payload]
   (publish! mqtt-client topic payload {}))
  ([^Mqtt5BlockingClient mqtt-client ^String topic payload
    {:keys [qos retain?]
     :or {qos 0
          retain? false}}]
   (let [qos-level (case qos
                     0 MqttQos/AT_MOST_ONCE
                     1 MqttQos/AT_LEAST_ONCE
                     2 MqttQos/EXACTLY_ONCE)
         payload-bytes (when payload (.getBytes ^String (ensure-string-payload payload) StandardCharsets/UTF_8))]
     (try (-> mqtt-client
              (.publishWith)
              (.topic topic)
              (.payload payload-bytes)
              (.qos qos-level)
              (.retain retain?)
              (.send))
          (catch Throwable t
            (.printStackTrace t)
            (throw t))))))

;;;;;;;;;;;;;;;
;; Subscribe ;;
;;;;;;;;;;;;;;;
(defn subscribe!
  [^Mqtt5BlockingClient mqtt-client topics qos callback-fn]
  (try
    (let [subscriptions (->> (map #(-> (Mqtt5Subscription/builder)
                                       (.topicFilter ^String %)
                                       (.qos (get kwd->qos qos MqttQos/AT_LEAST_ONCE))
                                       (.build))
                                  topics))
          ^Mqtt5SubAck sub-ack (-> (.subscribeWith mqtt-client)
                                   (.addSubscriptions subscriptions)
                                   (.send))]
      (println (format "Subscribed to %s with return codes: %s" (str/join "," topics)
                       (map #(.name %) (.getReasonCodes sub-ack))))

      (-> (.toAsync mqtt-client)
          (.publishes MqttGlobalPublishFilter/ALL
                      (fn [^Mqtt5Publish publish]
                        (callback-fn (.getPayloadAsBytes publish))))))
    (catch Throwable t
      (.printStackTrace t)
      (throw t))))

(defn unsubscribe!
  "Unsubscribe from a topic."
  [^Mqtt5BlockingClient mqtt-client ^String topic]
  (-> mqtt-client
      (.unsubscribeWith)
      (.topicFilter topic)
      (.send)))

;;;;;;;;;;;;;;;;;;;;;;;;;
;; Multi-Topic Support ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;
;; Testing ;;
;;;;;;;;;;;;;
(comment
  (def mqtt-client* (-> (create-client {:client-id (str "testing_" (rand-int 10000))})
                        (connect! "mosquitto" "mosquitto")))

  (subscribe! mqtt-client* ["retroarch/content/crc32"] :at-least-once
              (fn [x] (println "XXXXX" (String. x StandardCharsets/UTF_8))))

  (unsubscribe! mqtt-client* "retroarch/content/crc32"))
