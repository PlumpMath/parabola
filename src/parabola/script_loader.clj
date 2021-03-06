(ns parabola.script-loader
  (:require [clojure.core.async :as async]
            [bultitude.core :refer [namespaces-on-classpath]]
            [com.stuartsierra.component :as comp]
            [taoensso.timbre :as timbre]
            [parabola.reactor :as reactor]))

(defrecord Script [ns reactors]
  comp/Lifecycle
  (start [this]
    (doseq [reactor reactors]
      (comp/start reactor))
    this)
  (stop [this]
    (doseq [reactor reactors]
      (comp/stop reactor))
    this))

(defn script-reactors [robot ns-name]
  (->> (for [v (vals (ns-publics ns-name))
             :when (:reactor (meta v))]
         (reactor/->Reactor (:name (meta v)) robot (async/chan 2) @v))
       vec))

(defn load-scripts [robot]
  (let [prefix (-> robot :config :script-prefix)]
    (vec (for [ns-name (namespaces-on-classpath :prefix prefix)]
           (do (require ns-name)
               (->Script ns-name (script-reactors robot ns-name)))))))

(defrecord ScriptLoader [robot scripts]
  comp/Lifecycle
  (start [this]
    (if-not scripts
      (do (timbre/info "script loader started")
          (let [scripts (load-scripts robot)]
            (doseq [script scripts]
              (comp/start script))
            (assoc this :scripts scripts)))
      this))
  (stop [this]
    (if scripts
      (do (doseq [script scripts]
            (comp/stop script))
          (timbre/info "script loader stopped")
          (assoc this :scripts nil))
      this)))

(defn new-script-loader []
  (map->ScriptLoader {}))
