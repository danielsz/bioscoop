(ns bioscoop.domain.spec
  (:require [clojure.spec.alpha :as s]
            [lang-utils.core :refer [seek]]))

;; Data structure specifications
(s/def ::filter-name (s/and string? #(re-matches #"[a-zA-Z0-9_]+(@[a-zA-Z0-9_]+)?" %)))
(s/def ::filter (s/keys :req-un [::filter-name]
                        :opt-un [::args]))
(s/def ::filterchain (s/coll-of ::filter))
(s/def ::filtergraph (s/coll-of ::filterchain))


(defn spec-aware-namespace-keyword [spec unqualified-kw]
  (let [spec-map (apply hash-map (rest (s/form spec)))
        opt-un-specs (get spec-map :opt-un [])]
    (seek (fn [kw] (= (name kw) (name unqualified-kw))) opt-un-specs)))
;; to process a map: (into {} (map (fn [[k v]] [(spec-aware-namespace-keyword ::scale k) v]) {:width 1920 :height 1080})) or like below
 
(defn spec-aware-namespace-map
  "Convert unnamespaced map to properly namespaced map based on spec registry.
   Looks up where each spec is actually defined and uses that namespace."
  [spec-keyword unnamespaced-map]
  (when-not (s/get-spec spec-keyword)
    (throw (ex-info "Spec not found in registry" {:spec spec-keyword})))

  (let [spec-form (s/form spec-keyword)]
    (if (and (sequential? spec-form) 
             (= 'clojure.spec.alpha/keys (first spec-form)))
      (let [spec-map (apply hash-map (rest spec-form))
            opt-un-specs (get spec-map :opt-un [])
            req-un-specs (get spec-map :req-un [])
            all-un-specs (concat opt-un-specs req-un-specs)
            ;; Create mapping from unqualified key to its actual qualified spec
            key-mapping (into {} 
                           (map (fn [qualified-spec-kw]
                                  (let [unqual-key (keyword (name qualified-spec-kw))]
                                    [unqual-key qualified-spec-kw]))
                                all-un-specs))]
        ;; Transform the map using the spec-derived key mapping
        (into {} 
              (map (fn [[k v]]
                     (if-let [qualified-kw (get key-mapping k)]
                       [qualified-kw v]
                       ;; Keep unmapped keys as-is (or could warn/error)
                       [k v]))
                   unnamespaced-map)))
      ;; If not a keys spec, return original map
      unnamespaced-map)))





