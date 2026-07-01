(ns bioscoop.config)

(def ^:dynamic *debug-mode* #{})
(def ^:dynamic *warn-verbose* true)
(def ^:dynamic *dynamic-resolution* false)

(defn toggle-warning [] (alter-var-root #'*warn-verbose* not))
(defn reset-debug [] (alter-var-root #'*debug-mode* (fn [nodes] (empty nodes))))
(defn toggle-dynamic-resolution [] (alter-var-root #'*dynamic-resolution* not))

(defn toggle-debug-nodes [& {:as args}]
  (alter-var-root #'*debug-mode* (fn [nodes] (reduce-kv (fn [acc k v] (if (true? v) (conj acc k) (disj acc k))) nodes args))))


(comment (toggle-debug-nodes :program true
                             :graph-definition true
                             :padded-graph true
                             :let-binding true
                             :for-binding true
                             :binding false
                             :list true
                             :map false
                             :symbol true
                             :keyword false
                             :string false
                             :label false
                             :boolean false
                             :number false))
