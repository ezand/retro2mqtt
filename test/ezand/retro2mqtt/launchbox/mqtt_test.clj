(ns ezand.retro2mqtt.launchbox.mqtt-test
  (:require [clojure.test :refer [deftest is testing]]
            [ezand.retro2mqtt.launchbox.mqtt :as launchbox-mqtt]))

(deftest state-topic-constants-test
  (testing "All state topic constants are defined"
    (is (string? launchbox-mqtt/topic-launchbox-content-loaded?))
    (is (string? launchbox-mqtt/topic-launchbox-content-running?))
    (is (string? launchbox-mqtt/topic-launchbox-content))
    (is (string? launchbox-mqtt/topic-launchbox-content-last-played))
    (is (string? launchbox-mqtt/topic-launchbox-content-details))
    (is (string? launchbox-mqtt/topic-launchbox-emulator-loaded?))
    (is (string? launchbox-mqtt/topic-launchbox-emulator-running?))
    (is (string? launchbox-mqtt/topic-launchbox-emulator))
    (is (string? launchbox-mqtt/topic-launchbox-emulator-last-loaded))
    (is (string? launchbox-mqtt/topic-launchbox-emulator-details))
    (is (string? launchbox-mqtt/topic-launchbox-running?))
    (is (string? launchbox-mqtt/topic-launchbox-details))
    (is (string? launchbox-mqtt/topic-launchbox-bigbox-running?))
    (is (string? launchbox-mqtt/topic-launchbox-bigbox-locked?))
    (is (string? launchbox-mqtt/topic-launchbox-system-event?)))

  (testing "Topic strings follow expected format"
    (is (.startsWith launchbox-mqtt/topic-launchbox-content-loaded? "launchbox/"))
    (is (.startsWith launchbox-mqtt/topic-launchbox-running? "launchbox/"))
    (is (.startsWith launchbox-mqtt/topic-launchbox-emulator "launchbox/"))))

(deftest state-topic-uniqueness-test
  (testing "All state topics are unique"
    (let [topics [launchbox-mqtt/topic-launchbox-content-loaded?
                  launchbox-mqtt/topic-launchbox-content-running?
                  launchbox-mqtt/topic-launchbox-content
                  launchbox-mqtt/topic-launchbox-content-last-played
                  launchbox-mqtt/topic-launchbox-content-details
                  launchbox-mqtt/topic-launchbox-emulator-loaded?
                  launchbox-mqtt/topic-launchbox-emulator-running?
                  launchbox-mqtt/topic-launchbox-emulator
                  launchbox-mqtt/topic-launchbox-emulator-last-loaded
                  launchbox-mqtt/topic-launchbox-emulator-details
                  launchbox-mqtt/topic-launchbox-running?
                  launchbox-mqtt/topic-launchbox-details
                  launchbox-mqtt/topic-launchbox-bigbox-running?
                  launchbox-mqtt/topic-launchbox-bigbox-locked?
                  launchbox-mqtt/topic-launchbox-system-event?]]
      (is (= (count topics) (count (set topics)))))))

(deftest specific-topic-values-test
  (testing "Content topics"
    (is (= "launchbox/content/loaded" launchbox-mqtt/topic-launchbox-content-loaded?))
    (is (= "launchbox/content/running" launchbox-mqtt/topic-launchbox-content-running?))
    (is (= "launchbox/content" launchbox-mqtt/topic-launchbox-content))
    (is (= "launchbox/content/last_played" launchbox-mqtt/topic-launchbox-content-last-played))
    (is (= "launchbox/content/details" launchbox-mqtt/topic-launchbox-content-details)))

  (testing "Emulator topics"
    (is (= "launchbox/emulator/loaded" launchbox-mqtt/topic-launchbox-emulator-loaded?))
    (is (= "launchbox/emulator/running" launchbox-mqtt/topic-launchbox-emulator-running?))
    (is (= "launchbox/emulator" launchbox-mqtt/topic-launchbox-emulator))
    (is (= "launchbox/emulator/last_loaded" launchbox-mqtt/topic-launchbox-emulator-last-loaded))
    (is (= "launchbox/emulator/details" launchbox-mqtt/topic-launchbox-emulator-details)))

  (testing "LaunchBox main topics"
    (is (= "launchbox/running" launchbox-mqtt/topic-launchbox-running?))
    (is (= "launchbox/details" launchbox-mqtt/topic-launchbox-details)))

  (testing "BigBox topics"
    (is (= "launchbox/bigbox/running" launchbox-mqtt/topic-launchbox-bigbox-running?))
    (is (= "launchbox/bigbox/locked" launchbox-mqtt/topic-launchbox-bigbox-locked?)))

  (testing "System topics"
    (is (= "launchbox/system/event" launchbox-mqtt/topic-launchbox-system-event?))))

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
  (testing "entity-configurations is a vector"
    (let [configs @#'launchbox-mqtt/entity-configurations]
      (is (vector? configs))
      (is (pos? (count configs)))))

  (testing "All entities have required keys"
    (let [configs @#'launchbox-mqtt/entity-configurations]
      (doseq [config configs]
        (is (contains? config :unique_id))
        (is (contains? config :name))
        (is (contains? config :state_topic))
        (is (contains? config :icon)))))

  (testing "Entity unique IDs are strings"
    (let [configs @#'launchbox-mqtt/entity-configurations]
      (doseq [config configs]
        (is (string? (:unique_id config))))))

  (testing "Entity names are strings"
    (let [configs @#'launchbox-mqtt/entity-configurations]
      (doseq [config configs]
        (is (string? (:name config))))))

  (testing "State topics are valid strings"
    (let [configs @#'launchbox-mqtt/entity-configurations]
      (doseq [config configs]
        (is (string? (:state_topic config)))
        (is (.startsWith (:state_topic config) "launchbox/")))))

  (testing "Icons follow MDI format"
    (let [configs @#'launchbox-mqtt/entity-configurations]
      (doseq [config configs]
        (is (.startsWith (:icon config) "mdi:"))))))

(deftest entity-configurations-count-test
  (testing "Has expected number of entities"
    (let [configs @#'launchbox-mqtt/entity-configurations]
      (is (= 12 (count configs))))))

(deftest entity-configurations-unique-ids-test
  (testing "All entity unique IDs are unique"
    (let [configs @#'launchbox-mqtt/entity-configurations
          unique-ids (map :unique_id configs)]
      (is (= (count unique-ids) (count (set unique-ids))))))

  (testing "Expected unique IDs exist"
    (let [configs @#'launchbox-mqtt/entity-configurations
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
    (let [configs @#'launchbox-mqtt/entity-configurations
          configs-with-attrs (filter :json_attributes_topic configs)]
      (is (= 5 (count configs-with-attrs)))
      (doseq [config configs-with-attrs]
        (is (string? (:json_attributes_topic config)))
        (is (.startsWith (:json_attributes_topic config) "homeassistant/sensor/"))
        (is (map? (:attribute-state-topics config))))))

  (testing "Attribute state topics have correct data types"
    (let [configs @#'launchbox-mqtt/entity-configurations
          configs-with-attrs (filter :attribute-state-topics configs)]
      (doseq [config configs-with-attrs]
        (let [attr-topics (:attribute-state-topics config)]
          (doseq [[topic-key topic-config] attr-topics]
            (is (string? topic-key))
            (is (map? topic-config))
            (is (= :map (:data-type topic-config)))))))))

(deftest entity-configurations-specific-entities-test
  (testing "LaunchBox main entity configuration"
    (let [configs @#'launchbox-mqtt/entity-configurations
          launchbox-entity (first (filter #(= "launchbox" (:unique_id %)) configs))]
      (is (some? launchbox-entity))
      (is (= "LaunchBox" (:name launchbox-entity)))
      (is (= launchbox-mqtt/topic-launchbox-running? (:state_topic launchbox-entity)))
      (is (true? (:retain-attributes? launchbox-entity)))
      (is (= "mdi:monitor-star" (:icon launchbox-entity)))))

  (testing "Content entity configuration"
    (let [configs @#'launchbox-mqtt/entity-configurations
          content-entity (first (filter #(= "launchbox_content" (:unique_id %)) configs))]
      (is (some? content-entity))
      (is (= "Current Game" (:name content-entity)))
      (is (= launchbox-mqtt/topic-launchbox-content (:state_topic content-entity)))
      (is (= "mdi:gamepad-variant" (:icon content-entity)))))

  (testing "Emulator entity configuration"
    (let [configs @#'launchbox-mqtt/entity-configurations
          emulator-entity (first (filter #(= "launchbox_emulator" (:unique_id %)) configs))]
      (is (some? emulator-entity))
      (is (= "Loaded Emulator" (:name emulator-entity)))
      (is (= launchbox-mqtt/topic-launchbox-emulator (:state_topic emulator-entity)))
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
    (let [configs @#'launchbox-mqtt/entity-configurations
          content-entities (filter #(.contains (:unique_id %) "content") configs)]
      (doseq [entity content-entities]
        (is (= "mdi:gamepad-variant" (:icon entity))))))

  (testing "Main entities use monitor-star icon"
    (let [configs @#'launchbox-mqtt/entity-configurations
          monitor-entities (filter #(contains? #{"launchbox" "launchbox_emulator" "launchbox_emulator_last_loaded"}
                                               (:unique_id %))
                                   configs)]
      (doseq [entity monitor-entities]
        (is (= "mdi:monitor-star" (:icon entity)))))))

(deftest topic-naming-convention-test
  (testing "State topics use underscores for multi-word properties"
    (is (.contains launchbox-mqtt/topic-launchbox-content-last-played "last_played"))
    (is (.contains launchbox-mqtt/topic-launchbox-emulator-last-loaded "last_loaded")))

  (testing "Boolean state topics end with ?"
    (is (.endsWith launchbox-mqtt/topic-launchbox-content-loaded? "loaded"))
    (is (.endsWith launchbox-mqtt/topic-launchbox-content-running? "running"))
    (is (.endsWith launchbox-mqtt/topic-launchbox-running? "running"))
    (is (.endsWith launchbox-mqtt/topic-launchbox-bigbox-running? "running"))
    (is (.endsWith launchbox-mqtt/topic-launchbox-bigbox-locked? "locked"))
    (is (.endsWith launchbox-mqtt/topic-launchbox-system-event? "event"))))
