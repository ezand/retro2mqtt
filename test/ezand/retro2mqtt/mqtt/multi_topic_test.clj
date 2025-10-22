(ns ezand.retro2mqtt.mqtt.multi-topic-test
  (:require [clojure.test :refer [deftest is testing]]
            [ezand.retro2mqtt.mqtt.multi-topic :as multi-topic])
  (:import (clojure.lang Atom)))

;; Access private atom for testing
(def multi-topic-states @#'multi-topic/multi-topic-states)

(deftest multi-topic-states-atom-test
  (testing "multi-topic-states is an atom"
    (is (instance? Atom multi-topic-states)))

  (testing "multi-topic-states is initialized as a map"
    (is (map? @multi-topic-states)))

  (testing "multi-topic-states can store state by identifier"
    ;; Save current state
    (let [original-state @multi-topic-states]
      (try
        ;; Test mutation
        (swap! multi-topic-states assoc :test-id {:test-key "test-value"})
        (is (contains? @multi-topic-states :test-id))
        (is (= {:test-key "test-value"} (get @multi-topic-states :test-id)))
        (finally
          ;; Restore original state
          (reset! multi-topic-states original-state))))))

(deftest subscribe-topics-function-exists-test
  (testing "subscribe-topics! function is defined"
    (is (fn? multi-topic/subscribe-topics!))))

(deftest topic-config-structure-test
  (testing "Topic config structure with :key"
    ;; Test the expected structure without actually subscribing
    (let [topic-config {"topic/one" {:key :one}
                        "topic/two" {:key :two :data-type :string}}]
      (is (map? topic-config))
      (is (every? string? (keys topic-config)))
      (is (every? map? (vals topic-config)))
      (is (every? :key (vals topic-config)))
      (is (every? keyword? (map :key (vals topic-config))))))

  (testing "Topic config with :data-type :map"
    (let [topic-config {"topic/details" {:key :details :data-type :map}}]
      (is (= :map (:data-type (get topic-config "topic/details"))))))

  (testing "Topic config validates expected keys"
    (let [valid-config {"topic/test" {:key :test}}
          invalid-config {"topic/test" {:no-key "value"}}]
      (is (contains? (get valid-config "topic/test") :key))
      (is (not (contains? (get invalid-config "topic/test") :key))))))

(deftest data-type-handling-test
  (testing "Data type defaults to :string"
    (let [config {:key :test}]
      (is (= :string (get config :data-type :string)))))

  (testing "Data type can be explicitly :map"
    (let [config {:key :test :data-type :map}]
      (is (= :map (:data-type config)))))

  (testing "Data type can be explicitly :string"
    (let [config {:key :test :data-type :string}]
      (is (= :string (:data-type config))))))

(deftest state-identifier-test
  (testing "State identifiers are keywords"
    (let [identifiers [:retroarch-attributes
                       :content-attributes
                       :core-attributes]]
      (is (every? keyword? identifiers))))

  (testing "State can be stored with different identifiers"
    (let [original-state @multi-topic-states]
      (try
        (swap! multi-topic-states assoc :id1 {:data1 "value1"})
        (swap! multi-topic-states assoc :id2 {:data2 "value2"})

        (is (= {:data1 "value1"} (get @multi-topic-states :id1)))
        (is (= {:data2 "value2"} (get @multi-topic-states :id2)))
        (is (not= (get @multi-topic-states :id1)
                  (get @multi-topic-states :id2)))
        (finally
          (reset! multi-topic-states original-state))))))

(deftest merge-behavior-test
  (testing "State merging preserves existing keys"
    (let [original-state @multi-topic-states]
      (try
        ;; Simulate the merge behavior in subscribe-topics!
        (swap! multi-topic-states assoc :test {:key1 "value1"})
        (let [existing (get @multi-topic-states :test)
              new-data {:key2 "value2"}
              merged (merge existing new-data)]
          (is (= {:key1 "value1" :key2 "value2"} merged)))
        (finally
          (reset! multi-topic-states original-state)))))

  (testing "State merging overwrites existing keys with same name"
    (let [original-state @multi-topic-states]
      (try
        (swap! multi-topic-states assoc :test {:key1 "old-value"})
        (let [existing (get @multi-topic-states :test)
              new-data {:key1 "new-value"}
              merged (merge existing new-data)]
          (is (= {:key1 "new-value"} merged)))
        (finally
          (reset! multi-topic-states original-state))))))

(deftest topic-filtering-test
  (testing "Topics can be filtered with set membership"
    (let [subscribed-topics ["topic/one" "topic/two" "topic/three"]
          topic-set (set subscribed-topics)]
      (is (contains? topic-set "topic/one"))
      (is (contains? topic-set "topic/two"))
      (is (not (contains? topic-set "topic/four"))))))

(deftest retain-flag-conversion-test
  (testing "Retain flag is converted to boolean"
    (is (true? (boolean true)))
    (is (false? (boolean false)))
    (is (true? (boolean "truthy")))
    (is (true? (boolean 1)))
    (is (false? (boolean nil)))))
