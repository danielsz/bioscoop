(ns bioscoop.dsl-test
  (:require [bioscoop.dsl :refer [compile-dsl last-errors]]
            [bioscoop.parse :refer [dsl-parser dsl-parses]]
            [bioscoop.render :refer [to-ffmpeg]]
            [bioscoop.ffmpeg-parser :as ffmpeg]
            [bioscoop.built-in]
            [clojure.test :refer [testing deftest is]]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [instaparse.core :as insta]
            [bioscoop.domain.records :refer [get-input-labels get-output-labels]])
  (:import [bioscoop.domain.records FilterGraph]))

;; Native image testing support
(def native-binary-path "target/bioscoop")

(defn native-binary-exists? []
  (.exists (io/file native-binary-path)))

(defn run-native
  "Run DSL code through the native binary, return stdout trimmed"
  [dsl-code]
  (let [{:keys [out err exit]} (sh native-binary-path "-e" dsl-code)]
    (when (not= 0 exit)
      (throw (ex-info "Native binary failed" {:exit exit :err err :dsl dsl-code})))
    (str/trim out)))

(def dsl-expressions
  [{:title "Basic filter creation"
    :dsl "(scale 1920 1080)"
    :expected "scale=width=1920:height=1080"}
   {:title "Filter with labels"
    :dsl "[[\"in\"] (scale 1920 1080) [\"scaled\"]]"
    :expected "[in]scale=width=1920:height=1080[scaled]"}
   {:title "Multiple expressions, implicit filterchain"
    :dsl "(scale 1920 1080) (overlay)"
    :expected "scale=width=1920:height=1080;overlay"}
   {:title "Filterchain"
    :dsl "(chain (scale 1920 1080) (overlay))"
    :expected "scale=width=1920:height=1080,overlay"}
   {:title "Nested filterchains"
    :dsl "(chain (scale \"1920\" \"1080\") (overlay)) (hflip)"
    :expected "scale=width=1920:height=1080,overlay;hflip"}
   {:title "Parent scope access and nesting"
    :dsl "(let [height 1920]
                 (let [width 1080]
                   (scale height width)))"
    :expected "scale=width=1920:height=1080"}
   {:title "Parent scope access and nesting II"
    :dsl "(let [width 1920]
                 (let [width 1280]
                   (scale 1080 width)))"
    :expected "scale=width=1080:height=1280"}
   {:title "Parent scope access and nesting III"
    :dsl "(let [width 1920]
                 (let [width 1280]
                   (let [width 800]
                     (scale 1080 width))))"
    :expected "scale=width=1080:height=800"}
   {:title "Real world - flip"
    :dsl "(compose [(chain (crop \"iw/2\" \"ih\" \"0\" \"0\") (split)) [\"left\"][\"tmp\"]]
                   [[\"tmp\"] (hflip) [\"right\"]]
                   [[\"left\"] [\"right\"](hstack)])"
    :expected "crop=out_w=iw/2:w=ih:out_h=0:h=0,split[left][tmp];[tmp]hflip[right];[left][right]hstack"}
   {:title "defgraph is being compiled before expressions"
    :dsl "(defgraph a (scale 1920 1080)) a"
    :expected "scale=width=1920:height=1080"}
   {:title "Compose I"
    :dsl "(defgraph a (scale 1920 1080)) (defgraph b (crop \"640\" \"480\")) (compose a b)"
    :expected "scale=width=1920:height=1080;crop=out_w=640:w=480"}
   {:title "Compose II"
    :dsl "(defgraph x (chain (scale 1920 1080) (hflip))) (defgraph y (vflip)) (compose x y)"
    :expected "scale=width=1920:height=1080,hflip;vflip"}
   {:title "Compose III"
    :dsl "(compose [[0] (chain (scale 133 220)) [1]] [[0] (crop \"111\") [1]])"
    :expected "[0]scale=width=133:height=220[1];[0]crop=out_w=111[1]"}
   {:title "Padded graphs"
    :dsl "[[in] (scale 1920 1080) [out]]"
    :expected "[in]scale=width=1920:height=1080[out]"}
   {:title "Padded graphs II"
    :dsl "[[0][1] (chain (scale 1920 1080) (overlay)) [out]]"
    :expected "[0][1]scale=width=1920:height=1080,overlay[out]"}
   {:title "Args as maps"
    :dsl "(color {:color \"blue\" :size \"1920x1080\" :rate 24 :duration \"10\" :sar \"16/9\"})"
    :expected "color=color=blue:size=1920x1080:rate=24:duration=10:sar=16/9"}
   {:title "Inline filterchain"
    :dsl "[[in][off] (chain (scale 1920 1080) (crop \"220\")) [out]]"
    :expected "[in][off]scale=width=1920:height=1080,crop=out_w=220[out]"}
   {:title "Single filter"
    :dsl "(defgraph my-crop (crop \"220\"))\n[[in][off] my-crop [out]]"
    :expected "[in][off]crop=out_w=220[out]"}
   {:title "Filterchain with two filters"
    :dsl "(defgraph my-scale (chain (scale 1920 1080) (crop \"220\")))\n[[in][off] my-scale [out]]"
    :expected "[in][off]scale=width=1920:height=1080,crop=out_w=220[out]"}
   {:title "Let bindings"
    :dsl "(let [size 1920] (scale size size))"
    :expected "scale=width=1920:height=1920"}
   {:title "Let bindings 2"
    :dsl "(let [x (mod 10 3)] (scale x 1080))"
    :expected "scale=width=1:height=1080"}])

(deftest test-dsl-expressions
  (doseq [{:keys [title dsl expected]} dsl-expressions]
    (testing title
      (is (= expected (to-ffmpeg (compile-dsl dsl)))))))

(deftest native-image-parity
  (if (native-binary-exists?)
    (doseq [{:keys [title dsl expected]} dsl-expressions]
      (testing (str "Native image:" title)
        (is (= expected (run-native dsl)))))
    (testing "Native binary not found - skipping native tests"
      (println "Native binary not found at" native-binary-path "- skipping native image tests")
      (is true))))

(deftest test-dsl-compilation
  (testing "Multiple expressions, one is invalid"
    (let [dsl "(scale 1920 1080)
               (overlay) 1"
          result (compile-dsl dsl)]
      (is (instance? FilterGraph result))
      (is (= :not-a-filtergraph (:error-type (ex-data (first @last-errors)))))))

  (testing "Filter chain - structural equivalence"
    (let [dsl "(chain 
                 (scale 1920 1080)
                 (overlay))"
          foo (compile-dsl dsl)
          bar (ffmpeg/parse "scale=width=1920:height=1080,overlay")]
      (is (= foo bar))))

  (testing "nested chains - structural equivalence"
    (let [dsl "(chain 
                 (scale 1920 1080)
                 (overlay))
               (hflip)"
          foo (compile-dsl dsl)
          bar (ffmpeg/parse "scale=1920:1080,overlay;hflip")]
      (is (= foo bar))))

  (testing "coma in maps is insignificant"
    (let [m1 "{:input \"tmp\" :output \"right\"}"
          m2 "{:input \"tmp\", :output \"right\"}"]
      (is (= (dsl-parser m1) (dsl-parser m2))))))

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
        (is (= 2 (count body-expressions))))))
  (testing "Nested let bindings"
    (let [result (compile-dsl "(let [foo 2] (let [foo 4] (scale x foo)))")]
      (is (= "scale=width=x:height=4" (to-ffmpeg result))))))

(deftest test-programs
  (testing "let binding should return valid structures (filter, filterchain, filtergraph)"
    (let [result (compile-dsl "(let [x 1] x)")]
      (is (instance? FilterGraph result))
      (is (= :not-a-filtergraph (:error-type (ex-data (first @last-errors)))))))
  (testing "invalid parameters"
    (let [result (compile-dsl "(scale 1.23 456)")]
      (is (instance? FilterGraph result))
      (is (= :invalid-parameter (:error-type (ex-data (first @last-errors))))))))

(deftest let-bindings
  (testing "Mathematical functions from clojure.core"
    (is (= "scale=width=4:height=1080" (to-ffmpeg (compile-dsl "(let [width (mod 10 6)] (scale width 1080))"))))
    (is (= "scale=width=1920:height=1920" (to-ffmpeg (compile-dsl "(let [size (max 1920 1080)] (scale size size))"))))
    (is (= "scale=width=10:height=100" (to-ffmpeg (compile-dsl "(let [offset (abs -10)] (scale offset 100))"))))
    (is (= "scale=width=1920:height=1080" (to-ffmpeg (compile-dsl "(let [next (inc 1919)] (scale next 1080))"))))
    (is (= "scale=width=1920:height=1080" (to-ffmpeg (compile-dsl "(let [next inc] (scale {:width (next 1919) :height 1080}))")))))
  (testing "Nested expressions work"
    (is (= "scale=width=5:height=100" (to-ffmpeg (compile-dsl "(let [result (inc (mod 10 6))] (scale result 100))")))))
  (testing "Negative numbers work properly"
    (is (= "scale=width=10:height=100" (to-ffmpeg (compile-dsl "(let [offset (abs -10)] (scale offset 100))")))))
  (testing "Unknown functions still become filters"
    (let [result (compile-dsl "(nonexistent 123 456)")]
      (is (instance? FilterGraph result))
      (is (= :unresolved-function (:error-type (ex-data (first @last-errors))))))
    (testing "Known filters that are unimplemented return empty filtergraph"
      (let [result (compile-dsl "(find_rect 123 456)")]
        (is (instance? FilterGraph result))
        (is (= :not-implemented (:error-type (ex-data (first @last-errors)))))))))

(deftest instaparse-grammar
  (testing "grammar is not ambiguous"
    (is (= 1 (count (dsl-parses "6"))))
    (is (= 1 (count (dsl-parses "foo"))))
    (is (= 1 (count (dsl-parses "\"foo\""))))
    (is (= 1 (count (dsl-parses "6foo"))))
    (is (= 1 (count (dsl-parses "foo6"))))
    (is (= 1 (count (dsl-parses ":input"))))
    (is (= 1 (count (dsl-parses "-6"))))
    (is (= 1 (count (dsl-parses "-6.6"))))
    (is (= 1 (count (dsl-parses "[[in] foo [out]]"))))
    (is (= 1 (count (dsl-parses "[[v:0][v:1] foo [out]]"))))
    (is (= 1 (count (dsl-parses "{:input \"tmp\"}"))))
    (is (= 1 (count (dsl-parses "{:input \"tmp\",}"))))
    (is (= 1 (count (dsl-parses "{:input \"tmp\" :output \"right\"}"))))
    (is (= 1 (count (dsl-parses "{:input \"tmp\", :output \"right\"}"))))))

(deftest defgraph
  (testing "Parsing graph definitions is done for their side effects"
    (let [result (compile-dsl "(defgraph my-scale (scale 1920 1080))")]
      (is (instance? FilterGraph result))
      (is (nil? (seq (:chains result))))))
  (testing "Parsing a regular expression and a graph definition - only the regular expression is transformed and returned"
    (let [dsl "(defgraph my-crop (crop \"1920\" \"1080\"))\n(scale 1920 180)"]
      (is (= (compile-dsl dsl)
             (compile-dsl "(scale 1920 180)")))))
  (testing "If no regular expressions are present, return empty Filtergraph"
    (let [dsl "(defgraph my-scale (scale 1920 1080))"
          result (compile-dsl dsl)]
      (is (and (nil? (seq (.-chains result))) (instance? FilterGraph result)))))
  (testing "we can compose filtergraphs"
    (let [result (compile-dsl "(defgraph my-scale (scale 1920 1080))\n(defgraph my-crop (crop \"1920\" \"1080\"))\n(compose my-scale my-crop)")]
      (is (= 2 (count (:chains result))))
      (is (= "scale=width=1920:height=1080;crop=out_w=1920:w=1080" (to-ffmpeg result))))))

(deftest name-shadowing
  (testing "When we use the name of built-in function in a let binding, we shadow the built-in function so reject it"
    (testing "built-in reserved words"
      (let [dsl "(let [color red] (color {:c color}))"]
        (compile-dsl dsl)
        (is (= :reserved-word (:error-type (ex-data (first @last-errors)))))))
    (testing "built-in Clojure names"
      (let [dsl "(let [map red] (color {:c map}))"]
        (is (= "color=c=red" (to-ffmpeg (compile-dsl dsl))))))))

(deftest keywords
  (testing "Keywords are functions"
    (let [dsl "(let [data {:width 1920 :height 1080}](scale {:width (:width data) :height (:height data)}))"]
      (is (= "scale=width=1920:height=1080" (to-ffmpeg (compile-dsl dsl)))))))

(deftest ffmpeg-parsing
  (testing "Labels are preserved when parsing ffmpeg command"
    (let [foo (ffmpeg/parse "crop=iw/2:ih:0:0,split[left][tmp];[tmp]hflip[right];[left][right]hstack")
          bar (first (:filters (second (:chains foo))))]
      (is (= ["tmp"] (:input-labels bar)))
      (is (= ["right"] (:output-labels bar))))))

(deftest for-binding
  (testing "Structural tests"
    (testing "a positive integer range produces exactly N chains"
      (let [result (compile-dsl "(for [i (range 3)] (scale))")]
        (is (instance? FilterGraph result))
        (is (= 3 (count (:chains result))))))
    (testing "range of 0 produces a FilterGraph with no chains"
      (let [result (compile-dsl "(for [i (range 0)] (scale))")]
        (is (instance? FilterGraph result))
        (is (empty? (:chains result)))))
    (testing "(range 2 5) as range node gives 3 iterations"
      (let [result (compile-dsl "(for [i (range 2 5)] (scale))")]
        (is (instance? FilterGraph result))
        (is (= 3 (count (:chains result))))))
    (testing "3 iterations produces 3 chains"
      (let [result (compile-dsl "(for [i (range 3)] (chain (scale) (crop)))")]
        (is (instance? FilterGraph result))
        (is (= 3 (count (:chains result))))))
    (testing "loop variable is in scope for parameter expressions"
      (let [result (compile-dsl "(for [i (range 3)] (lagfun {:decay (/ (- 99.0 i) 100.0)}))")]
        (is (= "lagfun=decay=0.99;lagfun=decay=0.98;lagfun=decay=0.97" (to-ffmpeg result)))))
    (testing "loop variable is in scope for input/output label expressions"
      (let [result (compile-dsl "[[(for [i (range 2)] (str \"v\" i))] (scale) [(for [i (range 2)] (str \"out\" i))]]")
            filter (first (:filters (first (:chains result))))]
        (is (= (get-input-labels filter) ["v0" "v1"]))
        (is (= (get-output-labels filter) ["out0" "out1"]))))
    (testing "generated filtergraph serialises to expected ffmpeg syntax"
      (let [result (compile-dsl "(for [i (range 3)] [[(str \"v\" i)] (scale) [(str \"out\" i)]])")]
        (is (= "[v0]scale[out0];[v1]scale[out1];[v2]scale[out2]" (to-ffmpeg result)))))
    (testing "lagfun chains with varying decay render as a semicolon-separated string"
      (let [result (compile-dsl "(for [i (range 3)] [[(str \"i\" i)] (lagfun {:decay (/ (- 99.0 i) 100.0)}) [(str \"o\" i)]])")]
        (is (= (to-ffmpeg result) "[i0]lagfun=decay=0.99[o0];[i1]lagfun=decay=0.98[o1];[i2]lagfun=decay=0.97[o2]"))))
    (testing "compose-filtergraphs is associative so nesting has no semantic effect. Demonstrates the redundancy of the inner compose for single-body iterations."
      (testing "nested and flat compose produce identical chain sequences"
        (let [result (compile-dsl "(for [i (range 4)] [[(str \"v\" i)] (scale) [(str \"o\" i)]])")]
          (is (= 4 (count (:chains result))))
          (is (= "[v0]scale[o0];[v1]scale[o1];[v2]scale[o2];[v3]scale[o3]" (to-ffmpeg result))))))
    (testing "we can have if logic in labels"
      (let [result (compile-dsl "(for [i (range 0 4)] [[(if (= i 1) (str \"v\" i) (str \"in\" i))] (scale)])")]
        (is (= (to-ffmpeg result) "[in0]scale;[v1]scale;[in2]scale;[in3]scale")))
      (let [result (compile-dsl "(for [i (range 0 4)] [[(if (> i 1) (str \"v\" i) (str \"in\" i))](scale)])")]
        (is (= (to-ffmpeg result) "[in0]scale;[in1]scale;[v2]scale;[v3]scale")))
      (let [result (compile-dsl "(for [i (range 0 4)] [[(if (< i 1) (str \"v\" i) (str \"in\" i))](scale)])")]
        (is (= (to-ffmpeg result) "[v0]scale;[in1]scale;[in2]scale;[in3]scale")))
      (let [result (compile-dsl "(for [i (range 0 4)] [[(if (<= i 1) (str \"v\" i) (str \"in\" i))](scale)])")]
        (is (= (to-ffmpeg result) "[v0]scale;[v1]scale;[in2]scale;[in3]scale")))
      (let [result (compile-dsl "(for [i (range 0 4)] [[(if (zero? i) (str \"out\" i) (str \"t\" i))](scale)])")]
        (is (= (to-ffmpeg result) "[out0]scale;[t1]scale;[t2]scale;[t3]scale"))))
    (testing "for binding in label position")
    (let [result (compile-dsl "[[(for [i (range 3)] (str \"i\" i))] (scale)[\"out\"]]")]
      (is (= (to-ffmpeg result) "[i0][i1][i2]scale[out]")))
    (testing "interleaving values in label position requires alias due to ffmpeg filter bearing same name")
    (let [result (compile-dsl "[[(interleave_ (for [i (range 3)] (str \"foo\" i)) (for [i (range 3)] (str \"bar\" i)))] (hue) [\"out\"]]")]
      (is (= (to-ffmpeg result) "[foo0][bar0][foo1][bar1][foo2][bar2]hue[out]")))))

(deftest defgraph
  (testing "Parsing graph definitions is done for their side effects"
    (let [result (compile-dsl "(defgraph my-scale (scale 1920 1080))")]
      (is (instance? FilterGraph result))
      (is (nil? (seq (:chains result))))))
  (testing "Parsing a regular expression and a graph definition - only the regular expression is transformed and returned"
    (let [dsl "(defgraph my-crop (crop \"1920\" \"1080\"))\n(scale 1920 180)"]
      (is (= (compile-dsl dsl)
             (compile-dsl "(scale 1920 180)")))))
  (testing "If no regular expressions are present, return empty Filtergraph"
    (let [dsl "(defgraph my-scale (scale 1920 1080))"
          result (compile-dsl dsl)]
      (is (and (nil? (seq (.-chains result))) (instance? FilterGraph result)))))
  (testing "registering a graph under an already-registered name redefines it"
    (let [result (compile-dsl "(defgraph dupe-graph (scale 1920 1080))\n(defgraph dupe-graph (crop \"640\" \"480\"))\ndupe-graph")]
      (is (= "crop" (:name (first (:filters (first (:chains result))))))))
    (testing "a graph can refer to a previously defined graph"
      (let [result (compile-dsl "(defgraph foo (scale 1920 1080))\n(defgraph bar (compose foo (crop \"640\" \"480\")))\nbar")]
        (is (= "scale=width=1920:height=1080;crop=out_w=640:w=480" (to-ffmpeg result)))))))

