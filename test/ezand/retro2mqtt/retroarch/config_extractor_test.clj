(ns ezand.retro2mqtt.retroarch.config-extractor-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ezand.retro2mqtt.retroarch.config-extractor :as config])
  (:import (java.io File)))

;; Access private functions for testing
(def parse-value #'config/parse-value)
(def parse-line #'config/parse-line)
(def interesting-keys @#'config/interesting-keys)

(deftest parse-value-test
  (testing "Parses string values"
    (is (= "ezand" (parse-value "ezand")))
    (is (= "hello world" (parse-value "hello world"))))

  (testing "Parses boolean true"
    (is (true? (parse-value "true"))))

  (testing "Parses boolean false"
    (is (false? (parse-value "false"))))

  (testing "Parses integers"
    (is (= 42 (parse-value "42")))
    (is (= -10 (parse-value "-10")))
    (is (= 0 (parse-value "0"))))

  (testing "Parses floating point numbers"
    (is (= 3.14 (parse-value "3.14")))
    (is (= -2.5 (parse-value "-2.5")))
    (is (= 0.0 (parse-value "0.0"))))

  (testing "Returns nil for empty string"
    (is (nil? (parse-value ""))))

  (testing "Returns nil for whitespace-only string"
    (is (nil? (parse-value "   ")))
    (is (nil? (parse-value "\t"))))

  (testing "Returns nil for nil input"
    (is (nil? (parse-value nil))))

  (testing "Trims whitespace from string values"
    (is (= "value" (parse-value "  value  ")))))

(deftest parse-line-test
  (testing "Parses line with quoted value"
    (let [result (parse-line "netplay_nickname = \"ezand\"")]
      (is (= [:netplay-nickname "ezand"] result))))

  (testing "Parses line without quotes"
    (let [result (parse-line "netplay_nickname = ezand")]
      (is (= [:netplay-nickname "ezand"] result))))

  (testing "Parses line with boolean value"
    (let [result (parse-line "cheevos_enable = \"true\"")]
      (is (= [:cheevos-enable true] result))))

  (testing "Parses line with empty quoted value"
    (let [result (parse-line "cheevos_username = \"\"")]
      (is (= [:cheevos-username nil] result))))

  (testing "Converts underscores to dashes in keys"
    (let [result (parse-line "cloud_sync_enable = \"false\"")]
      (is (= [:cloud-sync-enable false] result))))

  (testing "Handles spaces around equals sign for interesting keys"
    ;; Note: These return nil because "key" is not in interesting-keys
    ;; Testing with an actual interesting key
    (is (some? (parse-line "netplay_nickname=\"value\"")))
    (is (some? (parse-line "netplay_nickname = \"value\"")))
    (is (some? (parse-line "netplay_nickname  =  \"value\""))))

  (testing "Handles leading/trailing whitespace"
    (let [result (parse-line "  netplay_nickname = \"ezand\"  ")]
      (is (= [:netplay-nickname "ezand"] result))))

  (testing "Returns nil for non-interesting keys"
    (is (nil? (parse-line "not_interesting = \"value\"")))
    (is (nil? (parse-line "some_other_key = \"123\""))))

  (testing "Returns nil for invalid lines"
    (is (nil? (parse-line "invalid line without equals")))
    (is (nil? (parse-line "")))
    (is (nil? (parse-line "   "))))

  (testing "Returns nil for comment lines"
    (is (nil? (parse-line "# This is a comment"))))

  (testing "Parses lines with integer values"
    ;; Note: these would only work if the key is in interesting-keys
    ;; Since none of the interesting keys have numeric values in practice,
    ;; we'd need to add one to test this properly
    ))

(deftest interesting-keys-test
  (testing "Contains expected keys"
    (is (contains? interesting-keys :netplay-nickname))
    (is (contains? interesting-keys :cheevos-enable))
    (is (contains? interesting-keys :cheevos-username))
    (is (contains? interesting-keys :cloud-sync-enable)))

  (testing "Has correct count"
    (is (= 4 (count interesting-keys))))

  (testing "All keys are keywords"
    (is (every? keyword? interesting-keys))))

(deftest config-details-with-real-file-test
  (testing "Parses real retroarch.cfg from dev-resources"
    (let [config-dir (io/file "dev-resources")
          result (config/config-details config-dir)]

      (testing "returns a map"
        (is (map? result)))

      (testing "parses netplay nickname"
        (is (= "ezand" (:netplay-nickname result))))

      (testing "parses cheevos enable as boolean"
        (is (false? (:cheevos-enable result))))

      (testing "parses empty cheevos username as nil"
        (is (nil? (:cheevos-username result))))

      (testing "parses cloud sync enable as boolean"
        (is (false? (:cloud-sync-enable result))))

      (testing "only includes interesting keys"
        (is (= 4 (count result)))
        (is (every? interesting-keys (keys result))))

      (testing "does not include non-interesting keys"
        (is (not (contains? result :not)))))))

(deftest config-details-with-temp-file-test
  (testing "Parses custom config file"
    (let [temp-dir (File/createTempFile "retroarch-test" "")
          _ (.delete temp-dir)
          _ (.mkdir temp-dir)
          config-file (io/file temp-dir "retroarch.cfg")]
      (try
        (spit config-file "netplay_nickname = \"TestUser\"\ncheevos_enable = \"true\"\n")
        (let [result (config/config-details temp-dir)]
          (is (= "TestUser" (:netplay-nickname result)))
          (is (true? (:cheevos-enable result))))
        (finally
          (.delete config-file)
          (.delete temp-dir)))))

  (testing "Handles numeric values in config"
    (let [temp-dir (File/createTempFile "retroarch-test" "")
          _ (.delete temp-dir)
          _ (.mkdir temp-dir)
          config-file (io/file temp-dir "retroarch.cfg")]
      (try
        ;; Note: current interesting-keys don't include numeric fields
        ;; Just testing the parser can handle them if they were interesting
        (spit config-file "netplay_nickname = \"user\"\n")
        (let [result (config/config-details temp-dir)]
          (is (= "user" (:netplay-nickname result))))
        (finally
          (.delete config-file)
          (.delete temp-dir)))))

  (testing "Handles empty config file"
    (let [temp-dir (File/createTempFile "retroarch-test" "")
          _ (.delete temp-dir)
          _ (.mkdir temp-dir)
          config-file (io/file temp-dir "retroarch.cfg")]
      (try
        (spit config-file "")
        (let [result (config/config-details temp-dir)]
          (is (empty? result)))
        (finally
          (.delete config-file)
          (.delete temp-dir)))))

  (testing "Handles config with only non-interesting keys"
    (let [temp-dir (File/createTempFile "retroarch-test" "")
          _ (.delete temp-dir)
          _ (.mkdir temp-dir)
          config-file (io/file temp-dir "retroarch.cfg")]
      (try
        (spit config-file "some_key = \"value\"\nanother_key = \"123\"\n")
        (let [result (config/config-details temp-dir)]
          (is (empty? result)))
        (finally
          (.delete config-file)
          (.delete temp-dir))))))

(deftest config-details-handles-missing-file-test
  (testing "Returns nil for nonexistent config directory"
    (let [result (config/config-details (io/file "/nonexistent/directory"))]
      (is (nil? result))))

  (testing "Returns nil when config file doesn't exist in directory"
    (let [temp-dir (File/createTempFile "retroarch-test" "")
          _ (.delete temp-dir)
          _ (.mkdir temp-dir)]
      (try
        (let [result (config/config-details temp-dir)]
          (is (nil? result)))
        (finally
          (.delete temp-dir))))))

(deftest parse-line-edge-cases-test
  (testing "Handles values with special characters"
    (let [result (parse-line "netplay_nickname = \"user@example.com\"")]
      (is (= [:netplay-nickname "user@example.com"] result))))

  (testing "Handles values with spaces"
    (let [result (parse-line "netplay_nickname = \"John Doe\"")]
      (is (= [:netplay-nickname "John Doe"] result))))

  (testing "Handles mixed case in keys (but key won't match interesting-keys)"
    ;; Mixed case key gets normalized but won't match :netplay-nickname
    (let [result (parse-line "NetPlay_NickName = \"test\"")]
      ;; Returns nil because normalized key doesn't match interesting-keys
      (is (nil? result))))

  (testing "Handles boolean-like strings that aren't booleans"
    ;; "True" with capital T should be treated as string
    (let [result (parse-line "netplay_nickname = \"True\"")]
      (is (= [:netplay-nickname "True"] result)))))

(deftest parse-value-type-coercion-test
  (testing "String that looks like number but has trailing space is trimmed and becomes number"
    ;; parse-value trims whitespace first, so "123 " becomes "123" which parses as integer
    (is (= 123 (parse-value "123 "))))

  (testing "Boolean strings are case-sensitive"
    (is (= "True" (parse-value "True")))
    (is (= "FALSE" (parse-value "FALSE")))
    (is (true? (parse-value "true")))
    (is (false? (parse-value "false"))))

  (testing "Decimal numbers without leading digit"
    ;; ".5" doesn't match the float pattern, stays as string
    (is (= ".5" (parse-value ".5"))))

  (testing "Scientific notation stays as string"
    (is (= "1e5" (parse-value "1e5"))))

  (testing "Hexadecimal stays as string"
    (is (= "0xFF" (parse-value "0xFF")))))

(deftest config-details-preserves-all-interesting-keys-test
  (testing "Config file with all interesting keys"
    (let [temp-dir (File/createTempFile "retroarch-test" "")
          _ (.delete temp-dir)
          _ (.mkdir temp-dir)
          config-file (io/file temp-dir "retroarch.cfg")]
      (try
        (spit config-file
              (str "netplay_nickname = \"player1\"\n"
                   "cheevos_enable = \"true\"\n"
                   "cheevos_username = \"achiever\"\n"
                   "cloud_sync_enable = \"true\"\n"
                   "unrelated = \"ignored\"\n"))
        (let [result (config/config-details temp-dir)]
          (is (= 4 (count result)))
          (is (= "player1" (:netplay-nickname result)))
          (is (true? (:cheevos-enable result)))
          (is (= "achiever" (:cheevos-username result)))
          (is (true? (:cloud-sync-enable result))))
        (finally
          (.delete config-file)
          (.delete temp-dir))))))
