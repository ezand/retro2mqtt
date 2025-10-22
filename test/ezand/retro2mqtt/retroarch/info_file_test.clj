(ns ezand.retro2mqtt.retroarch.info-file-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ezand.retro2mqtt.retroarch.info-file :as info])
  (:import (java.io File)))

(deftest parse-info-with-real-file-test
  (testing "Parse real bsnes2014 info file"
    (let [info-file (io/file (io/resource "bsnes2014_accuracy_libretro.info"))
          result (info/parse-info info-file)]

      (testing "returns a map"
        (is (map? result))
        (is (seq result)))

      (testing "parses display name"
        (is (= "Nintendo - SNES / SFC (bsnes 2014 Accuracy)" (:display-name result))))

      (testing "parses categories"
        (is (= "Emulator" (:categories result))))

      (testing "parses authors"
        (is (= "Near" (:authors result))))

      (testing "parses corename"
        (is (= "bsnes 2014 Accuracy" (:corename result))))

      (testing "parses supported extensions"
        (is (= "sfc|smc|bml|gb|gbc|st|bs" (:supported-extensions result))))

      (testing "parses license"
        (is (= "GPLv3" (:license result))))

      (testing "parses display version"
        (is (= "v094 (Accuracy)" (:display-version result))))

      (testing "parses hardware information"
        (is (= "Nintendo" (:manufacturer result)))
        (is (= "Super Nintendo Entertainment System" (:systemname result)))
        (is (= "super_nes" (:systemid result))))

      (testing "parses firmware count"
        (is (= "18" (:firmware-count result))))

      (testing "parses firmware entries"
        (is (= "dsp1.data.rom" (:firmware0-desc result)))
        (is (= "dsp1.data.rom" (:firmware0-path result)))
        (is (= "true" (:firmware0-opt result))))

      (testing "parses libretro features"
        (is (= "true" (:savestate result)))
        (is (= "serialized" (:savestate-features result)))
        (is (= "true" (:cheats result)))
        (is (= "false" (:hw-render result))))

      (testing "parses database"
        (is (= "Nintendo - Super Nintendo Entertainment System|Nintendo - Sufami Turbo|Nintendo - Satellaview"
               (:database result))))

      (testing "parses long description"
        (is (string? (:description result)))
        (is (.contains (:description result) "bsnes 2014 Accuracy")))

      (testing "parses long notes with pipes"
        (is (string? (:notes result)))
        (is (.contains (:notes result) "Super Mario Kart")))

      (testing "converts keys to lisp-case"
        (is (contains? result :display-name))
        (is (contains? result :supported-extensions))
        (is (contains? result :firmware-count))
        (is (not (contains? result :display_name)))
        (is (not (contains? result :supported_extensions)))))))

(deftest parse-info-handles-empty-values-test
  (testing "Handles empty quoted values"
    (let [temp-file (File/createTempFile "test-info" ".info")]
      (try
        (spit temp-file "permissions = \"\"\nempty_field = \"\"\n")
        (let [result (info/parse-info temp-file)]
          (is (nil? (:permissions result)))
          (is (nil? (:empty-field result))))
        (finally
          (.delete temp-file))))))

(deftest parse-info-ignores-comments-test
  (testing "Ignores comment lines"
    (let [temp-file (File/createTempFile "test-info" ".info")]
      (try
        (spit temp-file "# This is a comment\nkey = \"value\"\n# Another comment\nkey2 = \"value2\"\n")
        (let [result (info/parse-info temp-file)]
          (is (= "value" (:key result)))
          (is (= "value2" (:key2 result)))
          (is (= 2 (count result))))
        (finally
          (.delete temp-file))))))

(deftest parse-info-ignores-blank-lines-test
  (testing "Ignores blank lines"
    (let [temp-file (File/createTempFile "test-info" ".info")]
      (try
        (spit temp-file "key1 = \"value1\"\n\n\nkey2 = \"value2\"\n")
        (let [result (info/parse-info temp-file)]
          (is (= "value1" (:key1 result)))
          (is (= "value2" (:key2 result)))
          (is (= 2 (count result))))
        (finally
          (.delete temp-file))))))

(deftest parse-info-handles-unquoted-values-test
  (testing "Handles values without quotes"
    (let [temp-file (File/createTempFile "test-info" ".info")]
      (try
        (spit temp-file "count = 5\nflag = true\n")
        (let [result (info/parse-info temp-file)]
          (is (= "5" (:count result)))
          (is (= "true" (:flag result))))
        (finally
          (.delete temp-file))))))

(deftest parse-info-handles-spaces-around-equals-test
  (testing "Handles various spacing around equals sign"
    (let [temp-file (File/createTempFile "test-info" ".info")]
      (try
        (spit temp-file "key1=\"value1\"\nkey2 = \"value2\"\nkey3  =  \"value3\"\n")
        (let [result (info/parse-info temp-file)]
          (is (= "value1" (:key1 result)))
          (is (= "value2" (:key2 result)))
          (is (= "value3" (:key3 result))))
        (finally
          (.delete temp-file))))))

(deftest parse-info-handles-quotes-in-value-test
  (testing "Preserves content within quotes"
    (let [temp-file (File/createTempFile "test-info" ".info")]
      (try
        (spit temp-file "name = \"Nintendo - SNES / SFC (test)\"\n")
        (let [result (info/parse-info temp-file)]
          (is (= "Nintendo - SNES / SFC (test)" (:name result))))
        (finally
          (.delete temp-file))))))

(deftest parse-info-handles-invalid-lines-test
  (testing "Ignores lines without equals sign"
    (let [temp-file (File/createTempFile "test-info" ".info")]
      (try
        (spit temp-file "valid_key = \"value\"\ninvalid line without equals\nkey2 = \"value2\"\n")
        (let [result (info/parse-info temp-file)]
          (is (= "value" (:valid-key result)))
          (is (= "value2" (:key2 result)))
          (is (= 2 (count result))))
        (finally
          (.delete temp-file))))))

(deftest parse-info-handles-nonexistent-file-test
  (testing "Returns nil for nonexistent file"
    (let [result (info/parse-info (io/file "/nonexistent/path/file.info"))]
      (is (nil? result))))

  (testing "Returns nil for nil input"
    (let [result (info/parse-info nil)]
      (is (nil? result)))))

(deftest parse-info-converts-underscore-to-dash-test
  (testing "Converts underscores to dashes in keys"
    (let [temp-file (File/createTempFile "test-info" ".info")]
      (try
        (spit temp-file "display_name = \"Test\"\nsupported_extensions = \"smc\"\nfirmware_count = \"1\"\n")
        (let [result (info/parse-info temp-file)]
          (is (contains? result :display-name))
          (is (contains? result :supported-extensions))
          (is (contains? result :firmware-count))
          (is (not (contains? result :display_name)))
          (is (not (contains? result :supported_extensions))))
        (finally
          (.delete temp-file))))))

(deftest parse-info-handles-multiline-values-test
  (testing "Handles long single-line values"
    (let [info-file (io/file (io/resource "bsnes2014_accuracy_libretro.info"))
          result (info/parse-info info-file)
          notes (:notes result)]
      (is (string? notes))
      (is (> (count notes) 100))
      ;; Notes field contains pipes as separators
      (is (.contains notes "|")))))

(deftest parse-info-trims-whitespace-test
  (testing "Trims whitespace from keys and values"
    (let [temp-file (File/createTempFile "test-info" ".info")]
      (try
        (spit temp-file "  key  =  \"  value  \"  \n")
        (let [result (info/parse-info temp-file)]
          (is (= "  value  " (:key result))))
        (finally
          (.delete temp-file))))))

(deftest parse-info-real-world-edge-cases-test
  (testing "Handles real-world bsnes info file edge cases"
    (let [info-file (io/file (io/resource "bsnes2014_accuracy_libretro.info"))
          result (info/parse-info info-file)]

      (testing "handles empty permissions field"
        (is (nil? (:permissions result))))

      (testing "handles numbered firmware keys"
        (is (= "dsp1.data.rom" (:firmware0-desc result)))
        (is (= "sgb.boot.rom (SGB Boot Image)" (:firmware17-desc result))))

      (testing "handles database with pipes"
        (is (string? (:database result)))
        (is (.contains (:database result) "|")))

      (testing "all parsed keys are keywords"
        (is (every? keyword? (keys result))))

      (testing "no empty string values exist"
        (is (every? #(or (nil? %) (seq %)) (vals result)))))))
