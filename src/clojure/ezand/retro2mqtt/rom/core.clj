(ns ezand.retro2mqtt.rom.core
  (:require [clojure.java.io :as io]
            [ezand.retro2mqtt.utils :as util]
            [superstring.core :as str])
  (:import (java.io RandomAccessFile)
           (java.nio.charset StandardCharsets)
           (java.util.zip ZipEntry ZipFile)))

(defn read-bytes
  "Read bytes from file at given offset"
  [^RandomAccessFile raf offset length]
  (.seek raf offset)
  (let [buffer (byte-array length)]
    (.read raf buffer)
    buffer))

(defn read-bytes-from-array
  "Read bytes from byte array at given offset"
  [^bytes data offset length]
  (let [buffer (byte-array length)]
    (System/arraycopy data offset buffer 0 length)
    buffer))

(defn bytes-to-string
  "Convert byte array to string, trimming nulls and spaces"
  [bytes]
  (-> (String. bytes StandardCharsets/US_ASCII)
      (.replaceAll "\u0000" "")
      (.trim)))

(defn check-header-validity-raf
  "Check if header at offset looks valid by examining checksum (for RandomAccessFile)"
  [^RandomAccessFile raf offset]
  (let [checksum-bytes (read-bytes raf (+ offset 0x1C) 2)
        complement-bytes (read-bytes raf (+ offset 0x1E) 2)
        checksum (bit-or (bit-shift-left (bit-and (aget checksum-bytes 1) 0xFF) 8)
                         (bit-and (aget checksum-bytes 0) 0xFF))
        complement (bit-or (bit-shift-left (bit-and (aget complement-bytes 1) 0xFF) 8)
                           (bit-and (aget complement-bytes 0) 0xFF))]
    (= 0xFFFF (bit-xor checksum complement))))

(defn check-header-validity-array
  "Check if header at offset looks valid by examining checksum (for byte array)"
  [^bytes data offset]
  (let [checksum-bytes (read-bytes-from-array data (+ offset 0x1C) 2)
        complement-bytes (read-bytes-from-array data (+ offset 0x1E) 2)
        checksum (bit-or (bit-shift-left (bit-and (aget checksum-bytes 1) 0xFF) 8)
                         (bit-and (aget checksum-bytes 0) 0xFF))
        complement (bit-or (bit-shift-left (bit-and (aget complement-bytes 1) 0xFF) 8)
                           (bit-and (aget complement-bytes 0) 0xFF))]
    (= 0xFFFF (bit-xor checksum complement))))

(defn- decode-region
  "Decode region code byte to region name"
  [region-code]
  (case (bit-and region-code 0xFF)
    0x00 "Japan"
    0x01 "North America"
    0x02 "Europe"
    0x03 "Scandinavia"
    0x04 "France"
    0x05 "Netherlands"
    0x06 "Spain"
    0x07 "Germany"
    0x08 "Italy"
    0x09 "China"
    0x0A "Korea"
    0x0D "South America"
    0x0F "Australia"
    "Unknown"))

(defn- bytes->word
  "Convert 2-byte little-endian array to word"
  [bytes]
  (bit-or (bit-shift-left (bit-and (aget bytes 1) 0xFF) 8)
          (bit-and (aget bytes 0) 0xFF)))

(defn- parse-header
  "Parse SNES ROM header from byte sequences. Takes a function to read bytes at offset."
  [read-fn offset smc-offset]
  (let [;; Extract game title (21 bytes)
        game-title (bytes-to-string (read-fn offset 21))

        ;; Extract ROM makeup byte
        rom-makeup (first (read-fn (+ offset 0x15) 1))
        rom-speed (if (bit-test rom-makeup 4) :fast :slow)

        ;; Extract cartridge type
        cartridge-type-byte (first (read-fn (+ offset 0x16) 1))

        ;; Extract ROM size
        rom-size-byte (first (read-fn (+ offset 0x17) 1))
        rom-size-kb (bit-shift-left 1 (bit-and rom-size-byte 0xFF))

        ;; Extract RAM size
        ram-size-byte (first (read-fn (+ offset 0x18) 1))
        ram-size-kb (if (zero? ram-size-byte) 0 (bit-shift-left 1 (bit-and ram-size-byte 0xFF)))

        ;; Extract region
        region-code (first (read-fn (+ offset 0x19) 1))
        region (decode-region region-code)

        ;; Extract version
        version (bit-and (first (read-fn (+ offset 0x1B) 1)) 0xFF)

        ;; Extract checksums
        checksum (bytes->word (read-fn (+ offset 0x1C) 2))
        complement (bytes->word (read-fn (+ offset 0x1E) 2))]

    {:game-title game-title
     :rom-type (if (= offset (+ smc-offset 0x7FC0)) :lorom :hirom)
     :rom-speed rom-speed
     :rom-size-kb rom-size-kb
     :ram-size-kb ram-size-kb
     :cartridge-type cartridge-type-byte
     :region region
     :version version
     :checksum checksum
     :complement complement}))

(defn extract-metadata-from-raf
  "Extract metadata from ROM data via RandomAccessFile"
  [^RandomAccessFile raf]
  (let [file-size (.length raf)
        has-smc-header (= 512 (mod file-size 1024))
        smc-offset (if has-smc-header 512 0)
        lorom-offset (+ smc-offset 0x7FC0)
        hirom-offset (+ smc-offset 0xFFC0)
        offset (cond
                 (check-header-validity-raf raf lorom-offset) lorom-offset
                 (check-header-validity-raf raf hirom-offset) hirom-offset
                 :else lorom-offset)
        read-fn (fn [off len] (read-bytes raf off len))]
    (assoc (parse-header read-fn offset smc-offset)
      :has-smc-header has-smc-header)))

(defn extract-metadata-from-bytes
  "Extract metadata from ROM data byte array"
  [^bytes data]
  (let [file-size (alength data)
        has-smc-header (= 512 (mod file-size 1024))
        smc-offset (if has-smc-header 512 0)
        lorom-offset (+ smc-offset 0x7FC0)
        hirom-offset (+ smc-offset 0xFFC0)
        offset (cond
                 (check-header-validity-array data lorom-offset) lorom-offset
                 (check-header-validity-array data hirom-offset) hirom-offset
                 :else lorom-offset)
        read-fn (fn [off len] (read-bytes-from-array data off len))]
    (assoc (parse-header read-fn offset smc-offset)
      :has-smc-header has-smc-header)))

(defn find-smc-in-zip
  "Find first .smc file in zip archive"
  [^ZipFile zip-file]
  (let [entries (enumeration-seq (.entries zip-file))]
    (first (filter #(let [name (.getName ^ZipEntry %)]
                      (and (not (.isDirectory ^ZipEntry %))
                           (or (.endsWith name ".smc")
                               (.endsWith name ".SMC"))))
                   entries))))

(defn read-zip-entry-bytes
  "Read all bytes from a zip entry"
  [^ZipFile zip-file ^ZipEntry entry]
  (with-open [is (.getInputStream zip-file entry)
              baos (java.io.ByteArrayOutputStream.)]
    (io/copy is baos)
    (.toByteArray baos)))

(defn is-zip-file?
  "Check if file is a zip file based on extension"
  [filepath]
  (let [lower-path (.toLowerCase filepath)]
    (.endsWith lower-path ".zip")))

(defn extract-metadata
  "Extract metadata from SMC ROM file or ZIP containing SMC file"
  [^String filepath]
  (try
    (let [^String real-filepath (cond-> filepath
                                  (str/index-of filepath "#") (subs 0 (str/index-of filepath "#")))
          fallback-title (util/filename-without-extension real-filepath)
          metadata (if (is-zip-file? real-filepath)
                     ;; Handle ZIP file
                     (with-open [zip-file (ZipFile. real-filepath)]
                       (if-let [smc-entry (find-smc-in-zip zip-file)]
                         (let [rom-data (read-zip-entry-bytes zip-file smc-entry)
                               metadata (extract-metadata-from-bytes rom-data)]
                           (assoc metadata :source :zip :zip-entry-name (.getName smc-entry)))
                         (throw (Exception. "No .smc file found in ZIP archive"))))
                     ;; Handle direct SMC file
                     (with-open [raf (RandomAccessFile. real-filepath "r")]
                       (assoc (extract-metadata-from-raf raf) :source :direct)))]
      (assoc metadata :fallback-title fallback-title))
    (catch Exception e
      {:error (str "Error reading ROM: " (.getMessage e))})))

(defn extract-game-name
  "Simple version that just extracts the game name from SMC or ZIP"
  [filepath]
  (let [{:keys [game-title fallback-title] :as result} (extract-metadata filepath)]
    (if (:error result)
      (:error result)
      (or (util/trim-to-nil game-title)
          (util/trim-to-nil fallback-title)))))

;;;;;;;;;;;;;
;; Testing ;;
;;;;;;;;;;;;;
(comment
  (->> (io/resource "Super Mario World (U) [!].zip")
       (io/file)
       (.getAbsolutePath)
       (extract-metadata))

  (->> (io/resource "Super Mario World (U) [!].zip")
       (io/file)
       (.getAbsolutePath)
       (extract-game-name)))
