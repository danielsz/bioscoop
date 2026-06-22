(ns bioscoop.dsl.chain-test
  (:require [clojure.test :refer [deftest testing is]]
            [bioscoop.dsl :refer [compile-dsl last-errors]]
            [bioscoop.macro :refer [bioscoop defgraph]]
            [bioscoop.render :refer [to-ffmpeg]]
            [bioscoop.domain.records :refer [get-input-labels get-output-labels]])
  (:import [bioscoop.domain.records FilterGraph]))

;; ── Helpers ───────────────────────────────────────────────────────────────────

(defn chain-count [fg] (count (:chains fg)))
(defn filter-count [fg] (count (:filters (first (:chains fg)))))
(defn filter-names [fg] (mapv :name (:filters (first (:chains fg)))))
(defn first-filter [fg] (first (:filters (first (:chains fg)))))
(defn last-filter  [fg] (last  (:filters (first (:chains fg)))))

;; ── Basic Structure ───────────────────────────────────────────────────────────

(deftest chain-produces-single-filterchain
  (testing "chain of bare filters produces one chain"
    (let [result (compile-dsl "(chain (scale 1920 1080) (eq) (crop \"iw\" \"ih\"))")]
      (is (instance? FilterGraph result))
      (is (= 1 (chain-count result)))
      (is (= 3 (filter-count result)))))

  (testing "filter order is preserved"
    (let [result (compile-dsl "(chain (scale 1920 1080) (eq) (crop \"iw\" \"ih\"))")]
      (is (= ["scale" "eq" "crop"] (filter-names result)))))

  (testing "single filter in chain"
    (let [result (compile-dsl "(chain (scale 1920 1080))")]
      (is (= 1 (chain-count result)))
      (is (= 1 (filter-count result)))))

  (testing "chain renders as comma-separated filters"
    (is (= "scale=width=1920:height=1080,eq"
           (to-ffmpeg (compile-dsl "(chain (scale 1920 1080) (eq))"))))))

;; ── Flattening ────────────────────────────────────────────────────────────────

(deftest chain-flattens-nested-chains
  (testing "nested chain is flattened into parent chain"
    (let [result (compile-dsl "(chain (chain (scale 1920 1080) (eq)) (crop \"iw\" \"ih\"))")]
      (is (= 1 (chain-count result)))
      (is (= 3 (filter-count result)))
      (is (= ["scale" "eq" "crop"] (filter-names result)))))

  (testing "multiple nested chains are flattened"
    (let [result (compile-dsl "(chain (chain (scale 1920 1080)) (chain (eq) (crop \"iw\" \"ih\")))")]
      (is (= 1 (chain-count result)))
      (is (= 3 (filter-count result))))))

(deftest chain-flattens-single-chain-filtergraph
  (testing "defgraph with single chain is flattened into chain"
    (defgraph grading (chain (eq {:contrast "1.1"}) (curves {:preset "lighter"})))
    (let [result (bioscoop (chain (scale 1920 1080) grading (crop "iw" "ih")))]
      (is (= 1 (chain-count result)))
      (is (= 4 (filter-count result)))
      (is (= ["scale" "eq" "curves" "crop"] (filter-names result)))))

  (testing "filtergraph with single chain flattens correctly via compile-dsl"
    (let [result (compile-dsl "(chain (scale 1920 1080) (chain (eq) (curves)) (crop \"iw\" \"ih\"))")]
      (is (= 1 (chain-count result)))
      (is (= 4 (filter-count result)))))

  (testing "renders correctly after flattening"
    (defgraph normalize (chain (curves {:preset "lighter"}) (eq {:contrast "1.1"})))
    (let [result (bioscoop (chain (scale 1920 1080) normalize))]
      (is (= "scale=width=1920:height=1080,curves=preset=lighter,eq=contrast=1.1"
             (to-ffmpeg result))))))

;; ── Error Cases ───────────────────────────────────────────────────────────────

(deftest chain-rejects-parallel-filtergraph
  (testing "multi-chain filtergraph inside chain triggers error"
    (defgraph parallel
      (compose
        [["a"] (scale 1920 1080) ["b"]]
        [["c"] (eq)              ["d"]]))
    (let [result (bioscoop (chain (scale 1920 1080) parallel))]
      (is (instance? FilterGraph result))
      (is (seq @last-errors))
      (is (= :chain-parallel-filtergraph
             (:error-type (ex-data (first @last-errors)))))))

  (testing "error does not prevent other filters from being processed"
    ;; the parallel graph contributes [] filters, others still present
    (defgraph parallel2
      (compose
        [["a"] (scale 1920 1080) ["b"]]
        [["c"] (eq)              ["d"]]))
    (let [result (bioscoop (chain (scale 1920 1080) parallel2 (crop "iw" "ih")))]
      (is (instance? FilterGraph result)))))

;; ── Labels ────────────────────────────────────────────────────────────────────

(deftest chain-labels-via-padded-graph
  (testing "input label applied to first filter"
    (let [result (compile-dsl "[[\"0:v\"] (chain (scale 1920 1080) (eq)) [\"out\"]]")]
      (is (= ["0:v"] (get-input-labels  (first-filter result))))
      (is (= ["out"] (get-output-labels (last-filter  result))))))

  (testing "input label on first filter of flattened defgraph chain"
    (defgraph grading2 (chain (eq {:contrast "1.1"}) (curves {:preset "lighter"})))
    (let [result (bioscoop [["0:v"] (chain (scale 1920 1080) grading2) ["out"]])]
      (is (= ["0:v"] (get-input-labels  (first-filter result))))
      (is (= ["out"] (get-output-labels (last-filter  result))))))

  (testing "labels render correctly in ffmpeg output"
    (is (= "[0:v]scale=width=1920:height=1080,eq[out]"
           (to-ffmpeg (compile-dsl "[[\"0:v\"] (chain (scale 1920 1080) (eq)) [\"out\"]]")))))

  (testing "multiple input labels"
    (let [result (compile-dsl "[[\"0:v\"][\"1:v\"] (chain (scale 1920 1080)) [\"out\"]]")]
      (is (= ["0:v" "1:v"] (get-input-labels (first-filter result))))))

  (testing "no labels — chain still renders correctly"
    (is (= "scale=width=1920:height=1080,eq"
           (to-ffmpeg (compile-dsl "(chain (scale 1920 1080) (eq))"))))))

;; ── Composition with compose ──────────────────────────────────────────────────

(deftest chain-inside-compose
  (testing "two chains in compose produce two filterchains"
    (let [result (compile-dsl
                   "(compose
                      [[\"0:v\"] (chain (scale 1920 1080) (eq)) [\"video\"]]
                      [[\"0:a\"] (chain (volume) (atempo))       [\"audio\"]])")]
      (is (= 2 (chain-count result)))))

  (testing "compose renders chains separated by semicolons"
    (let [result (compile-dsl
                   "(compose
                      [[\"0:v\"] (chain (scale 1920 1080) (eq)) [\"video\"]]
                      [[\"0:a\"] (chain (volume) (atempo))       [\"audio\"]])")]
      (is (= "[0:v]scale=width=1920:height=1080,eq[video];[0:a]volume,atempo[audio]"
             (to-ffmpeg result)))))

  (testing "chain with defgraph stages in compose"
    (defgraph grading3 (chain (curves {:preset "lighter"}) (eq {:contrast "1.1"})))
    (defgraph aprocessing (chain (volume) (atempo)))
    (let [result (bioscoop
                   (compose
                     [["0:v"] (chain (scale 1920 1080) grading3) ["video"]]
                     [["0:a"] aprocessing                        ["audio"]]))]
      (is (= 2 (chain-count result))))))

;; ── Macro path ────────────────────────────────────────────────────────────────

(deftest chain-macro-path
  (testing "macro and string paths produce same chain count"
    (let [string-result (compile-dsl "(chain (scale 1920 1080) (eq) (crop \"iw\" \"ih\"))")
          macro-result  (bioscoop (chain (scale 1920 1080) (eq) (crop "iw" "ih")))]
      (is (= (chain-count  string-result) (chain-count  macro-result)))
      (is (= (filter-count string-result) (filter-count macro-result)))
      (is (= (filter-names string-result) (filter-names macro-result)))))

  (testing "macro path renders identically to string path"
    (is (= (to-ffmpeg (compile-dsl "(chain (scale 1920 1080) (eq))"))
           (to-ffmpeg (bioscoop (chain (scale 1920 1080) (eq))))))))
