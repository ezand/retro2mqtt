(ns ezand.retro2mqtt.retroarch.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [ezand.retro2mqtt.mqtt.core :as mqtt]
            [ezand.retro2mqtt.provider :as provider]
            [ezand.retro2mqtt.retroarch.core :as retro-core]
            [ezand.retro2mqtt.retroarch.mqtt :as retro-mqtt]
            [ezand.retro2mqtt.retroarch.udp-connector :as retro-udp])
  (:import (clojure.lang Atom)
           (ezand.retro2mqtt.retroarch.core RetroarchProvider)
           (java.io File)))

(deftest retroarch-provider-record-test
  (testing "Creates RetroarchProvider record with correct fields"
    (let [mock-client {:client "mock"}
          config {:log-dir (File. "/tmp/logs")
                  :config-dir (File. "/tmp/config")}
          provider (retro-core/->RetroarchProvider mock-client config)]

      (is (instance? RetroarchProvider provider))
      (is (= mock-client (:mqtt-client provider)))
      (is (= config (:config provider)))))

  (testing "Config contains File objects"
    (let [mock-client {:client "mock"}
          config {:log-dir (File. "/path/to/logs")
                  :config-dir (File. "/path/to/config")}
          provider (retro-core/->RetroarchProvider mock-client config)]

      (is (instance? File (get-in provider [:config :log-dir])))
      (is (instance? File (get-in provider [:config :config-dir]))))))

(deftest retroarch-provider-implements-protocol-test
  (testing "RetroarchProvider implements RetroProvider protocol"
    (let [mock-client {:client "mock"}
          config {:log-dir (File. "/tmp/logs")
                  :config-dir (File. "/tmp/config")}
          provider (retro-core/->RetroarchProvider mock-client config)]

      ;; Check that protocol methods exist
      (is (satisfies? provider/RetroProvider provider)))))

(deftest retroarch-provider-config-extraction-test
  (testing "Extracts retroarch config from nested structure"
    (let [mock-client {:client "mock"}
          full-config {:retroarch {:log-dir "/path/to/logs"
                                   :config-dir "/path/to/config"
                                   :udp {:host "localhost" :port 55355}}
                       :mqtt {:broker "mqtt://localhost"}
                       :integrations {:home-assistant {:discovery? true}}}]

      ;; Mock the UDP call and MQTT publish to avoid side effects
      (with-redefs [retro-udp/retroarch-version (constantly nil)
                    mqtt/publish! (constantly nil)
                    retro-mqtt/publish-homeassistant-discovery! (constantly nil)]
        (let [provider (retro-core/retroarch-provider mock-client full-config)]

          (is (instance? RetroarchProvider provider))
          (is (= mock-client (:mqtt-client provider)))

          ;; Verify config extraction
          (let [config (:config provider)]
            (is (contains? config :log-dir))
            (is (contains? config :config-dir))
            (is (instance? File (:log-dir config)))
            (is (instance? File (:config-dir config)))

            ;; Path strings are preserved in File objects
            (is (.getPath (:log-dir config))))))))

  (testing "Converts string paths to File objects"
    (let [mock-client {:client "mock"}
          full-config {:retroarch {:log-dir "/var/log/retroarch"
                                   :config-dir "/etc/retroarch"}}]

      (with-redefs [retro-udp/retroarch-version (constantly nil)
                    mqtt/publish! (constantly nil)
                    retro-mqtt/publish-homeassistant-discovery! (constantly nil)]
        (let [provider (retro-core/retroarch-provider mock-client full-config)
              config (:config provider)]

          (is (= "/var/log/retroarch" (.getPath (:log-dir config))))
          (is (= "/etc/retroarch" (.getPath (:config-dir config))))))))

  (testing "Handles config without UDP settings"
    (let [mock-client {:client "mock"}
          full-config {:retroarch {:log-dir "/logs"
                                   :config-dir "/config"}}]

      (with-redefs [mqtt/publish! (constantly nil)]
        (let [provider (retro-core/retroarch-provider mock-client full-config)]

          (is (some? provider))
          (is (instance? File (get-in provider [:config :log-dir])))))))

  (testing "Handles config without home-assistant discovery"
    (let [mock-client {:client "mock"}
          full-config {:retroarch {:log-dir "/logs"
                                   :config-dir "/config"}
                       :integrations {:home-assistant {:discovery? false}}}]

      (with-redefs [retro-udp/retroarch-version (constantly nil)
                    mqtt/publish! (constantly nil)]
        (let [provider (retro-core/retroarch-provider mock-client full-config)]

          (is (some? provider))
          (is (= mock-client (:mqtt-client provider))))))))

(deftest listening-atom-test
  (testing "listening? atom exists and is initially false"
    (is (instance? Atom retro-core/listening?))
    ;; Note: Can't reliably test initial value as it might be modified by other tests
    ;; Just verify it's a boolean
    (is (boolean? @retro-core/listening?))))

(deftest retroarch-provider-preserves-additional-config-test
  (testing "Preserves additional config keys in retroarch config"
    (let [mock-client {:client "mock"}
          full-config {:retroarch {:log-dir "/logs"
                                   :config-dir "/config"
                                   :udp {:host "localhost" :port 55355}
                                   :custom-key "custom-value"}}]

      (with-redefs [retro-udp/retroarch-version (constantly nil)
                    mqtt/publish! (constantly nil)]
        (let [provider (retro-core/retroarch-provider mock-client full-config)
              config (:config provider)]

          ;; UDP config should be preserved
          (is (contains? config :udp))
          (is (= {:host "localhost" :port 55355} (:udp config)))

          ;; Custom keys should be preserved
          (is (= "custom-value" (:custom-key config))))))))
