(ns ezand.retro2mqtt.core
  (:gen-class))

;; ANSI color codes
(def green "\u001B[32m")

(defn -main [& args]
  (println (str green "✅ retro2mqtt started!")))
