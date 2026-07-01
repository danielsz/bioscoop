(ns bioscoop.resolve
  "Resolution interface that dispatches between runtime reflection (JVM Clojure)
   and static lookup (GraalVM native image) based on config/*dynamic-resolution*.
   
   When *dynamic-resolution* is true: Use runtime reflection via ns-resolve/resolve
   When *dynamic-resolution* is false (default): Use static lookup tables"
  (:require [bioscoop.config :as config :refer [*dynamic-resolution*]]
            [bioscoop.built-in]
            [bioscoop.domain.records :refer [compose-filtergraphs make-filtergraph]]
            [bioscoop.error-handling :refer [accumulate-error]]
            [clojure.tools.logging :as log])
  (:import [bioscoop.domain.records FilterGraph]))

(def built-in-functions (reduce-kv (fn [m k v] (assoc m (str k) v)) {} (ns-publics 'bioscoop.built-in)))
(def clojure-core-functions (reduce-kv (fn [m k v] (assoc m (str k) v)) {} (ns-publics 'clojure.core)))
(def reserved-words (merge clojure-core-functions built-in-functions ))
(defn reserved-word? [name] (contains? reserved-words name))
(defn reserved-word-type [name]
  (cond
    (contains? built-in-functions name) :built-in
    (contains? clojure-core-functions name) :clojure-core  ))

(defn- error [op env error-type]
  (accumulate-error env op error-type)
  (fn [_ _] (make-filtergraph [])))

(defmulti resolve-function (fn [op env] *dynamic-resolution*))

(defmethod resolve-function true [op env]
  (let [wrap-apply (fn [f]
                     (fn [arg _] (apply f arg)))
        wrap-user-defined (fn [f] (fn [arg _]
                                   (let [result (apply f arg)]
                                     (cond
                                       (instance? FilterGraph result) result
                                       (seqable? result)              (apply compose-filtergraphs result)
                                       :else                          (make-filtergraph [])))))]
    (cond
      (keyword? op) (wrap-apply op)
      :else
      (let [built-in (ns-resolve 'bioscoop.built-in (symbol op))
            ns-name  (str (:ns (meta built-in)))]
        (case ns-name
          "bioscoop.built-in" built-in
          "clojure.core" (wrap-apply built-in)
          (if-let [f (ns-resolve *ns* (symbol op))]
            (wrap-user-defined f)
            (error op env :unresolved-function)))))))

(defmethod resolve-function false [op env]
  (cond
    (contains? built-in-functions op) (get built-in-functions op)
    (contains? clojure-core-functions op) (fn [arg _]
                                            (let [f (get clojure-core-functions op)]
                                              (apply f arg)))
    :else (error op env :unresolved-function)))






