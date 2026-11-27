(ns bioscoop.dsl-helpers
  (:require [bioscoop.domain.records :refer [promote-to-filtergraph* promote-to-filterchain* get-input-labels get-output-labels]]
            [bioscoop.error-handling :refer [accumulate-error]]))

(def promote-to-filtergraph (promote-to-filtergraph* accumulate-error))
(def promote-to-filterchain (promote-to-filterchain* (partial accumulate-error []))) ; must return [] since the result feeds into mapcat

;; compose 

(defn- unlabeled-chain? [chain]
  (and (empty? (get-input-labels chain))
       (empty? (get-output-labels chain))))

(defn validate-compose [graph env]
  (let [chains (:chains graph)]
    (if (and (> (count chains) 1)
             (every? unlabeled-chain? chains))
      (accumulate-error env graph :compose-unlabeled-chain)
      graph)))


;; padded graph

(defn padded-graph-partition
  "Partition a padded-graph body into input labels, the body expression, and output labels.

   A padded graph has the form [[in-label*] expr [out-label*]].
   This function walks the AST nodes and collects:
   - :input  — label nodes before the first non-label expression
   - :expr   — the single body expression (filter, chain, or graph reference)
   - :output — label nodes after the body expression

   Uses a boolean flag to track whether the walker has crossed from the
   input-label region into the body-expression region. Once the first
   non-label node is encountered, the flag flips and all subsequent
   labels are classified as output labels."
  [body]
  (loop [xs body
         result {:input [] :expr nil :output []}
         flag false]
    (if (empty? xs)
      result
      (let [[label _ :as x] (first xs)]
        (cond
          (and (= label :label) (not flag)) (recur (next xs) (update result :input conj x) flag)
          (and (= label :label) flag) (recur (next xs) (update result :output conj x) flag)
          :else (recur (next xs) (assoc result :expr x) true))))))


