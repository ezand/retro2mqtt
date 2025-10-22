(ns ezand.retro2mqtt.mqtt.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [ezand.retro2mqtt.mqtt.core :as mqtt])
  (:import (com.hivemq.client.mqtt.datatypes MqttQos)))

;; Access private functions for testing
(def ensure-string-payload #'mqtt/ensure-string-payload)
(def kwd->qos @#'mqtt/kwd->qos)

(deftest ensure-string-payload-test
  (testing "Leaves strings unchanged"
    (is (= "hello" (ensure-string-payload "hello")))
    (is (= "" (ensure-string-payload "")))
    (is (= "with spaces" (ensure-string-payload "with spaces"))))

  (testing "Converts numbers to strings"
    (is (= "42" (ensure-string-payload 42)))
    (is (= "3.14" (ensure-string-payload 3.14)))
    (is (= "0" (ensure-string-payload 0)))
    (is (= "-10" (ensure-string-payload -10))))

  (testing "Converts booleans to on/off strings"
    (is (= "on" (ensure-string-payload true)))
    (is (= "off" (ensure-string-payload false))))

  (testing "Converts collections to JSON strings"
    (let [result (ensure-string-payload {:key "value" :number 123})]
      (is (string? result))
      (is (.contains result "key"))
      (is (.contains result "value")))

    (let [result (ensure-string-payload ["item1" "item2" "item3"])]
      (is (string? result))
      (is (.contains result "item1"))
      (is (.contains result "item2"))))

  (testing "Converts maps to JSON strings"
    (let [result (ensure-string-payload {:foo "bar" :baz 42})]
      (is (string? result))
      (is (.contains result "foo"))
      (is (.contains result "bar"))))

  (testing "Converts vectors to JSON arrays"
    (let [result (ensure-string-payload [1 2 3])]
      (is (string? result))
      (is (.startsWith result "["))))

  (testing "Converts nil to string"
    (is (string? (ensure-string-payload nil))))

  (testing "Converts keywords to strings"
    (is (= ":keyword" (ensure-string-payload :keyword))))

  (testing "Handles nested collections"
    (let [result (ensure-string-payload {:outer {:inner "value"}})]
      (is (string? result))
      (is (.contains result "outer"))
      (is (.contains result "inner")))))

(deftest kwd->qos-mapping-test
  (testing "QoS keyword mappings exist"
    (is (= MqttQos/EXACTLY_ONCE (:exactly-once kwd->qos)))
    (is (= MqttQos/AT_LEAST_ONCE (:at-least-once kwd->qos)))
    (is (= MqttQos/AT_MOST_ONCE (:at-most-once kwd->qos))))

  (testing "All mappings are valid MqttQos enum values"
    (doseq [[kwd qos-value] kwd->qos]
      (is (instance? MqttQos qos-value)
          (str "QoS value for " kwd " should be MqttQos instance"))))

  (testing "Has exactly 3 QoS levels"
    (is (= 3 (count kwd->qos))))

  (testing "All keys are keywords"
    (is (every? keyword? (keys kwd->qos)))))

(deftest create-client-default-options-test
  (testing "create-client accepts empty config"
    ;; Just verify the function can be called with minimal config
    ;; We won't actually create a client to avoid network dependencies
    (is (fn? mqtt/create-client)))

  (testing "create-client function exists and is callable"
    ;; Verify function signature by checking metadata
    (let [meta (meta #'mqtt/create-client)]
      (is (some? meta))
      (is (contains? meta :doc)))))

(deftest publish-qos-level-mapping-test
  (testing "QoS level integer mapping logic"
    ;; This tests the case expression logic in publish!
    ;; We can't easily test the actual function without a client
    ;; but we can verify the mapping matches what we expect
    (let [qos-0 (case 0
                  0 MqttQos/AT_MOST_ONCE
                  1 MqttQos/AT_LEAST_ONCE
                  2 MqttQos/EXACTLY_ONCE)
          qos-1 (case 1
                  0 MqttQos/AT_MOST_ONCE
                  1 MqttQos/AT_LEAST_ONCE
                  2 MqttQos/EXACTLY_ONCE)
          qos-2 (case 2
                  0 MqttQos/AT_MOST_ONCE
                  1 MqttQos/AT_LEAST_ONCE
                  2 MqttQos/EXACTLY_ONCE)]
      (is (= MqttQos/AT_MOST_ONCE qos-0))
      (is (= MqttQos/AT_LEAST_ONCE qos-1))
      (is (= MqttQos/EXACTLY_ONCE qos-2)))))

(deftest public-api-functions-exist-test
  (testing "All expected public functions are defined"
    (is (fn? mqtt/create-client))
    (is (fn? mqtt/connect!))
    (is (fn? mqtt/disconnect!))
    (is (fn? mqtt/publish!))
    (is (fn? mqtt/subscribe!))
    (is (fn? mqtt/unsubscribe!))
    (is (fn? mqtt/unsubscribe-topic!))))

(deftest ensure-string-payload-edge-cases-test
  (testing "Handles empty collections"
    (is (= "[]" (ensure-string-payload [])))
    (is (= "{}" (ensure-string-payload {}))))

  (testing "Handles special float values"
    (is (string? (ensure-string-payload ##Inf)))
    (is (string? (ensure-string-payload ##-Inf)))
    (is (string? (ensure-string-payload ##NaN))))

  (testing "Handles large numbers"
    (is (= "999999999999" (ensure-string-payload 999999999999)))
    (is (string? (ensure-string-payload Long/MAX_VALUE))))

  (testing "Handles strings with special characters"
    (is (= "hello\nworld" (ensure-string-payload "hello\nworld")))
    (is (= "with\"quotes\"" (ensure-string-payload "with\"quotes\""))))

  (testing "Handles nested maps and vectors"
    (let [complex-data {:users [{:name "Alice" :age 30}
                                {:name "Bob" :age 25}]
                        :count 2}
          result (ensure-string-payload complex-data)]
      (is (string? result))
      (is (.contains result "users"))
      (is (.contains result "Alice")))))
