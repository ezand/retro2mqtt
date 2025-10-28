(ns user
  (:require [ezand.retro2mqtt.core :as core]))

(defn current-namespace [] (.getName *ns*))

(defn start-dev! [] (core/-main))
