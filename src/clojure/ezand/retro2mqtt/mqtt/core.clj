(ns ezand.retro2mqtt.mqtt.core
  (:require [cheshire.core :as json]
            [ezand.retro2mqtt.utils :as util])
  (:import (com.hivemq.client.mqtt MqttClient MqttGlobalPublishFilter)
           (com.hivemq.client.mqtt.datatypes MqttQos)
           (com.hivemq.client.mqtt.mqtt5 Mqtt5BlockingClient)
           (com.hivemq.client.mqtt.mqtt5.message.publish Mqtt5Publish)
           (com.hivemq.client.mqtt.mqtt5.message.subscribe Mqtt5Subscription)
           (com.hivemq.client.mqtt.mqtt5.message.unsubscribe Mqtt5Unsubscribe)
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
  [{:keys [host port client-id-prefix]
    :or {host "localhost"
         port 1883
         client-id-prefix "retroarch_listener_"}}]
  (-> (MqttClient/builder)
      (.useMqttVersion5)
      (.serverHost ^String host)
      (.serverPort port)
      (.identifier ^String (str client-id-prefix (UUID/randomUUID)))
      (.buildBlocking)))

(defn connect!
  ([^Mqtt5BlockingClient mqtt-client]
   (.connect mqtt-client)
   mqtt-client)
  ([^Mqtt5BlockingClient mqtt-client {:keys [^String username ^String password] :as mqtt-config}]
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
         payload-bytes (when (some? payload) (.getBytes ^String (ensure-string-payload payload) StandardCharsets/UTF_8))]
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
    (-> (.subscribeWith mqtt-client)
        (.addSubscriptions (map #(-> (Mqtt5Subscription/builder)
                                     (.topicFilter ^String %)
                                     (.qos (get kwd->qos qos MqttQos/AT_LEAST_ONCE))
                                     (.build))
                                topics))
        (.send))
    (-> (.toAsync mqtt-client)
        (.publishes MqttGlobalPublishFilter/ALL
                    (fn [^Mqtt5Publish publish]
                      (callback-fn (str (.getTopic publish))
                                   (.getPayloadAsBytes publish)))))
    (catch Throwable t
      (.printStackTrace t)
      (throw t))))

(defn unsubscribe!
  [^Mqtt5BlockingClient mqtt-client ^Mqtt5Unsubscribe subscription]
  (.unsubscribe mqtt-client subscription))

(defn unsubscribe-topic!
  "Unsubscribe from a topic."
  [^Mqtt5BlockingClient mqtt-client ^String topic]
  (-> mqtt-client
      (.unsubscribeWith)
      (.topicFilter topic)
      (.send)))
