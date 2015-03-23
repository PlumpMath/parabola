(ns parabola.script
  (:require [parabola.response]))

(defn reactors [& rs]
  (fn [msg]
    (some #(% msg) rs)))

(defmacro defreactors [name & reactors]
  `(def ~(with-meta name {:reactor true})
     (reactors ~@reactors)))

(defmacro message [re arg & body]
  `(fn [msg#]
     (when-let [m# (and (= (:type msg#) :message)
                        (re-matches ~re (:text msg#)))]
       (let [~arg (assoc msg# :matches m#)]
         (respond (do ~@body) msg#)))))

