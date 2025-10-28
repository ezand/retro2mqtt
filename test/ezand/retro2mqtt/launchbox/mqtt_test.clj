(ns ezand.retro2mqtt.launchbox.mqtt-test
  (:require [clojure.test :refer [deftest is testing]]
            [ezand.retro2mqtt.launchbox.mqtt :as launchbox-mqtt]))

(def test-topics (launchbox-mqtt/launchbox-topics "launchbox"))

(deftest launchbox-topics-function-test
  (testing "launchbox-topics function returns map with all keys"
    (let [topics (launchbox-mqtt/launchbox-topics "launchbox")]
      (is (map? topics))
      (is (contains? topics :content-loaded))
      (is (contains? topics :content-running))
      (is (contains? topics :content))
      (is (contains? topics :content-last-played))
      (is (contains? topics :content-details))
      (is (contains? topics :emulator-loaded))
      (is (contains? topics :emulator-running))
      (is (contains? topics :emulator))
      (is (contains? topics :emulator-last-loaded))
      (is (contains? topics :emulator-details))
      (is (contains? topics :running))
      (is (contains? topics :details))
      (is (contains? topics :bigbox-running))
      (is (contains? topics :bigbox-locked))
      (is (contains? topics :system-event))))

  (testing "All topic values are strings"
    (let [topics (launchbox-mqtt/launchbox-topics "launchbox")]
      (doseq [[k v] topics]
        (is (string? v)))))

  (testing "Topics follow expected format with default prefix"
    (let [topics (launchbox-mqtt/launchbox-topics "launchbox")]
      (is (.startsWith (:content-loaded topics) "launchbox/"))
      (is (.startsWith (:running topics) "launchbox/"))
      (is (.startsWith (:emulator topics) "launchbox/"))))

  (testing "Topics use custom prefix"
    (let [topics (launchbox-mqtt/launchbox-topics "custom")]
      (is (.startsWith (:content-loaded topics) "custom/"))
      (is (.startsWith (:running topics) "custom/"))
      (is (.startsWith (:emulator topics) "custom/")))))

(deftest state-topic-uniqueness-test
  (testing "All state topics are unique"
    (let [topic-values (vals test-topics)]
      (is (= (count topic-values) (count (set topic-values)))))))

(deftest specific-topic-values-test
  (testing "Content topics"
    (is (= "launchbox/content/loaded" (:content-loaded test-topics)))
    (is (= "launchbox/content/running" (:content-running test-topics)))
    (is (= "launchbox/content" (:content test-topics)))
    (is (= "launchbox/content/last_played" (:content-last-played test-topics)))
    (is (= "launchbox/content/details" (:content-details test-topics))))

  (testing "Emulator topics"
    (is (= "launchbox/emulator/loaded" (:emulator-loaded test-topics)))
    (is (= "launchbox/emulator/running" (:emulator-running test-topics)))
    (is (= "launchbox/emulator" (:emulator test-topics)))
    (is (= "launchbox/emulator/last_loaded" (:emulator-last-loaded test-topics)))
    (is (= "launchbox/emulator/details" (:emulator-details test-topics))))

  (testing "LaunchBox main topics"
    (is (= "launchbox/running" (:running test-topics)))
    (is (= "launchbox/details" (:details test-topics))))

  (testing "BigBox topics"
    (is (= "launchbox/bigbox/running" (:bigbox-running test-topics)))
    (is (= "launchbox/bigbox/locked" (:bigbox-locked test-topics))))

  (testing "System topics"
    (is (= "launchbox/system/event" (:system-event test-topics)))))

(deftest launchbox-device-config-test
  (testing "Device config structure with version"
    (let [device (#'launchbox-mqtt/launchbox-device-config "1.2.3")]
      (is (map? device))
      (is (contains? device :identifiers))
      (is (contains? device :name))
      (is (contains? device :manufacturer))
      (is (contains? device :model))
      (is (contains? device :sw_version))
      (is (= ["launchbox"] (:identifiers device)))
      (is (= "LaunchBox" (:name device)))
      (is (= "Unbroken Software, LLC" (:manufacturer device)))
      (is (= "LaunchBox" (:model device)))
      (is (= "1.2.3" (:sw_version device)))))

  (testing "Device config with different versions"
    (let [v1 (#'launchbox-mqtt/launchbox-device-config "13.5")
          v2 (#'launchbox-mqtt/launchbox-device-config "Unknown")]
      (is (= "13.5" (:sw_version v1)))
      (is (= "Unknown" (:sw_version v2)))))

  (testing "Device config has correct manufacturer"
    (let [device (#'launchbox-mqtt/launchbox-device-config "1.0")]
      (is (= "Unbroken Software, LLC" (:manufacturer device))))))

(deftest entity-configurations-structure-test
  (testing "entity-configurations returns a vector"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)]
      (is (vector? configs))
      (is (pos? (count configs)))))

  (testing "All entities have required keys"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)]
      (doseq [config configs]
        (is (contains? config :unique_id))
        (is (contains? config :name))
        (is (contains? config :state_topic))
        (is (contains? config :icon)))))

  (testing "Entity unique IDs are strings"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)]
      (doseq [config configs]
        (is (string? (:unique_id config))))))

  (testing "Entity names are strings"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)]
      (doseq [config configs]
        (is (string? (:name config))))))

  (testing "State topics are valid strings"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)]
      (doseq [config configs]
        (is (string? (:state_topic config)))
        (is (.startsWith (:state_topic config) "launchbox/")))))

  (testing "Icons follow MDI format"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)]
      (doseq [config configs]
        (is (.startsWith (:icon config) "mdi:"))))))

(deftest entity-configurations-count-test
  (testing "Has expected number of entities"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)]
      (is (= 12 (count configs))))))

(deftest entity-configurations-unique-ids-test
  (testing "All entity unique IDs are unique"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)
          unique-ids (map :unique_id configs)]
      (is (= (count unique-ids) (count (set unique-ids))))))

  (testing "Expected unique IDs exist"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)
          unique-ids (set (map :unique_id configs))]
      (is (contains? unique-ids "launchbox"))
      (is (contains? unique-ids "launchbox_bigbox_running"))
      (is (contains? unique-ids "launchbox_bigbox_locked"))
      (is (contains? unique-ids "launchbox_emulator"))
      (is (contains? unique-ids "launchbox_emulator_last_loaded"))
      (is (contains? unique-ids "launchbox_emulator_loaded"))
      (is (contains? unique-ids "launchbox_emulator_running"))
      (is (contains? unique-ids "launchbox_content"))
      (is (contains? unique-ids "launchbox_content_last_played"))
      (is (contains? unique-ids "launchbox_content_loaded"))
      (is (contains? unique-ids "launchbox_content_running"))
      (is (contains? unique-ids "launchbox_last_system_event")))))

(deftest entity-configurations-with-attributes-test
  (testing "Entities with JSON attributes have correct structure"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)
          configs-with-attrs (filter :json_attributes_topic configs)]
      (is (= 5 (count configs-with-attrs)))
      (doseq [config configs-with-attrs]
        (is (string? (:json_attributes_topic config)))
        (is (.startsWith (:json_attributes_topic config) "homeassistant/sensor/"))
        (is (map? (:attribute-state-topics config))))))

  (testing "Attribute state topics have correct data types"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)
          configs-with-attrs (filter :attribute-state-topics configs)]
      (doseq [config configs-with-attrs]
        (let [attr-topics (:attribute-state-topics config)]
          (doseq [[topic-key topic-config] attr-topics]
            (is (string? topic-key))
            (is (map? topic-config))
            (is (= :map (:data-type topic-config)))))))))

(deftest entity-configurations-specific-entities-test
  (testing "LaunchBox main entity configuration"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)
          launchbox-entity (first (filter #(= "launchbox" (:unique_id %)) configs))]
      (is (some? launchbox-entity))
      (is (= "LaunchBox" (:name launchbox-entity)))
      (is (= (:running test-topics) (:state_topic launchbox-entity)))
      (is (true? (:retain-attributes? launchbox-entity)))
      (is (= "mdi:monitor-star" (:icon launchbox-entity)))))

  (testing "Content entity configuration"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)
          content-entity (first (filter #(= "launchbox_content" (:unique_id %)) configs))]
      (is (some? content-entity))
      (is (= "Current Game" (:name content-entity)))
      (is (= (:content test-topics) (:state_topic content-entity)))
      (is (= "mdi:gamepad-variant" (:icon content-entity)))))

  (testing "Emulator entity configuration"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)
          emulator-entity (first (filter #(= "launchbox_emulator" (:unique_id %)) configs))]
      (is (some? emulator-entity))
      (is (= "Loaded Emulator" (:name emulator-entity)))
      (is (= (:emulator test-topics) (:state_topic emulator-entity)))
      (is (= "mdi:monitor-star" (:icon emulator-entity))))))

(deftest publish-homeassistant-discovery-function-test
  (testing "publish-homeassistant-discovery! function exists"
    (is (var? #'launchbox-mqtt/publish-homeassistant-discovery!)))

  (testing "Function has correct arities"
    (let [func launchbox-mqtt/publish-homeassistant-discovery!]
      (is (fn? func)))))

(deftest homeassistant-topics-test
  (testing "Home Assistant attribute topics are defined"
    (let [launchbox-attr-topic @#'launchbox-mqtt/topic-homeassistant-launchbox-attributes
          emulator-attr-topic @#'launchbox-mqtt/topic-homeassistant-emulator-attributes
          content-attr-topic @#'launchbox-mqtt/topic-homeassistant-content-attributes]
      (is (string? launchbox-attr-topic))
      (is (string? emulator-attr-topic))
      (is (string? content-attr-topic))
      (is (.startsWith launchbox-attr-topic "homeassistant/sensor/"))
      (is (.startsWith emulator-attr-topic "homeassistant/sensor/"))
      (is (.startsWith content-attr-topic "homeassistant/sensor/"))))

  (testing "Home Assistant topics are unique"
    (let [topics [@#'launchbox-mqtt/topic-homeassistant-launchbox-attributes
                  @#'launchbox-mqtt/topic-homeassistant-emulator-attributes
                  @#'launchbox-mqtt/topic-homeassistant-content-attributes]]
      (is (= 3 (count (set topics)))))))

(deftest entity-icon-consistency-test
  (testing "Content-related entities use gamepad icon"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)
          content-entities (filter #(.contains (:unique_id %) "content") configs)]
      (doseq [entity content-entities]
        (is (= "mdi:gamepad-variant" (:icon entity))))))

  (testing "Main entities use monitor-star icon"
    (let [configs (#'launchbox-mqtt/entity-configurations test-topics)
          monitor-entities (filter #(contains? #{"launchbox" "launchbox_emulator" "launchbox_emulator_last_loaded"}
                                               (:unique_id %))
                                   configs)]
      (doseq [entity monitor-entities]
        (is (= "mdi:monitor-star" (:icon entity)))))))

(deftest topic-naming-convention-test
  (testing "State topics use underscores for multi-word properties"
    (is (.contains (:content-last-played test-topics) "last_played"))
    (is (.contains (:emulator-last-loaded test-topics) "last_loaded")))

  (testing "State topics end with expected suffixes"
    (is (.endsWith (:content-loaded test-topics) "loaded"))
    (is (.endsWith (:content-running test-topics) "running"))
    (is (.endsWith (:running test-topics) "running"))
    (is (.endsWith (:bigbox-running test-topics) "running"))
    (is (.endsWith (:bigbox-locked test-topics) "locked"))
    (is (.endsWith (:system-event test-topics) "event"))))
