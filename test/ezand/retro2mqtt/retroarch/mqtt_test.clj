(ns ezand.retro2mqtt.retroarch.mqtt-test
  (:require [clojure.test :refer [deftest is testing]]
            [ezand.retro2mqtt.retroarch.mqtt :as mqtt]))

;; Access private function for testing
(def retroarch-device-config #'mqtt/retroarch-device-config)
(def entity-configurations #'mqtt/entity-configurations)

(deftest retroarch-device-config-test
  (testing "Creates device config with version"
    (let [config (retroarch-device-config "1.21.0")]
      (is (map? config))
      (is (= ["retroarch"] (:identifiers config)))
      (is (= "RetroArch" (:name config)))
      (is (= "Libretro" (:manufacturer config)))
      (is (= "RetroArch" (:model config)))
      (is (= "1.21.0" (:sw_version config)))))

  (testing "Handles different version formats"
    (let [config (retroarch-device-config "v2.0.0-beta")]
      (is (= "v2.0.0-beta" (:sw_version config))))))

(deftest topic-constants-test
  (testing "State topic constants exist and are strings"
    (is (= "retroarch/details" mqtt/topic-retroarch-details))
    (is (= "retroarch/status" mqtt/topic-retroarch-status))
    (is (= "retroarch/core" mqtt/topic-retroarch-core))
    (is (= "retroarch/core/details" mqtt/topic-retroarch-core-details))
    (is (= "retroarch/core/last_loaded" mqtt/topic-retroarch-core-last-loaded))
    (is (= "retroarch/content" mqtt/topic-retroarch-content))
    (is (= "retroarch/content/last_played" mqtt/topic-retroarch-content-last-played))
    (is (= "retroarch/content/loaded" mqtt/topic-retroarch-content-loaded?))
    (is (= "retroarch/content/running" mqtt/topic-retroarch-content-running?))
    (is (= "retroarch/content/crc32" mqtt/topic-retroarch-content-crc32))
    (is (= "retroarch/content/video_size" mqtt/topic-retroarch-content-video-size))
    (is (= "retroarch/system/cpu" mqtt/topic-retroarch-system-cpu))
    (is (= "retroarch/system/capabilities" mqtt/topic-retroarch-system-capabilities))
    (is (= "retroarch/system/display/driver" mqtt/topic-retroarch-system-display-driver))
    (is (= "retroarch/system/joypad/driver" mqtt/topic-retroarch-system-joypad-driver))
    (is (= "retroarch/system/audio/input_rate" mqtt/topic-retroarch-system-audio-input-rate))
    (is (= "retroarch/system/pixel_format" mqtt/topic-retroarch-system-pixel-format))
    (is (= "retroarch/libretro/version" mqtt/topic-retroarch-libretro-api-version))
    (is (= "retroarch/libretro/core_file" mqtt/topic-retroarch-libretro-core-file))
    (is (= "retroarch/version" mqtt/topic-retroarch-version))
    (is (= "retroarch/version/build_date" mqtt/topic-retroarch-version-build-date))
    (is (= "retroarch/version/git_hash" mqtt/topic-retroarch-version-git-hash))
    (is (= "retroarch/cmd_interface_port" mqtt/topic-retroarch-cmd-interface-port)))

  (testing "All topics are non-empty strings"
    (is (every? #(and (string? %) (seq %))
                [mqtt/topic-retroarch-details
                 mqtt/topic-retroarch-status
                 mqtt/topic-retroarch-core
                 mqtt/topic-retroarch-content]))))

(deftest entity-configurations-structure-test
  (testing "Entity configurations is a collection"
    (is (coll? @entity-configurations))
    (is (seq @entity-configurations)))

  (testing "Has expected number of entities"
    (is (= 7 (count @entity-configurations))))

  (testing "All entities have required keys"
    (doseq [entity @entity-configurations]
      (is (contains? entity :unique_id) (str "Missing :unique_id in " entity))
      (is (contains? entity :name) (str "Missing :name in " entity))
      (is (contains? entity :state_topic) (str "Missing :state_topic in " entity))
      (is (contains? entity :icon) (str "Missing :icon in " entity))))

  (testing "All unique_id values are non-empty strings"
    (doseq [entity @entity-configurations]
      (is (string? (:unique_id entity)))
      (is (seq (:unique_id entity)))))

  (testing "All name values are non-empty strings"
    (doseq [entity @entity-configurations]
      (is (string? (:name entity)))
      (is (seq (:name entity)))))

  (testing "All state_topic values are non-empty strings"
    (doseq [entity @entity-configurations]
      (is (string? (:state_topic entity)))
      (is (seq (:state_topic entity)))))

  (testing "All icons use MDI format"
    (doseq [entity @entity-configurations]
      (is (string? (:icon entity)))
      (is (re-matches #"mdi:.+" (:icon entity))
          (str "Icon doesn't match MDI format: " (:icon entity)))))

  (testing "unique_id values are unique"
    (let [ids (map :unique_id @entity-configurations)]
      (is (= (count ids) (count (distinct ids)))
          "Duplicate unique_id found"))))

(deftest entity-specific-configurations-test
  (testing "retroarch entity configuration"
    (let [entity (first (filter #(= "retroarch" (:unique_id %)) @entity-configurations))]
      (is (some? entity))
      (is (= "Retroarch" (:name entity)))
      (is (= mqtt/topic-retroarch-status (:state_topic entity)))
      (is (contains? entity :json_attributes_topic))
      (is (contains? entity :attribute-state-topics))
      (is (map? (:attribute-state-topics entity)))
      (is (true? (:retain-attributes? entity)))
      (is (= "mdi:monitor-star" (:icon entity)))))

  (testing "retroarch_core entity configuration"
    (let [entity (first (filter #(= "retroarch_core" (:unique_id %)) @entity-configurations))]
      (is (some? entity))
      (is (= "Loaded Core" (:name entity)))
      (is (= mqtt/topic-retroarch-core (:state_topic entity)))))

  (testing "retroarch_content entity configuration"
    (let [entity (first (filter #(= "retroarch_content" (:unique_id %)) @entity-configurations))]
      (is (some? entity))
      (is (= "Current Game" (:name entity)))
      (is (= mqtt/topic-retroarch-content (:state_topic entity)))
      (is (= "mdi:gamepad-variant" (:icon entity)))))

  (testing "retroarch_content_loaded entity configuration"
    (let [entity (first (filter #(= "retroarch_content_loaded" (:unique_id %)) @entity-configurations))]
      (is (some? entity))
      (is (= "Is Game Loaded" (:name entity)))
      (is (= mqtt/topic-retroarch-content-loaded? (:state_topic entity)))))

  (testing "retroarch_content_running entity configuration"
    (let [entity (first (filter #(= "retroarch_content_running" (:unique_id %)) @entity-configurations))]
      (is (some? entity))
      (is (= "Is Game Running" (:name entity)))
      (is (= mqtt/topic-retroarch-content-running? (:state_topic entity))))))

(deftest attribute-state-topics-structure-test
  (testing "Entities with attribute-state-topics have valid structure"
    (doseq [entity (filter :attribute-state-topics @entity-configurations)]
      (let [attr-topics (:attribute-state-topics entity)]
        (testing (str "Entity " (:unique_id entity))
          (is (map? attr-topics) "attribute-state-topics should be a map")

          (doseq [[topic config] attr-topics]
            (testing (str "Topic " topic)
              (is (string? topic) "Topic key should be a string")
              (is (map? config) "Topic config should be a map")
              (is (contains? config :key) "Config should have :key")
              (is (keyword? (:key config)) "Config :key should be a keyword")

              (when (contains? config :data-type)
                (is (keyword? (:data-type config))
                    "Config :data-type should be a keyword when present")))))))))

(deftest json-attributes-topic-consistency-test
  (testing "Entities with attribute-state-topics also have json_attributes_topic"
    (doseq [entity @entity-configurations]
      (when (:attribute-state-topics entity)
        (is (contains? entity :json_attributes_topic)
            (str "Entity " (:unique_id entity) " has attribute-state-topics but no json_attributes_topic")))))

  (testing "Entities with json_attributes_topic also have attribute-state-topics"
    (doseq [entity @entity-configurations]
      (when (:json_attributes_topic entity)
        (is (contains? entity :attribute-state-topics)
            (str "Entity " (:unique_id entity) " has json_attributes_topic but no attribute-state-topics"))))))

(deftest retroarch-main-entity-attributes-test
  (testing "Main retroarch entity has comprehensive attribute mappings"
    (let [entity (first (filter #(= "retroarch" (:unique_id %)) @entity-configurations))
          attr-topics (:attribute-state-topics entity)]
      (is (contains? attr-topics mqtt/topic-retroarch-system-cpu))
      (is (contains? attr-topics mqtt/topic-retroarch-system-capabilities))
      (is (contains? attr-topics mqtt/topic-retroarch-system-display-driver))
      (is (contains? attr-topics mqtt/topic-retroarch-system-joypad-driver))
      (is (contains? attr-topics mqtt/topic-retroarch-system-audio-input-rate))
      (is (contains? attr-topics mqtt/topic-retroarch-system-pixel-format))
      (is (contains? attr-topics mqtt/topic-retroarch-libretro-api-version))
      (is (contains? attr-topics mqtt/topic-retroarch-version))
      (is (contains? attr-topics mqtt/topic-retroarch-version-build-date))
      (is (contains? attr-topics mqtt/topic-retroarch-version-git-hash))
      (is (contains? attr-topics mqtt/topic-retroarch-cmd-interface-port))
      (is (contains? attr-topics mqtt/topic-retroarch-details))

      (testing "Details attribute has data-type :map"
        (is (= :map (:data-type (get attr-topics mqtt/topic-retroarch-details))))))))

(deftest core-entity-attributes-test
  (testing "Core entities have libretro-core-file and details attributes"
    (doseq [unique-id ["retroarch_core" "retroarch_core_last_loaded"]]
      (let [entity (first (filter #(= unique-id (:unique_id %)) @entity-configurations))
            attr-topics (:attribute-state-topics entity)]
        (testing (str "Entity " unique-id)
          (is (contains? attr-topics mqtt/topic-retroarch-libretro-core-file))
          (is (contains? attr-topics mqtt/topic-retroarch-core-details))
          (is (= :map (:data-type (get attr-topics mqtt/topic-retroarch-core-details)))))))))

(deftest content-entity-attributes-test
  (testing "Content entities have crc32 and video-size attributes"
    (doseq [unique-id ["retroarch_content" "retroarch_content_last_played"]]
      (let [entity (first (filter #(= unique-id (:unique_id %)) @entity-configurations))
            attr-topics (:attribute-state-topics entity)]
        (testing (str "Entity " unique-id)
          (is (contains? attr-topics mqtt/topic-retroarch-content-crc32))
          (is (contains? attr-topics mqtt/topic-retroarch-content-video-size))
          (is (= :crc32 (:key (get attr-topics mqtt/topic-retroarch-content-crc32))))
          (is (= :video-size (:key (get attr-topics mqtt/topic-retroarch-content-video-size)))))))))

(deftest state-topic-references-test
  (testing "All state_topic values reference defined topic constants"
    (let [defined-topics #{mqtt/topic-retroarch-status
                           mqtt/topic-retroarch-core
                           mqtt/topic-retroarch-core-last-loaded
                           mqtt/topic-retroarch-content
                           mqtt/topic-retroarch-content-last-played
                           mqtt/topic-retroarch-content-loaded?
                           mqtt/topic-retroarch-content-running?}
          used-topics (set (map :state_topic @entity-configurations))]
      (is (every? defined-topics used-topics)
          "All state_topic values should reference defined constants"))))

(deftest entity-naming-conventions-test
  (testing "Entity names follow conventions"
    (doseq [entity @entity-configurations]
      (let [name (:name entity)]
        (is (re-matches #"[A-Z].*" name)
            (str "Entity name should start with capital letter: " name)))))

  (testing "unique_id values use snake_case"
    (doseq [entity @entity-configurations]
      (let [id (:unique_id entity)]
        (is (re-matches #"[a-z_]+" id)
            (str "unique_id should be lowercase with underscores: " id))))))

(deftest retain-attributes-flag-test
  (testing "Only main retroarch entity has retain-attributes? true"
    (let [entities-with-retain (filter :retain-attributes? @entity-configurations)]
      (is (= 1 (count entities-with-retain)))
      (is (= "retroarch" (:unique_id (first entities-with-retain)))))))
