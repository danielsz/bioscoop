(ns bioscoop.config)

(def ^:dynamic *debug-mode* false)
(def ^:dynamic *warn-verbose* true)

(defn toggle-warning [] (alter-var-root #'*warn-verbose* not))
(defn toggle-debug [] (alter-var-root #'*debug-mode* not))
