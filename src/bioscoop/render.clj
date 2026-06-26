(ns bioscoop.render
  (:require [clojure.string :as str]
            [bioscoop.domain.records :refer [get-input-labels get-output-labels]]
            [clojure.tools.logging :as log])
  (:import [bioscoop.domain.records Filter FilterChain FilterGraph]))

(declare drop-namespace-from-map)

(defn- filter-to-dsl [f]
  (let [{:keys [name args]} f
        in    (:input-labels f)
        out   (:output-labels f)
        call  (if args
                (str (apply list [(symbol name)
                                  (drop-namespace-from-map args)]))
                (str (list (symbol name))))]
    (if (or (seq in) (seq out))
      (str "["
           (when (seq in)
             (str (str/join " " (map #(str "[\"" % "\"]") in)) " "))
           call
           (when (seq out)
             (str " " (str/join " " (map #(str "[\"" % "\"]") out))))
           "]")
      call)))


(defprotocol Renderable
  (to-ffmpeg [this] "Convert to ffmpeg filter string")
  (to-dsl [this] "Convert to DSL string"))

(extend-protocol Renderable
  Filter
  (to-ffmpeg [filter]
    (let [{:keys [name args]} filter
          input-labels (get-input-labels filter)
          output-labels (get-output-labels filter)
          input-str (when (seq input-labels)
                      (str/join "" (map #(str "[" % "]") input-labels)))
          output-str (when (seq output-labels)
                       (str/join "" (map #(str "[" % "]") output-labels)))
          args-str (when args
                     (str "=" (str/join ":" (map (fn [[k v]] (str (clojure.core/name k) "=" v)) args))))]
      (str input-str name args-str output-str)))

  (to-dsl [filter]
    (filter-to-dsl filter))
  
  FilterChain
  (to-ffmpeg [{:keys [filters]}]
    (str/join "," (map to-ffmpeg filters)))
  
  (to-dsl [{:keys [filters]}]
  (if (= 1 (count filters))
    (filter-to-dsl (first filters))
    (format "(chain %s)"
            (str/join " " (map filter-to-dsl filters)))))
  
  FilterGraph
  (to-ffmpeg [{:keys [chains]}]
    (str/join ";" (mapv to-ffmpeg chains)))

  (to-dsl [{:keys [chains]}]
  (if (= 1 (count chains))
    (to-dsl (first chains))
    (format "(compose %s)" (str/join " " (map to-dsl chains))))))


(defn drop-namespace-from-map
  "Transforms a map by removing the namespace from qualified keyword keys."
  [m]
  (reduce-kv (fn [acc k v]
               (assoc acc (if (qualified-keyword? k) (keyword (name k)) k) v))
             {}
             m))
