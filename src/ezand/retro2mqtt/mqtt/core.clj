(ns ezand.retro2mqtt.mqtt.core
  (:import (com.hivemq.client.mqtt MqttClient)
           (com.hivemq.client.mqtt.datatypes MqttQos)
           (com.hivemq.client.mqtt.mqtt3 Mqtt3BlockingClient)
           (java.nio.charset StandardCharsets)
           (java.util UUID)))

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
      (.useMqttVersion3)
      (.serverHost ^String host)
      (.serverPort port)
      (.identifier ^String client-id)
      (.buildBlocking)))

(defn connect!
  ([^Mqtt3BlockingClient mqtt-client]
   (.connect mqtt-client)
   mqtt-client)
  ([^Mqtt3BlockingClient mqtt-client ^String username ^String password]
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
  [^Mqtt3BlockingClient mqtt-client]
  (.disconnect mqtt-client))

(defn publish!
  "Publish a message to a topic.

  Options:
  - :qos - Quality of Service level (0, 1, or 2, default: 0)
  - :retain - Retain message flag (default: false)"
  ([mqtt-client topic payload]
   (publish! mqtt-client topic payload {}))
  ([^Mqtt3BlockingClient mqtt-client ^String topic ^String payload
    {:keys [qos retain?]
     :or {qos 0
          retain? false}}]
   (let [qos-level (case qos
                     0 MqttQos/AT_MOST_ONCE
                     1 MqttQos/AT_LEAST_ONCE
                     2 MqttQos/EXACTLY_ONCE)
         payload-bytes (when payload (.getBytes ^String payload StandardCharsets/UTF_8))]
     (-> mqtt-client
         (.publishWith)
         (.topic topic)
         (.payload payload-bytes)
         (.qos qos-level)
         (.retain retain?)
         (.send)))))
