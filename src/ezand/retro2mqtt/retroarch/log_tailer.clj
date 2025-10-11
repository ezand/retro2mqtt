(ns ezand.retro2mqtt.retroarch.log-tailer
  (:require [cheshire.core :as json]
            [ezand.retro2mqtt.printer :as printer]
            [ezand.retro2mqtt.retroarch.mqtt :as retroarch-mqtt]
            [ezand.retro2mqtt.utils :as util]
            [superstring.core :as str])
  (:import (java.io File RandomAccessFile)))

;;;;;;;;;;;
;; Utils ;;
;;;;;;;;;;;
(defn- match-line
  "Check if line matches any pattern. Returns [pattern-key data] or nil.
  Pure function - testable without I/O."
  [patterns line]
  (some (fn [[k {:keys [regexp update-fn state-topic retain? on-match-fn]}]]
          (when-let [match (re-find regexp line)]
            (let [updated-data (if update-fn
                                 (update-fn match)
                                 (rest match))]
              [k updated-data state-topic retain? on-match-fn])))
        patterns))

(defn- filter-log-files
  "Filter files matching retroarch log pattern. Pure function."
  [files]
  (filter #(and (.isFile %)
                (re-matches #"retroarch__\d{4}_\d{2}_\d{2}__\d{2}_\d{2}_\d{2}\.log" (.getName %)))
          files))

(defn- most-recent-file
  "Find most recent file from collection by lastModified. Pure function."
  [files]
  (first (sort-by #(.lastModified %) > files)))

(defn- should-switch-file?
  "Determine if we should switch to a different file. Pure function."
  [current-file latest-file]
  (and latest-file (not= latest-file current-file)))

(defn- maybe-serialize-data
  [data]
  (cond-> data
    (coll? data) json/generate-string))

(defn- first-match
  [matches]
  (first (rest matches)))

;;;;;;;;;;;;;;;;;;;
;; I/O Functions ;;
;;;;;;;;;;;;;;;;;;;
(defn- most-recent-log-file
  "Find the most recent retroarch log file in directory."
  [^File log-dir]
  (when (.exists log-dir)
    (some->> (.listFiles log-dir)
             (filter-log-files)
             (most-recent-file))))

(defn- open-log-reader
  "Open RandomAccessFile positioned at end."
  [^File file]
  (doto (RandomAccessFile. file "r")
    (.seek (.length file))))

(defn- close-reader
  "Safely close reader if not nil."
  [raf]
  (when raf (.close raf)))

;;;;;;;;;;;;;;;;;;;
;; Tail Log File ;;
;;;;;;;;;;;;;;;;;;;
(def ^:private ^:const interesting-log-patterns
  {:core-libretro-file {:regexp #"\[INFO\] \[Core\]: Loading dynamic libretro core from: \"(.+)\""
                        :update-fn first-match
                        :on-match-fn (fn [publish-fn data]
                                       (publish-fn retroarch-mqtt/topic-retroarch-libretro-core-file data false)
                                       ; TODO get core details from info-file
                                       )}
   :content {:regexp #"\[INFO\] \[Content\]: Loading content file: \"(?:.*#)?([^#\"]+?)(?:\.[^.\"]+)?\""
             :update-fn first-match
             :on-match-fn
             (fn [publish-fn data]
               (publish-fn retroarch-mqtt/topic-retroarch-content data false)
               (publish-fn retroarch-mqtt/topic-retroarch-content-loaded? (util/bool->toggle-str true) false)
               (publish-fn retroarch-mqtt/topic-retroarch-content-running? (util/bool->toggle-str true) false)
               ; TODO publish content image
               ; TODO read details from content file?
               )}
   :content-unloaded?
   {:regexp #"\[INFO\] \[Core\]: Content ran for a total of"
    :update-fn some?
    :on-match-fn (fn [publish-fn _]
                   (publish-fn retroarch-mqtt/topic-retroarch-libretro-core-file nil false)
                   (publish-fn retroarch-mqtt/topic-retroarch-content-loaded? (util/bool->toggle-str false) false)
                   (publish-fn retroarch-mqtt/topic-retroarch-content nil false)
                   (publish-fn retroarch-mqtt/topic-retroarch-content-running? (util/bool->toggle-str false) false)
                   (publish-fn retroarch-mqtt/topic-retroarch-content-crc32 nil false)
                   (publish-fn retroarch-mqtt/topic-retroarch-core nil false)
                   ; TODO remove core and content details
                   )}
   :content-crc32 {:regexp #"\[INFO\] \[Content\]: CRC32: (0x[0-9a-fA-F]+)"
                   :update-fn #(format "%08x" (Long/parseLong (subs (first-match %) 2) 16))
                   :state-topic retroarch-mqtt/topic-retroarch-content-crc32}
   :content-video-size {:regexp #"\[INFO\] \[Video\]: Set video size to: (\d+x\d+)"
                        :update-fn first-match
                        :state-topic retroarch-mqtt/topic-retroarch-content-video-size
                        :retain? true}
   :system-cpu {:regexp #"\[INFO\] CPU Model Name: (.+)"
                :update-fn first-match
                :state-topic retroarch-mqtt/topic-retroarch-system-cpu
                :retain? true}
   :system-capabilities {:regexp #"\[INFO\] Capabilities: (.+)"
                         :update-fn #(str/split (first-match %) #" ")
                         :state-topic retroarch-mqtt/topic-retroarch-system-capabilities
                         :retain? true}
   :system-display-driver {:regexp #"\[INFO\] \[Display\]: Found display driver: \"(.+)\""
                           :update-fn first-match
                           :state-topic retroarch-mqtt/topic-retroarch-system-display-driver
                           :retain? true}
   :system-joypad-driver {:regexp #"\[INFO\] \[Joypad\]: Found joypad driver: \"(.+)\"."
                          :update-fn first-match
                          :state-topic retroarch-mqtt/topic-retroarch-system-joypad-driver
                          :retain? true}
   :system-audio-input-rate {:regexp #"\[INFO\] \[Audio\]: Set audio input rate to: (.+)."
                             :update-fn first-match
                             :state-topic retroarch-mqtt/topic-retroarch-system-audio-input-rate
                             :retain? true}
   :system-pixel-format {:regexp #"\[INFO\] \[Environ\]: SET_PIXEL_FORMAT: (.+)."
                         :update-fn first-match
                         :state-topic retroarch-mqtt/topic-retroarch-system-pixel-format
                         :retain? true}
   :libretro-api-version {:regexp #"\[INFO\] \[Core\]: Version of libretro API: (.+), Compiled against API: (.+)"
                          :update-fn first-match
                          :state-topic retroarch-mqtt/topic-retroarch-libretro-api-version
                          :retain? true}
   :retroarch-version {:regexp #"\[INFO\] Version: (.+)"
                       :update-fn first-match
                       :state-topic retroarch-mqtt/topic-retroarch-version
                       :retain? true}
   :retroarch-version-build-date {:regexp #"\[INFO\] Built: (.+)"
                                  :update-fn first-match
                                  :state-topic retroarch-mqtt/topic-retroarch-version-build-date
                                  :retain? true}
   :retroarch-git-hash {:regexp #"\[INFO\] Git: (.+)"
                        :update-fn first-match
                        :state-topic retroarch-mqtt/topic-retroarch-version-git-hash
                        :retain? true}
   :retroarch-cmd-interface-port {:regexp #"\[INFO\] \[NetCMD\]: bringing_up_command_interface_at_port (.+)."
                                  :update-fn #(util/with-suppressed-errors (Integer/parseInt (first-match %)))
                                  :state-topic retroarch-mqtt/topic-retroarch-cmd-interface-port
                                  :retain? true}})

(defn tail-log-file!
  "Tail most recent RetroArch log file in directory, switching to newer logs as they appear.

  Options:
    :patterns - Map of pattern configs (default: interesting-log-patterns)
    :publish-fn - Callback fn [state-topic data retain?] (default: println all)
    :poll-interval-ms - Milliseconds between polls (default: 100)
    :wait-interval-ms - Milliseconds to wait for log file (default: 1000)"
  ([keep-listening? ^File log-dir] (tail-log-file! keep-listening? log-dir {}))
  ([keep-listening? ^File log-dir
    {:keys [patterns publish-fn poll-interval-ms wait-interval-ms]
     :or {patterns interesting-log-patterns
          publish-fn (fn [state-topic data retain?]
                       (println (format "Publish to %s (retain?=%s): %s" state-topic data retain?)))
          poll-interval-ms 100
          wait-interval-ms 1000}}]
   (println (str printer/yellow "♻️ Tailing latest RetroArch log-file in " (.getAbsoluteFile log-dir) printer/reset))
   (loop [current-file nil
          raf nil
          position 0]
     (println (format "Checking... [%s]" (str @keep-listening?)))
     (if-not @keep-listening?
       (do
         (println (str printer/green "✅ Stopped tailing RetroArch log file" printer/reset))
         (close-reader raf))
       (let [latest-file (most-recent-log-file log-dir)]
         (cond
           ;; No log file yet, wait and retry
           (nil? latest-file)
           (do
             (Thread/sleep ^Long wait-interval-ms)
             (recur nil nil 0))

           ;; New log file detected, switch to it
           (should-switch-file? current-file latest-file)
           (do
             (close-reader raf)
             (let [new-raf (open-log-reader latest-file)]
               (recur latest-file new-raf (.length latest-file))))

           ;; Read from current log file
           :else
           (if-let [line (.readLine raf)]
             (do
               (when-let [[pattern-key data state-topic retain? on-match-fn] (match-line patterns line)]
                 (cond
                   state-topic (publish-fn state-topic (maybe-serialize-data data) retain?)
                   on-match-fn (on-match-fn publish-fn data)
                   :else (println (format "Unhandled pattern match key (%s): %s" pattern-key data))))
               (recur current-file raf (.getFilePointer raf)))
             (do
               (Thread/sleep ^Long poll-interval-ms)
               (recur current-file raf position)))))))))

;;;;;;;;;;;;;
;; Testing ;;
;;;;;;;;;;;;;
(comment
  (match-line interesting-log-patterns "[INFO] [Core]: Loading dynamic libretro core from: \"/Users/user/Library/Application Support/RetroArch/cores/bsnes2014_accuracy_libretro.dylib\"")
  (match-line interesting-log-patterns "[INFO] [Content]: CRC32: 0xa31bead4.")
  (match-line interesting-log-patterns "[INFO] [Video]: Set video size to: 897x672.")
  (match-line interesting-log-patterns "[INFO] CPU Model Name: Apple M2 Max")
  (match-line interesting-log-patterns "[INFO] Capabilities: NEON VFPV3 VFPV4")
  (match-line interesting-log-patterns "[INFO] Version: 1.21.0")
  (match-line interesting-log-patterns "[INFO] Git: 05f94af4")
  (match-line interesting-log-patterns "[INFO] Built: Apr 30 2025")
  (match-line interesting-log-patterns "[INFO] [Display]: Found display driver: \"vulkan\".")
  (match-line interesting-log-patterns "[INFO] [NetCMD]: bringing_up_command_interface_at_port 55355.")
  (match-line interesting-log-patterns "[INFO] [Audio]: Set audio input rate to: 48000.00 Hz.")
  (match-line interesting-log-patterns "[INFO] [Joypad]: Found joypad driver: \"mfi\".")
  (match-line interesting-log-patterns "[INFO] [Core]: Version of libretro API: 1, Compiled against API: 1")
  (match-line interesting-log-patterns "[INFO] [Environ]: SET_PIXEL_FORMAT: RGB565.")
  (match-line interesting-log-patterns "[INFO] [Core]: Content ran for a total of: 00 hours, 00 minutes, 05 seconds.")
  (match-line interesting-log-patterns "[INFO] [Content]: Updating firmware status for: \"/Users/user/Library/Application Support/RetroArch/cores/bsnes2014_accuracy_libretro.dylib\" on \"/Users/user/Documents/RetroArch/system\".\n"))
