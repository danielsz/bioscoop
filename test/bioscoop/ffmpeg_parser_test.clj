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


(deftest ffmpeg-grammar
  (testing "Filter with positional arguments"
    (let [expr "scale=640:480"]
      (is (= (ffmpeg/ffmpeg-parser expr)
             [:filtergraph
              [:filterchain
               [:filter
                [:filter-spec
                 [:filter-name "scale"]
                 [:filter-arguments [:unquoted-args "640:480"]]]]]]))))
  (testing "Labeled filter"
    (let [expr "scale@hd=1280:720"]
      (is (= (ffmpeg/ffmpeg-parser expr)
             [:filtergraph
              [:filterchain
               [:filter
                [:filter-spec
                 [:filter-name "scale" [:at "@"] "hd"]
                 [:filter-arguments [:unquoted-args "1280:720"]]]]]]))))
  (testing "Multiple output linklabels"
    (let [expr "[in]scale=1280:720[scaled1][scaled2]"]
      (is (= (ffmpeg/ffmpeg-parser expr)
             [:filtergraph
              [:filterchain
               [:filter
                [:input-linklabels [:linklabel "in"]]
                [:filter-spec
                 [:filter-name "scale"]
                 [:filter-arguments [:unquoted-args "1280:720"]]]
                [:output-linklabels
                 [:linklabel "scaled1"]
                 [:linklabel "scaled2"]]]]])))
    (let [expr "[0:v]split[o0][o1]"]
      (is (= (ffmpeg/ffmpeg-parser expr)
             [:filtergraph
              [:filterchain
               [:filter
                [:input-linklabels [:linklabel "0:v"]]
                [:filter-spec [:filter-name "split"]]
                [:output-linklabels [:linklabel "o0"] [:linklabel "o1"]]]]]))))
  (testing "Filter with instance label"
    (let [expr "[in]scale@highres=1920:1080[hd]"]
      (is (= (ffmpeg/ffmpeg-parser expr)
             [:filtergraph
              [:filterchain
               [:filter
                [:input-linklabels [:linklabel "in"]]
                [:filter-spec
                 [:filter-name "scale" [:at "@"] "highres"]
                 [:filter-arguments [:unquoted-args "1920:1080"]]]
                [:output-linklabels [:linklabel "hd"]]]]]))))
  (testing "Quoted string with escapes"
    (let [expr "[v]drawtext=text='Hello\\\"s World':fontsize=24[out]"]
      (is (= (ffmpeg/ffmpeg-parser expr)
             [:filtergraph
              [:filterchain
               [:filter
                [:input-linklabels [:linklabel "v"]]
                [:filter-spec
                 [:filter-name "drawtext"]
                 [:filter-arguments
                  [:unquoted-args "text="]
                  [:quoted-string "Hello\\\"s World"]
                  [:unquoted-args ":fontsize=24"]]]
                [:output-linklabels [:linklabel "out"]]]]])))
    (let [expr "[v]drawtext=text='He said \\\"Hi\\\"':fontsize=24[out]"]
      (is (= (ffmpeg/ffmpeg-parser expr)
             [:filtergraph
              [:filterchain
               [:filter
                [:input-linklabels [:linklabel "v"]]
                [:filter-spec
                 [:filter-name "drawtext"]
                 [:filter-arguments
                  [:unquoted-args "text="]
                  [:quoted-string "He said \\\"Hi\\\""]
                  [:unquoted-args ":fontsize=24"]]]
                [:output-linklabels [:linklabel "out"]]]]]))))
  (testing "Complex argument with special characters"
    (let [expr "[vid]drawtext=text='Price: $50\\%':x=(w-tw)/2:fontsize=24[out]"]
      (is (= (ffmpeg/ffmpeg-parser expr)
             [:filtergraph
              [:filterchain
               [:filter
                [:input-linklabels [:linklabel "vid"]]
                [:filter-spec
                 [:filter-name "drawtext"]
                 [:filter-arguments
                  [:unquoted-args "text="]
                  [:quoted-string "Price: $50\\%"]
                  [:unquoted-args ":x=(w-tw)/2:fontsize=24"]]]
                [:output-linklabels [:linklabel "out"]]]]]))))
  (testing "Multiple filters in chain"
    (let [expr "[0:v]scale=640:360[small]; [1:a]volume=1.5[loud]"]
      (is (= (ffmpeg/ffmpeg-parser expr)
             [:filtergraph
              [:filterchain
               [:filter
                [:input-linklabels [:linklabel "0:v"]]
                [:filter-spec
                 [:filter-name "scale"]
                 [:filter-arguments [:unquoted-args "640:360"]]]
                [:output-linklabels [:linklabel "small"]]]]
              [:filterchain
               [:filter
                [:input-linklabels [:linklabel "1:a"]]
                [:filter-spec
                 [:filter-name "volume"]
                 [:filter-arguments [:unquoted-args "1.5"]]]
                [:output-linklabels [:linklabel "loud"]]]]]))))
  (testing "Complex chain"
    (let [expr "[in]scale@thumb=320:240[small];[in]scale@full=1280:720[big]"]
      (is (= (ffmpeg/ffmpeg-parser expr)
             [:filtergraph
              [:filterchain
               [:filter
                [:input-linklabels [:linklabel "in"]]
                [:filter-spec
                 [:filter-name "scale" [:at "@"] "thumb"]
                 [:filter-arguments [:unquoted-args "320:240"]]]
                [:output-linklabels [:linklabel "small"]]]]
              [:filterchain
               [:filter
                [:input-linklabels [:linklabel "in"]]
                [:filter-spec
                 [:filter-name "scale" [:at "@"] "full"]
                 [:filter-arguments [:unquoted-args "1280:720"]]]
                [:output-linklabels [:linklabel "big"]]]]]))))
  (testing "Sws_flags declaration"
    (let [expr "sws_flags=lanczos+accurate_rnd; [0:v]scale=1920:1080[hd]"]
      (is (= (ffmpeg/ffmpeg-parser expr)
             [:filtergraph
              [:sws-flags [:flags "lanczos+accurate_rnd"]]
              [:filterchain
               [:filter
                [:input-linklabels [:linklabel "0:v"]]
                [:filter-spec
                 [:filter-name "scale"]
                 [:filter-arguments [:unquoted-args "1920:1080"]]]
                [:output-linklabels [:linklabel "hd"]]]]]))))
  (testing "Invalid case (empty linklabel)"
    (let [expr "[in]scale=640:480[]"]
      (is (contains? (ffmpeg/ffmpeg-parser expr) :reason)))))



