(ns bioscoop.grammar-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.walk :refer [postwalk]]
            [instaparse.core :as insta]
            [bioscoop.dsl :refer [dsl-parser compile-dsl]]
            [bioscoop.render :refer [to-ffmpeg]]))

(defn collect-nodes [tag ast]
  (let [acc (atom [])]
    (postwalk (fn [node]
                (when (and (vector? node) (= tag (first node)))
                  (swap! acc conj node))
                node)
              ast)
    @acc))

(deftest minus-disambiguation

  (testing "-1 parses as a single number node, not operator + number"
    (let [ast (dsl-parser "(scale -1 1080)")]
      (is (not (insta/failure? ast)))
      (is (some #(= [:number "-1"] %) (collect-nodes :number ast)))
      (is (not (some #(= [:symbol "-"] %) (collect-nodes :symbol ast))))))

  (testing "-1 as scale argument compiles to correct ffmpeg string"
    (is (= "scale=width=-1:height=1080"
           (to-ffmpeg (compile-dsl "(scale -1 1080)")))))

  (testing "(- a b) evaluates correctly as subtraction"
    (is (= "scale=width=980:height=1080"
           (to-ffmpeg (compile-dsl "(scale {:width (- 1080 100) :height 1080})")))))

  (testing "negative number in arithmetic context"
    (is (= "scale=width=100:height=1080"
           (to-ffmpeg (compile-dsl "(scale {:width (+ 101 -1) :height 1080})"))))))


(deftest padded-graphs
  (testing "regular padded graphs"
    (let [ast (dsl-parser "[[o1](scale -1 1080)[o3]]")]
      (is (not (insta/failure? ast))))
    (let [ast (dsl-parser "[[i1] (scale {:x 1080})[02]]")]
      (is (not (insta/failure? ast))))
    (let [ast (dsl-parser "[[(for [i (range 5)] (str i))] (scale {:x 1080})[02]]")]
      (is (not (insta/failure? ast))))
    (is (= (to-ffmpeg (compile-dsl "[[(for [i (range 5)] (str \"in\" i))] (scale {:x 1080})[02]]"))
           "[in0][in1][in2][in3][in4]scale=x=1080[2]"))
    (is (= (to-ffmpeg (compile-dsl "[[(for [i (range 5)] (str \"in\" i))] (scale {:x 1080})[(for [i (range 5)] (str \"out\" i))]]"))
           "[in0][in1][in2][in3][in4]scale=x=1080[out0][out1][out2][out3][out4]"))
    (is (not (insta/failure? (dsl-parser "[[i1] (scale {:x 1080})]"))))))
