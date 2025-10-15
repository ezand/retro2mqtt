(ns ezand.retro2mqtt.printer
  (:require [config.core :refer [env]]
            [cheshire.core :as json]))

;; ANSI color codes
(def ^:const green "\u001B[32m")
(def ^:const blue "\u001B[94m")
(def ^:const yellow "\u001B[33m")
(def ^:const reset "\u001B[0m")

(defn- hide-credentials
  [config]
  (update-in config [:mqtt :password] (constantly "******")))

(defn print-config
  []
  (println (str blue "⚙️ Current configuration:" reset))
  (-> (:retro2mqtt env)
      (hide-credentials)
      (json/generate-string {:pretty true})
      (println)))
