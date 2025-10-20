(ns ezand.retro2mqtt.retroarch.udp-connector
  (:require [ezand.retro2mqtt.udp :as udp]))

(defn retroarch-version
  [{:keys [host port timeout] :as retroarch-config}]
  (udp/send-udp-sync host port timeout "VERSION"))
