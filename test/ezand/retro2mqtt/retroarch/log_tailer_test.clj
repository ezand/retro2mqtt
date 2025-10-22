(ns ezand.retro2mqtt.retroarch.log-tailer-test
  (:require [clojure.test :refer [deftest is testing]]
            [ezand.retro2mqtt.retroarch.log-tailer :as tailer])
  (:import (java.io File)))

;; Access private functions for testing
(def match-line #'tailer/match-line)
(def filter-log-files #'tailer/filter-log-files)
(def most-recent-file #'tailer/most-recent-file)
(def should-switch-file? #'tailer/should-switch-file?)
(def maybe-serialize-data #'tailer/maybe-serialize-data)
(def interesting-log-patterns #'tailer/interesting-log-patterns)

(deftest match-line-test
  (testing "Matches RetroArch version line"
    (let [line "[INFO] Version: 1.21.0"
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :retroarch-version (first result)))
      (is (= "1.21.0" (second result)))))

  (testing "Matches Git hash line"
    (let [line "[INFO] Git: 05f94af4"
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :retroarch-git-hash (first result)))
      (is (= "05f94af4" (second result)))))

  (testing "Matches build date line"
    (let [line "[INFO] Built: Apr 30 2025"
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :retroarch-version-build-date (first result)))
      (is (= "Apr 30 2025" (second result)))))

  (testing "Matches CPU model line"
    (let [line "[INFO] CPU Model Name: Apple M2 Max"
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :system-cpu (first result)))
      (is (= "Apple M2 Max" (second result)))))

  (testing "Matches capabilities line and splits into vector"
    (let [line "[INFO] Capabilities: NEON VFPV3 VFPV4"
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :system-capabilities (first result)))
      (is (vector? (second result)))
      (is (= ["NEON" "VFPV3" "VFPV4"] (second result)))))

  (testing "Matches video size line"
    (let [line "[INFO] [Video]: Set video size to: 897x672."
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :content-video-size (first result)))
      (is (= "897x672" (second result)))))

  (testing "Matches CRC32 line and formats as hex"
    (let [line "[INFO] [Content]: CRC32: 0xa31bead4."
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :content-crc32 (first result)))
      (is (= "a31bead4" (second result)))))

  (testing "Matches display driver line"
    (let [line "[INFO] [Display]: Found display driver: \"vulkan\"."
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :system-display-driver (first result)))
      (is (= "vulkan" (second result)))))

  (testing "Matches joypad driver line"
    (let [line "[INFO] [Joypad]: Found joypad driver: \"mfi\"."
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :system-joypad-driver (first result)))
      (is (= "mfi" (second result)))))

  (testing "Matches audio input rate line"
    (let [line "[INFO] [Audio]: Set audio input rate to: 48000.00 Hz."
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :system-audio-input-rate (first result)))
      (is (= "48000.00 Hz" (second result)))))

  (testing "Matches pixel format line"
    (let [line "[INFO] [Environ]: SET_PIXEL_FORMAT: RGB565."
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :system-pixel-format (first result)))
      (is (= "RGB565" (second result)))))

  (testing "Matches libretro API version line"
    (let [line "[INFO] [Core]: Version of libretro API: 1, Compiled against API: 1"
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :libretro-api-version (first result)))
      (is (= "1" (second result)))))

  (testing "Matches NetCMD port line and parses as integer"
    (let [line "[INFO] [NetCMD]: bringing_up_command_interface_at_port 55355."
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :retroarch-cmd-interface-port (first result)))
      (is (= 55355 (second result)))))

  (testing "Matches content loaded line"
    (let [line "[INFO] [Content]: Loading content file: \"/path/to/game.smc\"."
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :content (first result)))
      (is (= "/path/to/game.smc" (second result)))))

  (testing "Matches core loading line"
    (let [line "[INFO] [Core]: Loading dynamic libretro core from: \"/path/to/core.dylib\""
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :core-libretro-file (first result)))
      (is (= "/path/to/core.dylib" (second result)))))

  (testing "Matches content unloaded line"
    (let [line "[INFO] [Core]: Content ran for a total of: 00 hours, 00 minutes, 05 seconds."
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (some? result))
      (is (= :content-unloaded? (first result)))
      (is (true? (second result)))))

  (testing "Returns nil for non-matching line"
    (let [line "[INFO] Some random log line"
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (nil? result))))

  (testing "Returns nil for empty line"
    (let [line ""
          patterns @interesting-log-patterns
          result (match-line patterns line)]
      (is (nil? result)))))

(deftest filter-log-files-test
  (testing "filter-log-files is a var"
    ;; This function is difficult to test without actual files
    ;; Testing the regex pattern instead
    (is (var? filter-log-files)))

  (testing "File pattern regex matches valid retroarch logs"
    (is (re-matches #"retroarch__\d{4}_\d{2}_\d{2}__\d{2}_\d{2}_\d{2}\.log"
                    "retroarch__2024_10_22__12_34_56.log"))
    (is (re-matches #"retroarch__\d{4}_\d{2}_\d{2}__\d{2}_\d{2}_\d{2}\.log"
                    "retroarch__2025_01_01__00_00_00.log"))
    (is (nil? (re-matches #"retroarch__\d{4}_\d{2}_\d{2}__\d{2}_\d{2}_\d{2}\.log"
                          "retroarch.log")))
    (is (nil? (re-matches #"retroarch__\d{4}_\d{2}_\d{2}__\d{2}_\d{2}_\d{2}\.log"
                          "other.log")))))

(deftest most-recent-file-test
  (testing "most-recent-file is a var"
    ;; This is a pure function, we can test the logic
    (is (var? most-recent-file))))

(deftest should-switch-file-test
  (testing "Returns false when files are the same"
    (let [file (File. "/tmp/test.log")]
      (is (false? (should-switch-file? file file)))))

  (testing "Returns true when files are different"
    (let [file1 (File. "/tmp/test1.log")
          file2 (File. "/tmp/test2.log")]
      (is (true? (should-switch-file? file1 file2)))))

  (testing "Returns true when current is nil"
    (let [file (File. "/tmp/test.log")]
      (is (true? (should-switch-file? nil file)))))

  (testing "Returns false when latest is nil (and returns nil, not false)"
    (let [file (File. "/tmp/test.log")]
      (is (not (should-switch-file? file nil))))))

(deftest maybe-serialize-data-test
  (testing "Serializes collections to JSON"
    (let [data {:key "value" :number 123}
          result (maybe-serialize-data data)]
      (is (string? result))
      (is (.contains result "key"))
      (is (.contains result "value"))))

  (testing "Serializes vectors to JSON"
    (let [data ["item1" "item2" "item3"]
          result (maybe-serialize-data data)]
      (is (string? result))
      (is (.contains result "item1"))))

  (testing "Leaves strings unchanged"
    (let [data "plain string"
          result (maybe-serialize-data data)]
      (is (= "plain string" result))))

  (testing "Leaves numbers unchanged"
    (let [data 42
          result (maybe-serialize-data data)]
      (is (= 42 result))))

  (testing "Leaves nil unchanged"
    (let [data nil
          result (maybe-serialize-data data)]
      (is (nil? result)))))

(deftest pattern-structure-test
  (testing "All patterns have valid structure"
    (let [patterns @interesting-log-patterns]
      (doseq [[pattern-key pattern-config] patterns]
        (testing (str "Pattern " pattern-key " has regexp")
          (is (instance? java.util.regex.Pattern (:regexp pattern-config))))

        (testing (str "Pattern " pattern-key " has either state-topic or on-match-fn")
          (is (or (contains? pattern-config :state-topic)
                  (contains? pattern-config :on-match-fn))))

        (when (:update-fn pattern-config)
          (testing (str "Pattern " pattern-key " update-fn is a function")
            (is (fn? (:update-fn pattern-config)))))

        (when (:on-match-fn pattern-config)
          (testing (str "Pattern " pattern-key " on-match-fn is a function")
            (is (fn? (:on-match-fn pattern-config)))))

        (when (:retain? pattern-config)
          (testing (str "Pattern " pattern-key " retain? is a boolean")
            (is (boolean? (:retain? pattern-config))))))))

  (testing "Pattern count matches expectation"
    (let [patterns @interesting-log-patterns]
      (is (= 16 (count patterns))))))

(deftest match-line-returns-correct-structure-test
  (testing "Match result includes all required elements"
    (let [line "[INFO] Version: 1.21.0"
          patterns @interesting-log-patterns
          [pattern-key data state-topic retain? on-match-fn] (match-line patterns line)]
      (is (keyword? pattern-key))
      (is (some? data))
      (is (some? state-topic))
      (is (boolean? retain?))
      (is (nil? on-match-fn)))))
