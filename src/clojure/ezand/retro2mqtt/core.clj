(ns ezand.retro2mqtt.core
  (:gen-class)
  (:require [config.core :refer [env]]
            [ezand.retro2mqtt.launchbox.core :as launchbox]
            [ezand.retro2mqtt.mqtt.core :as mqtt]
            [ezand.retro2mqtt.printer :as printer]
            [ezand.retro2mqtt.provider :as provider]
            [ezand.retro2mqtt.retroarch.core :as retroarch]))

(defn -main [& args]
  (let [{mqtt-config :mqtt
         {retroarch-enabled? :enabled?} :retroarch
         {launchbox-enabled? :enabled?} :launchbox
         {hyperspin-enabled? :enabled?} :hyperspin
         :as config} (:retro2mqtt env)]
    (printer/print-config)
    (let [mqtt-client (-> (mqtt/create-client mqtt-config)
                          (mqtt/connect! mqtt-config))]
      (when retroarch-enabled?
        (->> (retroarch/retroarch-provider mqtt-client config)
             (provider/start-listening!)))
      (when launchbox-enabled?
        (-> (launchbox/launchbox-provider mqtt-client config)
            (provider/start-listening!))))

    (println (str printer/green "✅ retro2mqtt started!" printer/reset))))
