(ns bioscoop.ffmpeg-parser-test
  (:require [bioscoop.dsl :refer [compile-dsl]]
            [bioscoop.macro :refer [bioscoop]]
            [bioscoop.render :refer [to-ffmpeg]]
            [bioscoop.ffmpeg-parser :as ffmpeg]
            [clojure.test :refer [testing deftest is]]))

(deftest simple
  (testing "Basic filter - structural equivalence"
    (let [foo (compile-dsl "(scale 1920 1080)")
          bar (ffmpeg/parse "scale=w=1920:h=1080")]
      (is (= foo bar))))
  (testing "Basic named filter - structural equivalence"
    (let [foo (compile-dsl "(scale 1920 1080)")
          bar (ffmpeg/parse "scale=w=1920:h=1080")]
      (is (= foo bar))))
  (testing "Filter with labels - structural equivalence"
    (let [dsl "(let [input-vid (input-labels \"in\")
                     scaled (scale 1920 1080 input-vid (output-labels \"scaled\"))]
                 scaled)"
          foo (compile-dsl dsl)
          bar (ffmpeg/parse "[in]scale=1920:1080[scaled]")]
      (is (= foo bar)))
    (let [dsl "(scale 1920 1080 {:input \"in\"} {:output \"scaled\"})"
          foo (compile-dsl dsl)
          bar (ffmpeg/parse "[in]scale=1920:1080[scaled]")]
      (is (= foo bar)))))

(deftest test-roundtrip
  (testing "DSL -> FFmpeg -> DSL roundtrip"
    (let [original-dsl "(chain (scale 1920 1080) (overlay))"
          compiled (compile-dsl original-dsl)
          ffmpeg-output (to-ffmpeg compiled)
          parsed-back (ffmpeg/parse ffmpeg-output)]
      (is (= ffmpeg-output (to-ffmpeg parsed-back))))))

(deftest let-binding
  (testing "substitution"
    (is (= "scale=width=1920:height=1080" (to-ffmpeg (compile-dsl "(let [width 1920] (scale width 1080))")))))
  (testing "substitution - structural equivalence"
    (let [foo (compile-dsl "(let [width 1920] (scale width 1080))")
          bar (ffmpeg/parse "scale=w=1920:h=1080")]
      (is (= foo bar))))
  (let [foo (bioscoop (let [width 1920] (scale width 1080)))
          bar (ffmpeg/parse "scale=w=1920:h=1080")]
      (is (= foo bar)))
  (testing "arithmetic in let binding expression"
    (is (= "scale=width=1920:height=1080" (to-ffmpeg (compile-dsl "(let [width (+ 1919 1)] (scale width 1080))")))))
  (testing "arithmetic - structural equivalence"
    (let [foo (compile-dsl "(let [width (+ 1919 1)] (scale width 1080))")
          bar (ffmpeg/parse "scale=width=1920:height=1080")]
      (is (= foo bar))))
  (testing "more complicated dsl"
    (let [structures (bioscoop (graph (chain (smptebars {:output "v0"}))
                              (chain (testsrc {:output "v1"}))
                              (chain (pad {:width "iw*2" :height "ih"} {:input "v0"} {:output "out0"}))
                              (chain (overlay {:x "w"} {:input "out0"} {:input "v1"}))))
          s (to-ffmpeg structures)
          back (ffmpeg/parse s)]
      (is (= structures back)))))

