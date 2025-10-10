(ns ezand.retro2mqtt.provider)

(defprotocol RetroProvider
  (start-listening! [this] "Start listening for provider events")
  (stop-listening! [this] "Stop listening for provider events"))
