(ns ezand.retro2mqtt.utils)

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
