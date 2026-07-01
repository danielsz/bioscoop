(ns bioscoop.env)

(defn make-env
  ([] {:errors (atom [])})
  ([parent] (assoc {:errors (:errors parent)} :parent parent)))

(defn env-get [env sym]
  (if (contains? env sym)
    (get env sym)
    (when-let [parent (:parent env)]
      (env-get parent sym))))

(defn env-put [env sym val]
  (tap> {:event :env-put :name sym :value val :env env})
  (assoc env sym val))
