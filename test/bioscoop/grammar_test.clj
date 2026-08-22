(ns bioscoop.grammar-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.walk :refer [postwalk]]
            [instaparse.core :as insta]
            [bioscoop.dsl :refer [compile-dsl last-errors]]
            [bioscoop.parse :refer [dsl-parser dsl-parses]]
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

(deftest function-definitions
  (testing "defn with zero params parses"
    (is (not (insta/failure? (dsl-parser "(defn bars [] (smptebars))")))))
  (testing "defn with two params parses"
    (is (not (insta/failure? (dsl-parser "(defn letterbox [w h] (scale w h))")))))
  (testing "defn with a single param parses (see ambiguity note below)"
    (is (not (insta/failure? (dsl-parser "(defn double [x] (scale x x))")))))
  (testing "defn body can contain multiple expressions"
    (is (not (insta/failure? (dsl-parser "(defn f [x] (scale x 1080) (fade {:type \"in\"}))")))))
  (testing "defn allows hyphenated and predicate-style names"
    (is (not (insta/failure? (dsl-parser "(defn valid-input? [x] x)")))))
  (testing "defn with zero params produces a well-formed function-definition node"
    (let [ast (dsl-parser "(defn bars [] (smptebars))")
          [fd] (collect-nodes :function-definition ast)]
      (is (= [:symbol "bars"] (nth fd 1)))
      (is (= [:params] (nth fd 2)))
      (is (= 4 (count fd)))))
  (testing "defn with multiple params produces a well-formed function-definition node"
    (let [ast (dsl-parser "(defn letterbox [w h] (scale w h))")
          [fd] (collect-nodes :function-definition ast)]
      (is (= [:symbol "letterbox"] (nth fd 1)))
      (is (= [:params [:symbol "w"] [:symbol "h"]] (nth fd 2)))
      (is (= 4 (count fd))))))

(deftest lambda-parsing
  (testing "fn with zero params parses"
    (is (not (insta/failure? (dsl-parser "(fn [] (smptebars))")))))
  (testing "fn with two params parses"
    (is (not (insta/failure? (dsl-parser "(fn [x y] (scale x y))")))))
  (testing "fn with a single param parses"
    (is (not (insta/failure? (dsl-parser "(fn [x] (scale x 1080))")))))
  (testing "fn can appear directly in operator position of a call"
    (is (not (insta/failure? (dsl-parser "((fn [x] (scale x 1080)) 100)")))))
  (testing "fn can be bound with let, matching existing binding style"
    (is (not (insta/failure? (dsl-parser "(let [box (fn [x] (drawbox {:x x}))] (box 10))")))))
  (testing "fn body respects minus-disambiguation"
    (let [ast (dsl-parser "(fn [w] (scale -1 w))")]
      (is (not (insta/failure? ast)))
      (is (some #(= [:number "-1"] %) (collect-nodes :number ast)))
      (is (not (some #(= [:symbol "-"] %) (collect-nodes :symbol ast))))))
  (testing "fn with two params produces a well-formed lambda node"
    (let [ast (dsl-parser "(fn [x y] (scale x y))")
          [lam] (collect-nodes :lambda ast)]
      (is (= [:params [:symbol "x"] [:symbol "y"]] (nth lam 1))))))

(deftest single-arg-ambiguity
  (testing "a single-arg defn has more than one valid parse"
    (is (< 1 (count (dsl-parses "(defn f [x] (scale x 1080))")))))
  (testing "a single-arg fn has more than one valid parse"
    (is (< 1 (count (dsl-parses "(fn [x] (scale x 1080))"))))))

(deftest single-arg-default-resolution
  (testing "dsl-parser's single result resolves ambiguous single-arg defn to function-definition"
    (let [ast (dsl-parser "(defn f [x] (scale x 1080))")]
      (is (= :function-definition (first (second ast))))))
  (testing "dsl-parser's single result resolves ambiguous single-arg fn to lambda"
    (let [ast (dsl-parser "(fn [x] (scale x 1080))")]
      (is (= :lambda (first (second ast)))))))

(deftest function-application
  (testing "applying a defn'd function with multiple params"
    (is (= "scale=width=1920:height=1080"
           (to-ffmpeg (compile-dsl "(defn letterbox [w h] (scale w h)) (letterbox 1920 1080)")))))
  (testing "applying a defn'd function with zero params"
    (is (= "smptebars"
           (to-ffmpeg (compile-dsl "(defn bars [] (smptebars)) (bars)")))))
  (testing "applying an anonymous fn directly in operator position"
    (is (= "scale=width=100:height=1080"
           (to-ffmpeg (compile-dsl "((fn [x] (scale x 1080)) 100)")))))
  (testing "applying an anonymous fn with multiple params"
    (is (= "scale=width=100:height=200"
           (to-ffmpeg (compile-dsl "((fn [x y] (scale x y)) 100 200)")))))
  (testing "applying a fn bound with let"
    (is (= "scale=width=10:height=1080"
           (to-ffmpeg (compile-dsl "(let [box (fn [x] (scale x 1080))] (box 10))")))))
  (testing "a fn closes over the surrounding let scope"
    (is (= "scale=width=1280:height=720"
           (to-ffmpeg (compile-dsl "(let [h 720] ((fn [x] (scale x h)) 1280))")))))
  (testing "only the last body expression is returned"
    (is (= "scale=width=100:height=100;hflip"
           (to-ffmpeg (compile-dsl "(defn two [x] (scale x x) (hflip)) (two 100)")))))
  (testing "a defn body can build a chain"
    (is (= "scale=width=1920:height=1080,fade=type=in"
           (to-ffmpeg (compile-dsl "(defn letterbox [w h] (chain (scale w h) (fade {:type \"in\"}))) (letterbox 1920 1080)")))))
  (testing "a defn can call another defn"
    (is (= "scale=width=1920:height=1080"
           (to-ffmpeg (compile-dsl "(defn first-f [x] (scale x 1080)) (defn second-f [x] (first-f x)) (second-f 1920)")))))
  (testing "functions can be passed as arguments to other functions"
    (is (= "scale=width=100:height=100"
           (to-ffmpeg (compile-dsl "(defn apply-twice [f x] (f x x)) (apply-twice (fn [a b] (scale a b)) 100)")))))
  (testing "function params can feed keyword arguments"
    (is (= "drawbox=x=10:y=20:w=10:h=20"
           (to-ffmpeg (compile-dsl "(defn box [w h] (drawbox {:x w :y h :w w :h h})) (box \"10\" \"20\")")))))
  (testing "applying a function with the wrong number of arguments reports an arity-mismatch"
    (let [result (compile-dsl "(defn f [x] (scale x 1080)) (f 1 2)")]
      (is (= "" (to-ffmpeg result)))
      (is (= :arity-mismatch (:error-type (ex-data (first @last-errors))))))))
