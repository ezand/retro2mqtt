(ns ezand.retro2mqtt.core
  (:gen-class)
  (:require [config.core :refer [env]]
            [ezand.retro2mqtt.printer :as printer]))

(defn -main [& args]
  (let [{retroarch :retroarch
         launchbox :launchbox
         hyperspin :hyperspin
         {:keys [home-assistant]} :integrations
         :as config} (:retro2mqtt env)]
    (println "RetroArch:" retroarch)
    (println "LaunchBox:" launchbox)
    (println "Hyperspin:" hyperspin)
    (println "HomeAssistant:" home-assistant)
    (println (str printer/green "✅ retro2mqtt started!" printer/reset))))
