(ns ezand.retro2mqtt.core
  (:gen-class)
  (:require [ezand.retro2mqtt.printer :as printer]))

(defn -main [& args]
  (println (str printer/green "✅ retro2mqtt started!")))
