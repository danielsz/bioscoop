(ns bioscoop.domain.specs.noise
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))


(def allowed-members #{"a" "p" "t" "u"})

;; Predicate: split by "+" and check if every part is in the set
(s/def ::flags 
  (s/and string? 
         #(let [parts (str/split % #"\+")]
            (and (seq parts) 
                 (every? allowed-members parts)))))


(s/def ::all_flags ::flags)
(s/def ::c0_flags ::flags)
(s/def ::c1_flags ::flags)
(s/def ::c2_flags ::flags)
(s/def ::c3_flags ::flags)

(s/def ::all_seed (s/int-in -1 Integer/MAX_VALUE))
(s/def ::c0_seed (s/int-in -1 Integer/MAX_VALUE))
(s/def ::c1_seed (s/int-in -1 Integer/MAX_VALUE))
(s/def ::c2_seed (s/int-in -1 Integer/MAX_VALUE))
(s/def ::c3_seed (s/int-in -1 Integer/MAX_VALUE))

(s/def ::all_strength (s/int-in 0 101))
(s/def ::c0_strength (s/int-in 0 101))
(s/def ::c1_strength (s/int-in 0 101))
(s/def ::c2_strength (s/int-in 0 101))
(s/def ::c3_strength (s/int-in 0 101))

(s/def ::noise (s/keys :opt-un [::all_seed ::all_strength ::all_flags
                                ::c0_seed ::c0_strength  ::c0_flags 
                                ::c1_seed ::c1_strength  ::c1_flags 
                                ::c2_seed ::c2_strength  ::c2_flags 
                                ::c3_seed ::c3_strength  ::c3_flags]))
