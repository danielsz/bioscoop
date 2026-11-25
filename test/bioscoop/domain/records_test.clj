(ns bioscoop.domain.records-test
  (:require [clojure.test :refer [deftest testing is]]
            [bioscoop.domain.records :refer [make-filter
                                             make-filterchain
                                             make-filtergraph
                                             get-input-labels
                                             get-output-labels]]))

(defn- filter [name in out]
  (make-filter name nil in out))

(deftest filter-labels
  (let [f (filter "trim" ["0:v"] ["clip"])]
    (is (= ["0:v"] (get-input-labels f)))
    (is (= ["clip"] (get-output-labels f)))))
(deftest filterchain-boundary-labels
  (let [chain (make-filterchain [(filter "trim" ["0:v"] ["mid"])
                                 (filter "setpts" [] ["clip"])])]
    (is (= ["0:v"] (get-input-labels chain)))
    (is (= ["clip"] (get-output-labels chain))))
  (testing "empty chain"
    (let [chain (make-filterchain [])]
      (is (= [] (get-input-labels chain)))
      (is (= [] (get-output-labels chain))))))

(deftest filtergraph-boundary-labels
  (let [graph (make-filtergraph
               [(make-filterchain [(filter "trim" ["1:v"] ["clip1"])])
                (make-filterchain [(filter "atrim" ["1:a"] ["aclip1"])])
                (make-filterchain [(filter "trim" ["2:v"] ["clip2"])])])]
    (is (= ["1:v" "1:a" "2:v"] (get-input-labels graph)))
    (is (= ["clip1" "aclip1" "clip2"] (get-output-labels graph))))
  (testing "empty graph"
    (let [graph (make-filtergraph [])]
      (is (= [] (get-input-labels graph)))
      (is (= [] (get-output-labels graph)))))
  (testing "matches the former internal expression"
    (let [graph (make-filtergraph
                 [(make-filterchain [(filter "trim" ["1:v"] ["clip1"])])
                  (make-filterchain [(filter "atrim" ["1:a"] ["aclip1"])])])]
      (is (= (into [] (mapcat (comp :output-labels last :filters) (:chains graph)))
             (get-output-labels graph))))))
(deftest nil-labels
  (is (= [] (get-input-labels nil)))
  (is (= [] (get-output-labels nil))))
