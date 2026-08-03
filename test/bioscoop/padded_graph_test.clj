(ns bioscoop.padded-graph-test
   (:require [clojure.test :refer [deftest testing is]]
            [bioscoop.dsl :as dsl :refer [compile-dsl last-errors]]
            [bioscoop.macro :refer [bioscoop defgraph]])
  (:import [bioscoop.domain.records FilterGraph]))


(defn- error-types []
  (map (comp :error-type ex-data) @last-errors))

(deftest padded-graph-empty-chain--literal-empty-chain
  (testing "[[\"in\"] (chain) [\"out\"]] — empty chain literal"
    (let [result (compile-dsl "(compose [[\"in\"] (chain) [\"out\"]])")]
      (is (instance? FilterGraph result))
      (is (some #{:padded-graph-empty-chain} (error-types))))))

(deftest padded-graph-empty-chain--typo-d-filter-name
  (testing "[[\"in\"] (chain (zoompn {...})) [\"out\"]] — typo empties the chain's only arg"
    (let [result (bioscoop (compose [["in"] (chain (zoompn {z "1"})) ["out"]]))]
      (is (instance? FilterGraph result))
      (is (some #{:unresolved-function} (error-types)))
      (is (some #{:padded-graph-empty-chain} (error-types))))))

(deftest padded-graph-empty-chain--empty-defgraph-reference
  (testing "labeling pads on a reference to an accidentally-empty defgraph"
    (defgraph placeholder-graph (chain))
    (let [result (bioscoop (compose [["in"] placeholder-graph ["out"]]))]
      (is (instance? FilterGraph result))
      (is (some #{:padded-graph-empty-chain} (error-types))))))

(deftest padded-graph-labels-still-attach-on-non-empty-chain
  (testing "regression: real chains still get labeled correctly"
    (let [result (compile-dsl "(compose [[\"in\"] (chain (scale {:width 1920 :height 1080})) [\"out\"]])")]
      (is (empty? @last-errors))
      (let [f (-> result :chains first :filters first)]
        (is (= ["in"]  (:input-labels f)))
        (is (= ["out"] (:output-labels f)))))))
