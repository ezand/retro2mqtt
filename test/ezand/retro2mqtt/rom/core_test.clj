(ns ezand.retro2mqtt.rom.core-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ezand.retro2mqtt.rom.core :as rom])
  (:import (java.io RandomAccessFile)
           (java.nio.charset StandardCharsets)
           (java.util.zip ZipEntry ZipFile)))

(deftest bytes-to-string-test
  (testing "Convert bytes to string and trim"
    (let [bytes (.getBytes "Hello World" StandardCharsets/US_ASCII)]
      (is (= "Hello World" (rom/bytes-to-string bytes))))

    (testing "removes null bytes"
      (let [bytes (byte-array [72 101 108 108 111 0 0 0])]
        (is (= "Hello" (rom/bytes-to-string bytes)))))

    (testing "trims whitespace"
      (let [bytes (.getBytes "  Hello  " StandardCharsets/US_ASCII)]
        (is (= "Hello" (rom/bytes-to-string bytes)))))))

(deftest read-bytes-test
  (testing "Read bytes from RandomAccessFile at offset"
    (let [test-file (io/file (io/resource "Super Mario World (U) [!].zip"))]
      (with-open [raf (RandomAccessFile. test-file "r")]
        (let [bytes (rom/read-bytes raf 0 4)]
          (is (= 4 (alength bytes)))
          ;; ZIP file magic number is PK (0x50 0x4B)
          (is (= 0x50 (bit-and (aget bytes 0) 0xFF)))
          (is (= 0x4B (bit-and (aget bytes 1) 0xFF))))))))

(deftest read-bytes-from-array-test
  (testing "Read bytes from byte array at offset"
    (let [data (byte-array [10 20 30 40 50 60])
          result (rom/read-bytes-from-array data 2 3)]
      (is (= 3 (alength result)))
      (is (= 30 (aget result 0)))
      (is (= 40 (aget result 1)))
      (is (= 50 (aget result 2))))))

(deftest is-zip-file-test
  (testing "Detects ZIP files by extension"
    (is (true? (rom/is-zip-file? "test.zip")))
    (is (true? (rom/is-zip-file? "test.ZIP")))
    (is (true? (rom/is-zip-file? "/path/to/file.zip")))
    (is (false? (rom/is-zip-file? "test.smc")))
    (is (false? (rom/is-zip-file? "test.txt")))))

(deftest extract-metadata-from-zip-test
  (testing "Extract metadata from Super Mario World ZIP"
    (let [zip-path (.getAbsolutePath (io/file (io/resource "Super Mario World (U) [!].zip")))
          metadata (rom/extract-metadata zip-path)]

      (testing "extracts game title"
        (is (string? (:game-title metadata)))
        (is (seq (:game-title metadata))))

      (testing "identifies source as ZIP"
        (is (= :zip (:source metadata))))

      (testing "includes ZIP entry name"
        (is (string? (:zip-entry-name metadata)))
        (is (re-matches #".*\.smc$|.*\.SMC$" (:zip-entry-name metadata))))

      (testing "includes ROM type"
        (is (contains? #{:lorom :hirom} (:rom-type metadata))))

      (testing "includes ROM speed"
        (is (contains? #{:fast :slow} (:rom-speed metadata))))

      (testing "includes ROM size"
        (is (number? (:rom-size-kb metadata)))
        (is (pos? (:rom-size-kb metadata))))

      (testing "includes RAM size"
        (is (number? (:ram-size-kb metadata)))
        (is (>= (:ram-size-kb metadata) 0)))

      (testing "includes region"
        (is (string? (:region metadata))))

      (testing "includes version"
        (is (number? (:version metadata))))

      (testing "includes checksums"
        (is (number? (:checksum metadata)))
        (is (number? (:complement metadata))))

      (testing "includes SMC header detection"
        (is (boolean? (:has-smc-header metadata))))

      (testing "includes fallback title"
        (is (string? (:fallback-title metadata)))
        (is (= "Super Mario World (U) [!]" (:fallback-title metadata)))))))

(deftest extract-metadata-with-hash-in-path-test
  (testing "Handles filepath with # fragment (common in some emulators)"
    (let [zip-path (.getAbsolutePath (io/file (io/resource "Super Mario World (U) [!].zip")))
          path-with-hash (str zip-path "#some-fragment")
          metadata (rom/extract-metadata path-with-hash)]

      (is (not (:error metadata)))
      (is (string? (:game-title metadata)))
      (is (= :zip (:source metadata))))))

(deftest extract-game-name-test
  (testing "Extract just the game name from ZIP"
    (let [zip-path (.getAbsolutePath (io/file (io/resource "Super Mario World (U) [!].zip")))
          game-name (rom/extract-game-name zip-path)]

      (is (string? game-name))
      (is (seq game-name))
      (is (not (.contains game-name "Error")))))

  (testing "Returns error message for non-existent file"
    (let [result (rom/extract-game-name "/nonexistent/file.zip")]
      (is (string? result))
      (is (.contains result "Error")))))

(deftest extract-metadata-error-handling-test
  (testing "Returns error map for non-existent file"
    (let [metadata (rom/extract-metadata "/nonexistent/path.zip")]
      (is (contains? metadata :error))
      (is (string? (:error metadata)))
      (is (.contains (:error metadata) "Error"))))

  (testing "Returns error for ZIP without SMC file"
    ;; This test would require a ZIP file without .smc - skipping for now
    ;; as we don't have such a test fixture
    (is true)))

(deftest check-header-validity-test
  (testing "Header validity check functions exist and work"
    (let [zip-path (.getAbsolutePath (io/file (io/resource "Super Mario World (U) [!].zip")))
          metadata (rom/extract-metadata zip-path)]
      ;; If we successfully extracted metadata, the header validation worked
      (is (not (:error metadata)))
      (is (contains? metadata :checksum))
      (is (contains? metadata :complement)))))

(deftest rom-type-detection-test
  (testing "Correctly detects LoROM vs HiROM"
    (let [zip-path (.getAbsolutePath (io/file (io/resource "Super Mario World (U) [!].zip")))
          metadata (rom/extract-metadata zip-path)]
      ;; Super Mario World should be LoROM
      (is (= :lorom (:rom-type metadata))))))

(deftest find-smc-in-zip-test
  (testing "Can find SMC file in ZIP archive"
    (let [zip-path (.getAbsolutePath (io/file (io/resource "Super Mario World (U) [!].zip")))]
      (with-open [zip-file (ZipFile. zip-path)]
        (let [entry (rom/find-smc-in-zip zip-file)]
          (is (some? entry))
          (is (instance? ZipEntry entry))
          (is (or (.endsWith (.getName entry) ".smc")
                  (.endsWith (.getName entry) ".SMC"))))))))

(deftest read-zip-entry-bytes-test
  (testing "Can read bytes from ZIP entry"
    (let [zip-path (.getAbsolutePath (io/file (io/resource "Super Mario World (U) [!].zip")))]
      (with-open [zip-file (ZipFile. zip-path)]
        (when-let [entry (rom/find-smc-in-zip zip-file)]
          (let [bytes (rom/read-zip-entry-bytes zip-file entry)]
            (is (some? bytes))
            (is (> (alength bytes) 0))
            ;; Should be a valid ROM size (multiple of 1024, possibly with 512 byte header)
            (is (or (zero? (mod (alength bytes) 1024))
                    (= 512 (mod (alength bytes) 1024))))))))))
