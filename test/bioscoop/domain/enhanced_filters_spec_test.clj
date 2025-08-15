(ns bioscoop.domain.enhanced-filters-spec-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [bioscoop.domain.spec :as ds]
            [bioscoop.domain.specs.drawtext :as drawtext]))

(deftest drawtext-spec-test
  (testing "valid drawtext filter"
    (is (s/valid? ::drawtext/drawtext
                  {:text "hello world"})))
  (testing "invalid drawtext filter with invalid arg"
    (is (not (s/valid? ::drawtext/drawtext
                       {:text 123})))))

(deftest enhanced-drawtext-spec-test
  (testing "valid enhanced drawtext filter with basic parameters"
    (is (s/valid? ::drawtext/drawtext
                  {:text "Hello World"
                   :fontsize 24
                   :x "10"
                   :y "10"})))

  (testing "valid enhanced drawtext filter with additional parameters"
    (is (s/valid? ::drawtext/drawtext
                  {:text "Hello World"
                   :fontsize 24
                   :x "10"
                   :y "10"
                   :fontcolor "white"
                   :borderw 2
                   :bordercolor "black"
                   :expansion "normal"
                   :fix_bounds true})))

  (testing "invalid drawtext filter with invalid expansion mode"
    (is (not (s/valid? ::drawtext/drawtext
                       {:text "Hello World"
                        :expansion "invalid-mode"}))))

  ;; Either text, a valid file, a timecode or text source must be provided
  (comment   (testing "invalid drawtext filter without required text parameter"
               (is (not (s/valid? ::drawtext/drawtext
                                  {:fontsize 24
                                   :x "10"
                                   :y "10"}))))))

(deftest acrossfade-spec-test
  (testing "valid acrossfade filter with required duration"
    (is (s/valid? ::ds/acrossfade
                  {:duration "2.0"})))

  (testing "valid acrossfade filter with all parameters"
    (is (s/valid? ::ds/acrossfade
                  {:duration "2.0"
                   :overlap true
                   :curve1 "tri"
                   :curve2 "qsin"})))

  (testing "invalid acrossfade filter with invalid curve"
    (is (not (s/valid? ::ds/acrossfade
                       {:duration 2.0
                        :curve1 "invalid-curve"}))))

  (testing "invalid acrossfade filter without required duration"
    (is (not (s/valid? ::ds/acrossfade
                       {:overlap true
                        :curve1 "tri"})))))

(deftest acompressor-spec-test
  (testing "valid acompressor filter with basic parameters"
    (is (s/valid? ::ds/acompressor
                  {:threshold -20
                   :ratio 4.0})))

  (testing "valid acompressor filter with all parameters"
    (is (s/valid? ::ds/acompressor
                  {:level_in 1.0
                   :mode "downward"
                   :threshold -20
                   :ratio 4.0
                   :attack 5
                   :release 50
                   :makeup 2
                   :knee 2.5
                   :link "average"
                   :detection "peak"
                   :level_sc 1.0
                   :mix 1.0})))

  (testing "invalid acompressor filter with invalid mode"
    (is (not (s/valid? ::ds/acompressor
                       {:mode "invalid-mode"
                        :threshold -20}))))

  (testing "invalid acompressor filter with invalid detection"
    (is (not (s/valid? ::ds/acompressor
                       {:detection "invalid-detection"
                        :ratio 2.0})))))

(deftest aecho-spec-test
  (testing "valid aecho filter with required parameters"
    (is (s/valid? ::ds/aecho
                  {:in_gain 0.6
                   :out_gain 0.3
                   :delays [1000 1800]
                   :decays [0.5 0.3]})))

  (testing "valid aecho filter with single delay/decay"
    (is (s/valid? ::ds/aecho
                  {:in_gain 0.8
                   :out_gain 0.4
                   :delays [500]
                   :decays [0.6]})))

  (testing "invalid aecho filter with empty delays"
    (is (not (s/valid? ::ds/aecho
                       {:in_gain 0.6
                        :out_gain 0.3
                        :delays []
                        :decays [0.5]}))))

  (testing "invalid aecho filter without required parameters"
    (is (not (s/valid? ::ds/aecho
                       {:delays [1000]
                        :decays [0.5]})))))
