(ns bioscoop.domain.specs.shared.flags
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(defn flags
  "Spec for a '+'-joined flags string whose tokens are all in `allowed`."
  [allowed]
  (s/and string?
         (fn [s]
           (let [parts (str/split s #"\+")]
             (and (seq parts)
                  (every? allowed parts))))))
