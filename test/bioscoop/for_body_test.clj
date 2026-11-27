(ns bioscoop.for-body-test
  (:require [clojure.test :refer [deftest testing is]]
            [instaparse.core :as insta]
            [bioscoop.parse :refer [dsl-parser]]
            [bioscoop.dsl :refer [compile-dsl]]
            [bioscoop.error-handling :refer [last-errors]]
            [bioscoop.macro :refer [bioscoop form->ast]]
            [bioscoop.render :refer [to-ffmpeg]]))

(def ^:private multi-body-dsl
  "(for [i (range 2)] (scale i 10) (hflip))")

(defn- for-body-arity-warning? []
  (some #(= :for-body-arity (:error-type (ex-data %))) @last-errors))

(deftest grammar-accepts-multi-body-for
  (testing "external grammar accepts multiple body expressions like the macro"
    (is (not (insta/failure? (dsl-parser multi-body-dsl))))))

(deftest external-evaluator-warns-and-uses-first-body
  (testing "compile-dsl warns and evaluates the first body"
    (let [result (compile-dsl multi-body-dsl)]
      (is (for-body-arity-warning?))
      (is (= "scale=width=0:height=10;scale=width=1:height=10"
             (to-ffmpeg result))))))

(deftest macro-evaluator-warns-and-uses-first-body
  (testing "bioscoop warns and evaluates the first body"
    (let [result (bioscoop (for [i (range 2)] (scale i 10) (hflip)))]
      (is (for-body-arity-warning?))
      (is (= "scale=width=0:height=10;scale=width=1:height=10"
             (to-ffmpeg result))))))

(deftest single-body-for-unaffected
  (testing "single-body for produces no warning"
    (let [result (compile-dsl "(for [i (range 2)] (hflip))")]
      (is (nil? (seq @last-errors)))
      (is (= "hflip;hflip" (to-ffmpeg result))))))

(deftest ast-convergence-for-multi-body-for
  (testing "external parser and form->ast agree on the multi-body AST"
    (is (= (dsl-parser multi-body-dsl)
           (into [:program]
                 [(form->ast '(for [i (range 2)] (scale i 10) (hflip)))])))))

(deftest external-evaluator-warns-and-uses-one-body
  (testing "compile-dsl warns and evaluates one body"
    (let [result (compile-dsl multi-body-dsl)]
      (is (for-body-arity-warning?))
      (is (= "scale=width=0:height=10;scale=width=1:height=10" (to-ffmpeg result))))))



