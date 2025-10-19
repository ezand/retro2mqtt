(ns ezand.retro2mqtt.launchbox.core
  (:require [ezand.retro2mqtt.launchbox.mqtt :as launchbox-mqtt]
            [ezand.retro2mqtt.printer :as printer]
            [ezand.retro2mqtt.provider :as provider]))

;;;;;;;;;;;
;; State ;;
;;;;;;;;;;;
(defonce listening? (atom false))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Provider Implementation ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- -start-listening!
  [mqtt-client launchbox-config]
  (println (str printer/yellow "♻️ Listening for LaunchBox events" printer/reset))
  (reset! listening? true)
  ;; TODO Subscribe to topics from launchbox2mqtt plugin
  )

(defn- -stop-listening!
  []
  (reset! listening? false)
  (println (str printer/green "✅ Stopped listening for LaunchBox events" printer/reset)))

(defrecord LaunchBoxProvider [mqtt-client config]
  provider/RetroProvider
  (start-listening! [this] (-start-listening! mqtt-client config))
  (stop-listening! [this] (-stop-listening!)))

(defn launchbox-provider
  [mqtt-client {{{:keys [discovery?]} :home-assistant} :integrations :as config}]
  (when discovery?
    (launchbox-mqtt/publish-homeassistant-discovery! mqtt-client))
  (->LaunchBoxProvider mqtt-client (:launchbox config)))
