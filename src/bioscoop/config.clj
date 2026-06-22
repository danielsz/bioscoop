(ns bioscoop.config)

(def ^:dynamic *debug-mode* false)
(def ^:dynamic *warn-verbose* true)
(def ^:dynamic *dynamic-resolution* false)

(defn toggle-warning [] (alter-var-root #'*warn-verbose* not))
(defn toggle-debug [] (alter-var-root #'*debug-mode* not))
(defn toggle-dynamic-resolution [] (alter-var-root #'*dynamic-resolution* not))

