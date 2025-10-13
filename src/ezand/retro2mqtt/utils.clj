(ns ezand.retro2mqtt.utils
  (:require [clojure.java.io :as io]
            [superstring.core :as str]))

(defmacro with-suppressed-errors
  "Suppress any errors, but prints them so we know they occurred."
  {:style/indent 0}
  [& body]
  `(try
     ~@body
     (catch Throwable e#
       (.printStackTrace e#)
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

