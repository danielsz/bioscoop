(ns bioscoop.env)

(defn make-env
  ([] {:errors (atom [])})
  ([parent] (assoc {:errors (atom [])} :parent parent)))

(defn env-get [env sym]
  (if (contains? env sym)
    (get env sym)
    (when-let [parent (:parent env)]
      (env-get parent sym))))

(defn env-put [env sym val]
  (assoc env sym val))
