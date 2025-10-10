(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'ezand/retro2mqtt)
(def version (format "0.0.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

;; delay to defer side effects (artifact downloads)
(def basis (delay (b/create-basis {:project "deps.edn"})))

;; ANSI color codes
(def green "\u001B[32m")
(def blue "\u001B[34m")
(def yellow "\u001B[33m")
(def reset "\u001B[0m")

(defn clean [_]
  (println (str yellow "🧹 Cleaning target directory..." reset))
  (b/delete {:path "target"})
  (println (str green "✅ Clean complete" reset)))

(defn uber [_]
  (println (str blue "🚀 Building uberjar for " lib " v" version reset))
  (clean nil)
  (println (str yellow "📦 Copying sources..." reset))
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (println (str yellow "⚙️ Compiling Clojure..." reset))
  (b/compile-clj {:basis @basis
                  :ns-compile '[ezand.retro2mqtt.core]
                  :class-dir class-dir})
  (println (str yellow "🔨 Creating uberjar..." reset))
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis @basis
           :main 'ezand.retro2mqtt.core})
  (println (str green "✅ Built " uber-file reset)))
