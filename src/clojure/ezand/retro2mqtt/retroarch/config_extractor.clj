(ns ezand.retro2mqtt.retroarch.config-extractor
  (:require [clojure.java.io :as io]
            [ezand.retro2mqtt.utils :as util]
            [superstring.core :as str])
  (:import (java.io File)))

(def ^:private ^:const interesting-keys
  #{:netplay-nickname :cheevos-enable :cheevos-username :cloud-sync-enable})

;;;;;;;;;;;
;; Utils ;;
;;;;;;;;;;;
(defn- parse-value
  [v]
  (let [trimmed (util/trim-to-nil v)]
    (cond
      (nil? trimmed) nil
      (= "true" trimmed) true
      (= "false" trimmed) false
      (re-matches #"^-?\d+$" trimmed) (parse-long trimmed)
      (re-matches #"^-?\d+\.\d+$" trimmed) (parse-double trimmed)
      :else trimmed)))

(defn- parse-line
  [line]
  (when-let [[_ k v] (re-matches #"^\s*([^=]+?)\s*=\s*\"?([^\"]*)\"?\s*$" line)]
    (let [kwd (keyword (str/lisp-case (str/trim k)))]
      (when (interesting-keys kwd)
        [kwd (parse-value v)]))))

;;;;;;;;;;;;;;;;
;; Extraction ;;
;;;;;;;;;;;;;;;;
(defn config-details
  [^File config-dir]
  (let [config-file (io/file config-dir "retroarch.cfg")]
    (when (.exists config-file)
      (with-open [rdr (io/reader config-file)]
        (->> (line-seq rdr)
             (keep parse-line)
             (into {}))))))
