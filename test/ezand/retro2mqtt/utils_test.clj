(ns ezand.retro2mqtt.utils-test
  (:require [clojure.test :refer [deftest is testing]]
            [ezand.retro2mqtt.logger :as log]
            [ezand.retro2mqtt.utils :as util]
            [superstring.core :as str]))

(def ^:private test-logger (log/create-logger! *ns*))

(deftest bool->toggle-str-test
  (testing "Converts true to 'on'"
    (is (= "on" (util/bool->toggle-str true))))

  (testing "Converts false to 'off'"
    (is (= "off" (util/bool->toggle-str false))))

  (testing "Returns nil for nil input"
    (is (nil? (util/bool->toggle-str nil))))

  (testing "Handles truthy values"
    ;; Note: The function only checks if bool-value is some? and truthy
    ;; But it's specifically designed for booleans
    (is (= "on" (util/bool->toggle-str true)))
    (is (= "off" (util/bool->toggle-str false)))))

(deftest filename-without-extension-test
  (testing "Removes single extension"
    (is (= "file" (util/filename-without-extension "file.txt")))
    (is (= "document" (util/filename-without-extension "document.pdf")))
    (is (= "image" (util/filename-without-extension "image.png"))))

  (testing "Removes extension from path"
    (is (= "file" (util/filename-without-extension "/path/to/file.txt")))
    (is (= "file" (util/filename-without-extension "/path/to/file.smc"))))

  (testing "Handles files without extension"
    (is (= "README" (util/filename-without-extension "README")))
    (is (= "Makefile" (util/filename-without-extension "/path/Makefile"))))

  (testing "Handles multiple dots in filename"
    ;; Only removes the last extension
    (is (= "archive.tar" (util/filename-without-extension "archive.tar.gz")))
    (is (= "file.backup" (util/filename-without-extension "file.backup.txt"))))

  (testing "Handles hidden files"
    ;; Hidden files without extension: the dot is considered part of filename and removed
    (is (= "" (util/filename-without-extension ".gitignore")))
    ;; Hidden files with extension: removes the extension
    (is (= ".config" (util/filename-without-extension ".config.json"))))

  (testing "Handles files with spaces"
    (is (= "My File" (util/filename-without-extension "My File.txt")))
    (is (= "Game (U) [!]" (util/filename-without-extension "Game (U) [!].zip")))))

(deftest trim-to-nil-test
  (testing "Trims whitespace from string"
    (is (= "hello" (util/trim-to-nil "  hello  ")))
    (is (= "test" (util/trim-to-nil "\ttest\n")))
    (is (= "value" (util/trim-to-nil "   value   "))))

  (testing "Returns nil for nil input"
    (is (nil? (util/trim-to-nil nil))))

  (testing "Returns nil for empty string"
    (is (nil? (util/trim-to-nil ""))))

  (testing "Returns nil for whitespace-only string"
    (is (nil? (util/trim-to-nil "   ")))
    (is (nil? (util/trim-to-nil "\t")))
    (is (nil? (util/trim-to-nil "\n")))
    (is (nil? (util/trim-to-nil "  \t\n  "))))

  (testing "Preserves non-whitespace content"
    (is (= "a" (util/trim-to-nil "a")))
    (is (= "hello world" (util/trim-to-nil "hello world"))))

  (testing "Handles strings with internal whitespace"
    (is (= "hello world" (util/trim-to-nil "  hello world  ")))))

(deftest homeassistant-config-topic-test
  (testing "Generates correct topic for simple ID"
    (is (= "homeassistant/sensor/test/config"
           (util/homeassistant-config-topic "test"))))

  (testing "Generates correct topic for complex ID"
    (is (= "homeassistant/sensor/retroarch_status/config"
           (util/homeassistant-config-topic "retroarch_status"))))

  (testing "Handles IDs with special characters"
    (is (= "homeassistant/sensor/my-sensor/config"
           (util/homeassistant-config-topic "my-sensor")))
    (is (= "homeassistant/sensor/sensor_123/config"
           (util/homeassistant-config-topic "sensor_123"))))

  (testing "Topic follows Home Assistant convention"
    (let [topic (util/homeassistant-config-topic "example")]
      (is (.startsWith topic "homeassistant/"))
      (is (.contains topic "/sensor/"))
      (is (.endsWith topic "/config")))))

(deftest current-os-test
  (testing "current-os is a keyword"
    (is (keyword? util/current-os)))

  (testing "current-os is either :windows or :unix-style"
    (is (contains? #{:windows :unix-style} util/current-os)))

  (testing "current-os matches actual OS"
    (let [os-name (System/getProperty "os.name")]
      (if (.contains (.toLowerCase os-name) "windows")
        (is (= :windows util/current-os))
        (is (= :unix-style util/current-os))))))

(deftest with-suppressed-errors-macro-test
  (testing "Returns result when no error occurs"
    (is (= 42 (util/with-suppressed-errors test-logger "test"
                (+ 40 2)))))

  (testing "Returns nil when error occurs"
    (is (nil? (util/with-suppressed-errors test-logger "test"
                (throw (Exception. "Test error"))))))

  (testing "Suppresses errors and continues execution"
    (let [result (util/with-suppressed-errors test-logger "test"
                   (/ 1 0))]
      (is (nil? result))))

  (testing "Works with multiple expressions"
    (is (= 3 (util/with-suppressed-errors test-logger "test"
               (println "Side effect")
               (+ 1 2)))))

  (testing "Logs errors with provided message"
    ;; Just verify it doesn't throw - actual logging is tested in logger tests
    (is (nil? (util/with-suppressed-errors test-logger "Custom error message"
                (throw (RuntimeException. "Boom"))))))

  (testing "Catches all Throwable types"
    (is (nil? (util/with-suppressed-errors test-logger "test"
                (throw (Error. "Fatal error")))))
    (is (nil? (util/with-suppressed-errors test-logger "test"
                (throw (RuntimeException. "Runtime error")))))
    (is (nil? (util/with-suppressed-errors test-logger "test"
                (throw (IllegalArgumentException. "Invalid arg")))))))

(deftest filename-without-extension-edge-cases-test
  (testing "Handles empty extension (dot at end)"
    ;; Dot at end is not removed by the regex (it requires at least one char after dot)
    (is (= "file." (util/filename-without-extension "file."))))

  (testing "Handles very long extensions"
    (is (= "file" (util/filename-without-extension "file.verylongextension"))))

  (testing "Handles paths with dots in directory names"
    (is (= "file" (util/filename-without-extension "/path.with.dots/file.txt")))))

(deftest trim-to-nil-preserves-meaningful-whitespace-test
  (testing "Preserves internal whitespace"
    (is (= "hello   world" (util/trim-to-nil "  hello   world  "))))

  (testing "Handles tabs and newlines in content"
    (is (= "line1\nline2" (util/trim-to-nil "line1\nline2"))))

  (testing "Single non-whitespace character"
    (is (= "x" (util/trim-to-nil "   x   ")))))

(deftest homeassistant-config-topic-format-test
  (testing "Topic has exactly 4 segments"
    (let [topic (util/homeassistant-config-topic "test")
          segments (str/split topic #"/")]
      (is (= 4 (count segments)))
      (is (= "homeassistant" (first segments)))
      (is (= "sensor" (second segments)))
      (is (= "test" (nth segments 2)))
      (is (= "config" (last segments))))))

(deftest topic-matches?-test
  (testing "Exact match without wildcards"
    (is (util/topic-matches? "sensor/temperature" "sensor/temperature"))
    (is (not (util/topic-matches? "sensor/temperature" "sensor/humidity")))
    (is (not (util/topic-matches? "sensor/temperature" "sensor/temperature/extra"))))

  (testing "Single-level wildcard (+)"
    (is (util/topic-matches? "sensor/+/temperature" "sensor/living-room/temperature"))
    (is (util/topic-matches? "sensor/+/temperature" "sensor/bedroom/temperature"))
    (is (not (util/topic-matches? "sensor/+/temperature" "sensor/living-room/bedroom/temperature")))
    (is (not (util/topic-matches? "sensor/+" "sensor/living-room/temperature")))
    (is (util/topic-matches? "+/temperature" "sensor/temperature"))
    (is (util/topic-matches? "sensor/+" "sensor/temperature")))

  (testing "Multi-level wildcard (#)"
    (is (util/topic-matches? "sensor/#" "sensor/living-room/temperature"))
    (is (util/topic-matches? "sensor/#" "sensor/temperature"))
    (is (util/topic-matches? "sensor/#" "sensor/a/b/c/d"))
    (is (not (util/topic-matches? "sensor/#" "other/temperature")))
    (is (util/topic-matches? "#" "any/topic/at/all"))
    (is (util/topic-matches? "audio_events/event/#" "audio_events/event/foo"))
    (is (util/topic-matches? "audio_events/event/#" "audio_events/event/foo/bar")))

  (testing "Combined wildcards"
    (is (util/topic-matches? "sensor/+/temperature/#" "sensor/living-room/temperature/reading"))
    (is (util/topic-matches? "sensor/+/temperature/#" "sensor/bedroom/temperature/current/value"))
    (is (not (util/topic-matches? "sensor/+/temperature/#" "sensor/living-room/humidity"))))

  (testing "Edge cases"
    (is (util/topic-matches? "" ""))
    (is (not (util/topic-matches? "sensor" "")))
    (is (not (util/topic-matches? "sensor" "sensor/foo")))
    (is (util/topic-matches? "sensor/#" "sensor/foo"))
    (is (not (util/topic-matches? "" "sensor")))
    (is (util/topic-matches? "#" ""))
    (is (util/topic-matches? "sensor" "sensor")))

  (testing "Multi-level wildcard only matches from its position"
    (is (not (util/topic-matches? "sensor/#" "other/sensor/temp")))
    (is (util/topic-matches? "sensor/#" "sensor"))))
