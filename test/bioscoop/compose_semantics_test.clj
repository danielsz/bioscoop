(ns bioscoop.compose-semantics-test
  (:require [clojure.test :refer [deftest testing is]]
            [bioscoop.macro :refer [bioscoop]]
            [bioscoop.error-handling :refer [last-errors]]))

(defn- compose-unlabeled-chain-error? []
  (some #(= :compose-unlabeled-chain (:error-type (ex-data %))) @last-errors))

(deftest compose-without-labels-is-rejected
  (testing "a multi-chain compose with no boundary labels is rejected"
    (let [result (bioscoop (compose (scale 1920 1080) (overlay)))]
      (is (empty? (:chains result)))
      (is (compose-unlabeled-chain-error?)))))

(deftest chain-with-the-same-filters-is-accepted
  (testing "the same filters composed sequentially are accepted"
    (let [result (bioscoop (chain (scale 1920 1080) (overlay)))]
      (is (= 1 (count (:chains result))))
      (is (nil? (seq @last-errors))))))

(deftest labeled-compose-is-accepted
  (testing "a compose with boundary labels is accepted"
    (let [result (bioscoop
                   (compose [["0:v"] (scale 1920 1080) ["s"]]
                            [["s"] (overlay) ["out"]]))]
      (is (= 2 (count (:chains result))))
      (is (nil? (seq @last-errors))))))
