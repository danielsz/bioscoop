(ns bioscoop.dsl-test
  (:require [bioscoop.dsl :refer [compile-dsl dsl-parser]]
            [bioscoop.render :refer [to-ffmpeg]]
            [bioscoop.ffmpeg-parser :refer [parse-ffmpeg-filter]]
            [clojure.test :refer [testing deftest is use-fixtures]]
            [instaparse.core :as insta])
  (:import [bioscoop.render FFmpegRenderable]))

(deftest test-dsl-compilation
  (testing "Basic filter creation"
    (let [result (compile-dsl "(filter \"scale\" \"1920:1080\")")]
      (is (= "scale=1920:1080" (to-ffmpeg result)))))

  (testing "Basic filter - structural equivalence"
    (let [foo (compile-dsl "(filter \"scale\" \"1920:1080\")")
          bar (parse-ffmpeg-filter "scale=1920:1080")]
      (is (= foo bar))))

  (testing "Basic named filter creation"
    (let [result (compile-dsl "(scale 1920 1080)")]
      (is (= "scale=1920:1080" (to-ffmpeg result)))))

  (testing "Basic named filter - structural equivalence"
    (let [foo (compile-dsl "(scale 1920 1080)")
          bar (parse-ffmpeg-filter "scale=1920:1080")]
      (is (= foo bar))))

  (testing "Filter with labels"
    (let [dsl "(let [input-vid (input-labels \"in\")
                     scaled (filter \"scale\" \"1920:1080\" input-vid (output-labels \"scaled\"))]
                 scaled)"
          result (compile-dsl dsl)]
      (is (= "[in]scale=1920:1080[scaled]" (to-ffmpeg result)))))

  (testing "Filter with labels - structural equivalence"
    (let [dsl "(let [input-vid (input-labels \"in\")
                     scaled (filter \"scale\" \"1920:1080\" input-vid (output-labels \"scaled\"))]
                 scaled)"
          foo (compile-dsl dsl)
          bar (parse-ffmpeg-filter "[in]scale=1920:1080[scaled]")]
      (is (= foo bar))))

  (testing "Filter chain"
    (let [dsl "(chain 
                 (filter \"scale\" \"1920:1080\")
                 (filter \"overlay\"))"
          result (compile-dsl dsl)]
      (is (= "scale=1920:1080,overlay" (to-ffmpeg result)))))

  (testing "Filter chain - structural equivalence"
    (let [dsl "(chain 
                 (filter \"scale\" \"1920:1080\")
                 (filter \"overlay\"))"
          foo (compile-dsl dsl)
          bar (parse-ffmpeg-filter "scale=1920:1080,overlay")]
      (is (= foo bar))))

  (testing "Parent scope access"
    (let [dsl "(let [width 1920]
                 (let [height 1080]
                   (scale width height)))"
          result (compile-dsl dsl)]
      (is (= "scale=1920:1080" (to-ffmpeg result))))))

(deftest test-grammar-parse-trees
  (testing "Let binding parse tree structure"
    (let [parse-result (dsl-parser "(let [x 1920 y 1080] (scale x y))")]
      (is (not (insta/failure? parse-result)))
      ;; The parse tree should look like:
      ;; [:program 
      ;;   [:let-binding 
      ;;     [:binding-vector 
      ;;       [:binding [:symbol "x"] [:number "1920"]]
      ;;       [:binding [:symbol "y"] [:number "1080"]]]
      ;;     [:list [:symbol "scale"] [:symbol "x"] [:symbol "y"]]]]
      (is (= :program (first parse-result)))))

  (testing "Simple let binding parse"
    (let [parse-result (dsl-parser "(let [foo 2] foo)")]
      (is (not (insta/failure? parse-result)))
      (is (= :let-binding (first (second parse-result))))))

  (testing "Multiple expressions in let body"
    (let [parse-result (dsl-parser "(let [x 1] x (scale x 480))")]
      (is (not (insta/failure? parse-result)))
      ;; Should have two expressions in the let body
      (let [let-binding (second parse-result)
            body-expressions (drop 2 let-binding)]
        (is (= 2 (count body-expressions)))))))

(deftest test-programs
  (testing "let binding should return valid structures (filter, filterchain, filtergraph)"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"^DSL programs" (compile-dsl "(let [x 1] x)")))))

(deftest let-bindings
  (testing "substitution"
    (is (= "scale=1920:1080"(to-ffmpeg (compile-dsl "(let [width 1920] (scale width 1080))")))))
  (testing "substitution - structural equivalence"
    (let [foo (compile-dsl "(let [width 1920] (scale width 1080))")
          bar (parse-ffmpeg-filter "scale=1920:1080")]
      (is (= foo bar))))
  (testing "arithmetic in let binding expression"
    (is (= "scale=1920:1080" (to-ffmpeg (compile-dsl "(let [width (+ 1919 1)] (scale width 1080))")))))
  (testing "arithmetic - structural equivalence"
    (let [foo (compile-dsl "(let [width (+ 1919 1)] (scale width 1080))")
          bar (parse-ffmpeg-filter "scale=1920:1080")]
      (is (= foo bar))))
  (testing "Mathematical functions from clojure.core"
    (is (= "scale=4:1080" (to-ffmpeg (compile-dsl "(let [width (mod 10 6)] (scale width 1080))"))))
    (is (= "scale=1920:1920" (to-ffmpeg (compile-dsl "(let [size (max 1920 1080)] (scale size size))"))))
    (is (= "scale=10:100" (to-ffmpeg (compile-dsl "(let [offset (abs -10)] (scale offset 100))"))))
    (is (= "scale=1920:1080" (to-ffmpeg (compile-dsl "(let [next (inc 1919)] (scale next 1080))")))))
  (testing "Nested expressions work"
    (is (= "scale=5:100" (to-ffmpeg (compile-dsl "(let [result (inc (mod 10 6))] (scale result 100))")))))
  (testing "Negative numbers work properly"
    (is (= "scale=10:100" (to-ffmpeg (compile-dsl "(let [offset (abs -10)] (scale offset 100))")))))
  (testing "Unknown functions still become filters"
    (is (= "nonexistent=123:456" (to-ffmpeg (compile-dsl "(nonexistent 123 456)"))))))

(deftest test-roundtrip
  (testing "DSL -> FFmpeg -> DSL roundtrip"
    (let [original-dsl "(chain (filter \"scale\" \"1920:1080\") (filter \"overlay\"))"
          compiled (compile-dsl original-dsl)
          ffmpeg-output (to-ffmpeg compiled)
          parsed-back (parse-ffmpeg-filter ffmpeg-output)]
      (is (= ffmpeg-output (to-ffmpeg parsed-back))))))
