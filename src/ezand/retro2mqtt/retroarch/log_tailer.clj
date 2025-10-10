(ns ezand.retro2mqtt.retroarch.log-tailer
  (:require [cheshire.core :as json])
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
; TODO
(def ^:private ^:const interesting-log-patterns {})

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
   (loop [current-file nil
          raf nil
          position 0]
     (if-not @keep-listening?
       (close-reader raf)
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
                   on-match-fn (on-match-fn pattern-key data)
                   :else (println (format "Unhandled pattern match key (%s): %s" pattern-key data))))
               (recur current-file raf (.getFilePointer raf)))
             (do
               (Thread/sleep ^Long poll-interval-ms)
               (recur current-file raf position)))))))))
