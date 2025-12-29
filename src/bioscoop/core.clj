(ns bioscoop.core
  (:require [bioscoop.cli :as cli])
  (:gen-class))

(defn -main [& args]
  (cli/bioscoop args))
