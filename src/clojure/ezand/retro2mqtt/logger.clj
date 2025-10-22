(ns ezand.retro2mqtt.logger
  (:require [cheshire.core :as json])
  (:import (org.slf4j Logger LoggerFactory)
           (org.slf4j.spi LoggingEventBuilder)))

;;;;;;;;;;;
;; Utils ;;
;;;;;;;;;;;
(defn- at-log-level
  ^LoggingEventBuilder [^Logger logger level-kwd]
  (case level-kwd
    :info (when (.isInfoEnabled logger) (.atInfo logger))
    :debug (when (.isDebugEnabled logger) (.atDebug logger))
    :warn (when (.isWarnEnabled logger) (.atWarn logger))
    :error (when (.isErrorEnabled logger) (.atError logger))
    :trace (when (.isTraceEnabled logger) (.atTrace logger))))

(defn- log!
  [^Logger logger log-level ^String msg markers-map]
  (when-let [builder (at-log-level logger log-level)]
    (let [markers (first markers-map)]
      (.log (reduce (fn [b [k v]]
                      (.addKeyValue b (name k) (cond-> v
                                                 (coll? v) json/generate-string)))
                    (-> builder (.setMessage msg))
                    markers)))))

;;;;;;;;;;;;;;;;;
;; Logger init ;;
;;;;;;;;;;;;;;;;;
(defn create-logger!
  [namespace]
  (LoggerFactory/getLogger (name (ns-name namespace))))

;;;;;;;;;;;;;
;; Logging ;;
;;;;;;;;;;;;;
(defn info
  [^Logger logger ^String msg & markers-map]
  (log! logger :info msg markers-map))

(defn debug
  [^Logger logger ^String msg & markers-map]
  (log! logger :debug msg markers-map))

(defn error
  [^Logger logger ^String msg & markers-map]
  (log! logger :error msg markers-map))

(defn warn
  [^Logger logger ^String msg & markers-map]
  (log! logger :warn msg markers-map))

(defn trace
  [^Logger logger ^String msg & markers-map]
  (log! logger :trace msg markers-map))
