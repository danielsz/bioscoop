(ns bioscoop.render-test
  (:require [bioscoop.dsl :refer [compile-dsl]]
            [bioscoop.render :refer [to-dsl to-ffmpeg]]
            [clojure.test :refer [testing deftest is]]
            [bioscoop.ffmpeg-parser :as ffmpeg-parser])
  (:import [bioscoop.domain.records Filter FilterChain FilterGraph]))

(deftest test-to-dsl-basic-filters
  (testing "Basic scale filter"
    (let [structure (compile-dsl "(scale 1920 1080)")
          back-to-dsl (to-dsl structure)]
      (is (= "(scale {:width 1920, :height 1080})" back-to-dsl))))

  (testing "Basic crop filter with quoted args"
    (let [structure (compile-dsl "(crop \"iw/2\" \"ih\" \"0\" \"0\")")
          back-to-dsl (to-dsl structure)]
      (is (= "(crop {:out_w \"iw/2\", :w \"ih\", :out_h \"0\", :h \"0\"})" back-to-dsl))))

  (testing "Basic overlay filter (no args)"
    (let [structure (compile-dsl "(overlay)")
          back-to-dsl (to-dsl structure)]
      (is (= "(overlay)" back-to-dsl))))

  (testing "Generic filter"
    (let [structure (compile-dsl "(hflip)")
          back-to-dsl (to-dsl structure)]
      (is (= "(hflip)" back-to-dsl))))

  (testing "Generic filter with args"
    (let [structure (compile-dsl "(drawtext {:text \"Hello\"})")
          back-to-dsl (to-dsl structure)]
      (is (= "(drawtext {:text \"Hello\"})" back-to-dsl)))))

(deftest test-to-dsl-with-labels
  (testing "Filter with input and output labels"
    (let [structure (compile-dsl "(scale 1920 1080 {:input \"in\"} {:output \"scaled\"})")
          back-to-dsl (to-dsl structure)]
      (is (= "(scale {:width 1920, :height 1080} {:input \"in\"} {:output \"scaled\"})" back-to-dsl))))

  (testing "Filter with only input label"
    (let [structure (compile-dsl "(hflip {:input \"tmp\"})")
          back-to-dsl (to-dsl structure)]
      (is (= "(hflip {:input \"tmp\"})" back-to-dsl))))

  (testing "Filter with only output label"
    (let [structure (compile-dsl "(split {:output \"left\"})")
          back-to-dsl (to-dsl structure)]
      (is (= "(split {:output \"left\"})" back-to-dsl)))))

(deftest test-to-dsl-chains-and-graphs
  (testing "Simple chain"
    (let [structure (compile-dsl "(chain (scale 1920 1080) (overlay))")
          back-to-dsl (to-dsl structure)]
      (is (= "(chain (scale {:width 1920, :height 1080}) (overlay))" back-to-dsl))))

  (testing "Simple graph"
    (let [structure (compile-dsl "(graph (scale 1920 1080) (overlay))")
          back-to-dsl (to-dsl structure)]
      (is (= "(graph (scale {:width 1920, :height 1080}) (overlay))" back-to-dsl)))))


(deftest test-roundtrip-consistency
  (testing "DSL -> structure -> DSL roundtrip preserves semantics"
    (let [test-cases ["(scale 1920 1080)"
                      "(chain (scale 1920 1080) (overlay))"
                      "(graph (scale 1920 1080) (overlay))"
                      "(hflip)"
                      "(scale 1920 1080 {:input \"in\"} {:output \"scaled\"})"
                      "(crop \"iw/2\" \"ih\" \"0\" \"0\")"]]
      (doseq [original-dsl test-cases]
        (let [parsed (compile-dsl original-dsl)
              back-to-dsl (to-dsl parsed)
              reparsed (compile-dsl back-to-dsl)]
          (is (= parsed reparsed)
              (str "Roundtrip failed for: " original-dsl
                   "\nBack to DSL: " back-to-dsl))))))

  (testing "FFmpeg compatibility is preserved"
    (let [test-cases ["(scale 1920 1080)"
                      "(chain (scale 1920 1080) (overlay))"
                      "(graph (scale 1920 1080) (overlay))"]]
      (doseq [original-dsl test-cases]
        (let [original-structure (compile-dsl original-dsl)
              original-ffmpeg (to-ffmpeg original-structure)
              back-to-dsl (to-dsl original-structure)
              reparsed-structure (compile-dsl back-to-dsl)
              reparsed-ffmpeg (to-ffmpeg reparsed-structure)]
          (is (= original-ffmpeg reparsed-ffmpeg)
              (str "FFmpeg output differs for: " original-dsl
                   "\nOriginal FFmpeg: " original-ffmpeg
                   "\nReparsed FFmpeg: " reparsed-ffmpeg))))))

  (testing "Complex nested structure"
    (let [structure (compile-dsl "(graph (chain (crop \"iw/2\" \"ih\" \"0\" \"0\") (split (output-labels \"left\" \"tmp\"))) (hflip {:input \"tmp\"} {:output \"right\"}))")
          back-to-dsl (to-dsl structure)]
      (let [reparsed (compile-dsl back-to-dsl)]
        (is (= structure reparsed)))))

  (testing "real world"
    (let [filtergraph "testsrc,scale=width=qvga[a];rgbtestsrc,scale=width=qvga[b];smptebars,scale=width=qvga[c];yuvtestsrc,scale=width=qvga[d];[a][b][c][d]xstack=inputs=4:layout=0_0|0_h0|w0_0|w0_h0[out]"
          structures (ffmpeg-parser/parse filtergraph)
          back-to-dsl (to-dsl structures)]
      (is (= structures (compile-dsl back-to-dsl)))))
  )
