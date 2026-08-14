(ns bioscoop.built-in-test
  (:require [bioscoop.built-in :as built-in]
            [bioscoop.env :refer [make-env]]
            [bioscoop.domain.records :refer [->Filter make-filtergraph]]
            [clojure.test :refer [testing deftest is]]))

(deftest positional-args-validation
  (let [env (make-env)] ; make-env creates {:errors (atom [])}
    
    (testing "Valid positional args are accepted and namespaced correctly"
      (let [result (built-in/scale [1920 1080] env)
            expected (->Filter "scale" 
                               #:bioscoop.domain.specs.scale{:width 1920, :height 1080} 
                               [] [])]
        (is (= expected result))
        (is (empty? @(:errors env)) "Should not produce any errors for valid arguments")))
    
    (testing "Invalid positional argument types fail validation"
      (let [env (make-env)
            result (built-in/scale [true false] env)]
        (is (= (make-filtergraph []) result) "Should return an empty filtergraph on invalid parameters")
        (is (seq @(:errors env)) "Should accumulate an error for invalid parameter types")))

    (testing "Invalid positional argument types fail validation"
      ;; Bug 2 (False Positive): Before the fix, s/valid? ignores the namespaced keys
      ;; produced by zipmap. Because :width is :opt-un, it returns true, accepting `true` and `false`.
      ;; After the fix, the keys are unqualified, s/valid? checks them, and correctly fails.
      (let [env (make-env)
            result (built-in/scale [true false] env)]
        (is (= (make-filtergraph []) result) "Should return an empty filtergraph on invalid parameters")
        (is (seq @(:errors env)) "Should accumulate an error for invalid parameter types")))
    
    
    (testing "Required keys are properly validated when provided positionally"
      (let [env (make-env)
            result (built-in/setpts [123] env)]
        (is (= (make-filtergraph []) result) "Should return empty graph when required arg type is wrong")
        (is (seq @(:errors env)) "Should accumulate an error for invalid required parameter"))))


  (testing "Positional arguments populate :req-un keys correctly"
    ;; aevalsrc has ::exprs in :req-un and several :opt-un.
    (let [env (make-env)
          result (built-in/aevalsrc ["expr1"] env)]
      (is (instance? bioscoop.domain.records.Filter result) "Should return a valid Filter record")
      (is (= #:bioscoop.domain.specs.aevalsrc{:exprs "expr1"} (:args result)))
      (is (empty? @(:errors env)) "Should not produce any errors")))


  (testing "Positional arguments populate :req-un keys correctly"
    ;; aevalsrc has ::exprs in :req-un and several :opt-un.
    (let [env (make-env)
          result (built-in/aevalsrc [{:nonsense "eee"}] env)]
      (is (= (make-filtergraph []) result) "Should return an empty filtergraph on invalid parameters")
      (is (seq @(:errors env)) "Should accumulate an error for invalid parameter types")
      (is (= (:error-type (ex-data (first @(:errors env)))) :invalid-parameter)))))

(deftest photosensitivity-args-validation
  (let [env (make-env)]
    (testing "Valid keyword args are accepted and namespaced correctly"
      (let [result (built-in/photosensitivity [{:frames 60 :threshold 1.5 :skip 2 :bypass true}] env)
            expected (->Filter "photosensitivity"
                               #:bioscoop.domain.specs.photosensitivity{:frames 60, :threshold 1.5, :skip 2, :bypass true}
                               [] [])]
        (is (= expected result))
        (is (empty? @(:errors env)) "Should not produce any errors for valid arguments")))

    (testing "Invalid arguments fail validation"
      (let [env (make-env)
            result (built-in/photosensitivity [{:threshold 1}] env)]
        (is (= (make-filtergraph []) result) "Should return an empty filtergraph on invalid parameters")
        (is (seq @(:errors env)) "Should accumulate an error for invalid parameter types")
        (is (= (:error-type (ex-data (first @(:errors env)))) :invalid-parameter))))))


