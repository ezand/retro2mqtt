(ns ezand.retro2mqtt.utils
  (:require [clojure.java.io :as io]
            [ezand.retro2mqtt.logger :as log]
            [superstring.core :as str])
  (:import (org.slf4j Logger)))

(defmacro with-suppressed-errors
  "Suppress any errors, but logs them so we know they occurred."
  {:style/indent 0}
  [^Logger logger ^String msg & body]
  `(try
     ~@body
     (catch Throwable e#
       (log/error ~logger ~msg {:exception e#})
       nil)))

(defn bool->toggle-str
  [bool-value]
  (when (some? bool-value)
    (if bool-value "on" "off")))

(defn filename-without-extension
  [path]
  (let [file (.getName (io/file path))]
    (str/replace file #"\.[^.]+$" "")))

(defn trim-to-nil
  "Trim string and return nil if empty or already nil"
  [s]
  (when s
    (let [trimmed (str/trim s)]
      (when-not (str/blank? trimmed)
        trimmed))))

(defn homeassistant-config-topic
  [unique-id]
  (format "homeassistant/sensor/%s/config" unique-id))

(def current-os
  (if (str/includes? (str/lower-case (System/getProperty "os.name")) "windows")
    :windows
    :unix-style))
