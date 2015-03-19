(ns parabola.reactor
  (:require [clojure.core.async :as a]
            [com.stuartsierra.component :as comp]
            [taoensso.timbre :as timbre]))

(defn wrap [proc]
  (fn [message]
    (when-not (= (:type message) ::stop)
      (do (proc message)
          true))))

(defrecord Reactor [event robot ch proc]
  comp/Lifecycle
  (start [this]
    (timbre/info (str "reactor started for event " event))
    (a/sub (-> robot :channels :reactors-pub) event ch)
    (a/go-loop []
      (let [msg (a/<! ch)]
        (timbre/debug "message received: " msg)
        (when ((wrap proc) msg)
          (recur))))
    this)
  (stop [this]
    (timbre/info (str "reactor stopped for event " event))
    (a/>!! ch {:type ::stop})
    this))
