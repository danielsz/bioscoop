(ns bioscoop.resolve
  "Resolution interface that dispatches between runtime reflection (JVM Clojure)
   and static lookup (GraalVM native image) based on config/*dynamic-resolution*.
   
   When *dynamic-resolution* is true: Use runtime reflection via ns-resolve/resolve
   When *dynamic-resolution* is false (default): Use static lookup tables"
  (:require [bioscoop.config :as config :refer [*dynamic-resolution*]]
            [bioscoop.built-in]
            [bioscoop.domain.records :refer [compose-filtergraphs make-filtergraph]]
            [bioscoop.error-handling :refer [accumulate-error]]
            [bioscoop.env :refer [env-get]]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.set :refer [difference intersection]])
  (:import [bioscoop.domain.records FilterGraph]))

(def implemented-filters (reduce-kv (fn [m k v] (assoc m (str k) v)) {} (ns-publics 'bioscoop.built-in)))
(def clojure-core-functions (reduce-kv (fn [m k v] (assoc m (str k) v)) {} (ns-publics 'clojure.core)))
(def reserved-words (merge clojure-core-functions implemented-filters))
(def ffmpeg-filters (into #{} (str/split-lines (slurp (io/resource "filters.txt")))))
(def collisions (intersection ffmpeg-filters (into #{} (keys clojure-core-functions))))
(def aliases (into #{} (map (fn [n] (str n "_")) collisions)))
(def unimplemented-filters (difference ffmpeg-filters (into #{} (keys implemented-filters))))
(defn reserved-word? [name] (contains? reserved-words name))
(defn reserved-word-type [name]
  (cond
    (contains? implemented-filters name) :built-in
    (contains? clojure-core-functions name) :clojure-core  ))

(defn- error [op env error-type]
  (accumulate-error env op error-type)
  (fn [_ _] (make-filtergraph [])))

(defn wrap-apply [f]
  (fn [arg _] (apply f arg)))

(defn resolve-fn [op env else]
  (cond
    (keyword? op) (wrap-apply op)
    (contains? unimplemented-filters op) (error op env :not-implemented)
    (contains? implemented-filters op) (get implemented-filters op)
    (contains? clojure-core-functions op) (wrap-apply (get clojure-core-functions op))
    (contains? aliases op) (wrap-apply (get clojure-core-functions (subs op 0 (dec (count op)))))
    :else (else)))

(defmulti resolve-function (fn [op env] *dynamic-resolution*))

(defmethod resolve-function true [op env]
  (let [wrap-user-defined (fn [f] (fn [arg _]
                                   (let [result (apply f arg)]
                                     (cond
                                       (instance? FilterGraph result) result
                                       (seqable? result)              (apply compose-filtergraphs result)
                                       :else                          (make-filtergraph [])))))]
    (resolve-fn op env #(if-let [f (ns-resolve (:bioscoop/compile-ns env) (symbol op))]
                          (wrap-user-defined f)
                          (error op env :unresolved-function)))))

(defmethod resolve-function false [op env]
  (resolve-fn op env #(error op env :unresolved-function)))

(defn resolve-var
  "Resolve `name` to its Var. Returns the Var
   when found and bound, else nil."
  [name env]
  (when-let [v (if (namespace name)
                 (find-var name)
                 (ns-resolve (:bioscoop/compile-ns env) name))]
    (when (bound? v) v)))

(defmulti resolve-symbol (fn [sym env] *dynamic-resolution*))

(defmethod resolve-symbol false [sym env]
  (if-let [env-val (env-get env sym)]
    env-val
    sym))

(defmethod resolve-symbol true [sym env]
  (let [env-val (env-get env sym)
        v       (resolve-var (symbol sym) env)]
    (cond
      (and env-val v) (do (accumulate-error env sym :ambiguous-symbol)
                          env-val) ;; shadowing occurs, this is a warning not an error
      v (let [val (var-get v)]
          (if (fn? val) sym val))   ;; function → keep as string for dynamic dispatch
                                    ;; anything else → hand back the real value
      env-val env-val
      :else sym)))
