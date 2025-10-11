(ns ezand.retro2mqtt.retroarch.info-file
  (:require [medley.core :as medley]
            [superstring.core :as str])
  (:import (java.io File)))

(defn parse-info
  "Parse a .info file into a clojure map.

  Ex. info file content:
  # Hardware Information
  manufacturer = \"Nintendo\"
  systemname = \"Super Nintendo Entertainment System\"
  systemid = \"super_nes\"

  This will be parsed to:
  {:manufacturer \"Nintendo\"
   :systemname \"Super Nintendo Entertainment System\"
   :systemid \"super_nes\"}"
  [^File info-file]
  (when (and info-file
             (.exists info-file))
    (->> (slurp info-file)
         (str/split-lines)
         (remove #(or (str/blank? %)
                      (str/starts-with? (str/trim %) "#")))
         (map #(str/split % #"\s*=\s*" 2))
         (filter #(= 2 (count %)))
         (map (fn [[k v]]
                (let [trimmed-v (str/trim v)
                      unquoted (str/replace trimmed-v #"^\"(.*)\"$" "$1")]
                  [(keyword (str/trim k))
                   (if (str/blank? unquoted) nil unquoted)])))
         (into {})
         (medley/map-keys #(keyword (str/lisp-case (name %)))))))

;;;;;;;;;;;;;
;; Testing ;;
;;;;;;;;;;;;;
(comment
  (require '[clojure.java.io :as io])

  (parse-info (io/file (io/resource "bsnes2014_accuracy_libretro.info"))))
