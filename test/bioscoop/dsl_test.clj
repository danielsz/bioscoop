(ns bioscoop.dsl-test
  (:require [bioscoop.dsl :refer [compile-dsl dsl-parser dsl-parses]]
            [bioscoop.render :refer [to-ffmpeg]]
            [bioscoop.ffmpeg-parser :as ffmpeg]
            [bioscoop.built-in]
            [bioscoop.registry :refer [get-graph clear-registry!]]
            [clojure.test :refer [testing deftest is use-fixtures]]
            [instaparse.core :as insta])
  (:import [bioscoop.domain.records FilterGraph]))

(defn once-fixture [f]
  (f)
  (clear-registry!))

(use-fixtures :once once-fixture)

(deftest test-dsl-compilation
  (testing "Basic filter creation"
    (let [result (compile-dsl "(scale 1920 1080)")]
      (is (= "scale=width=1920:height=1080" (to-ffmpeg result)))))

  (testing "Basic named filter creation"
    (let [result (compile-dsl "(scale 1920 1080)")]
      (is (= "scale=width=1920:height=1080" (to-ffmpeg result)))))

  (testing "Filter with labels"
    (let [dsl "(let [input-vid (input-labels \"in\")
                     scaled (scale 1920 1080 input-vid (output-labels \"scaled\"))]
                 scaled)"
          result (compile-dsl dsl)]
      (is (= "[in]scale=width=1920:height=1080[scaled]" (to-ffmpeg result))))
    (let [dsl "(scale 1920 1080 {:input \"in\"} {:output \"scaled\"})"
          result (compile-dsl dsl)]
      (is (= "[in]scale=width=1920:height=1080[scaled]" (to-ffmpeg result)))))

  (testing "Labels are preserved when parsing ffmpeg command"
    (let [foo (ffmpeg/parse "crop=iw/2:ih:0:0,split[left][tmp];[tmp]hflip[right];[left][right]hstack")
          bar (meta (first (:filters (second  (:chains foo)))))]
      (is (= ["tmp"] (:input-labels bar)))
      (is (= ["right"] (:output-labels bar)))))

  (testing "Multiple expressions, implicit filterchain"
    (let [dsl "(scale 1920 1080)
               (overlay)"
          result (compile-dsl dsl)]
      (is (= "scale=width=1920:height=1080;overlay" (to-ffmpeg result)))))

  (testing "Multiple expressions, one is invalid"
    (let [dsl "(scale 1920 1080)
               (overlay) 1"
          result (compile-dsl dsl)]
      (is (= :bad-apple (:error-type result)))))
  
  (testing "Filter chain"
    (let [dsl "(chain 
                 (scale 1920 1080)
                 (overlay))"
          result (compile-dsl dsl)]
      (is (= "scale=width=1920:height=1080,overlay" (to-ffmpeg result)))))

  (testing "Filter chain - structural equivalence"
    (let [dsl "(chain 
                 (scale 1920 1080)
                 (overlay))"
          foo (compile-dsl dsl)
          bar (ffmpeg/parse "scale=width=1920:height=1080,overlay")]
      (is (= foo bar))))

  (testing "nested chains"
    (let [dsl "(chain 
                 (scale \"1920\" \"1080\")
                 (overlay))
               (hflip)"
          result (compile-dsl dsl)]
      (is (= "scale=width=1920:height=1080,overlay;hflip" (to-ffmpeg result)))))
  
 (testing "nested chains - structural equivalence"
    (let [dsl "(chain 
                 (scale 1920 1080)
                 (overlay))
               (hflip)"
          foo (compile-dsl dsl)
          bar (ffmpeg/parse "scale=1920:1080,overlay;hflip")]
      (is (=  foo bar))))
  
  (testing "Parent scope access and nesting"
    (let [dsl "(let [height 1920]
                 (let [width 1080]
                   (scale height width)))"
          result (compile-dsl dsl)]
      (is (= "scale=width=1920:height=1080" (to-ffmpeg result))))
    (let [dsl "(let [width 1920]
                 (let [width 1280]
                   (scale 1080 width)))"
          result (compile-dsl dsl)]
      (is (= "scale=width=1080:height=1280" (to-ffmpeg result)))))
  (let [dsl "(let [width 1920]
                 (let [width 1280]
                   (let [width 800]
                     (scale 1080 width))))"
          result (compile-dsl dsl)]
      (is (= "scale=width=1080:height=800" (to-ffmpeg result))))

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
        (is (= 2 (count body-expressions)))))))

(deftest test-programs
  (testing "let binding should return valid structures (filter, filterchain, filtergraph)"
    (is (= :not-a-filtergraph (:error-type (compile-dsl "(let [x 1] x)")))))
  (testing "invalid parameters"
    (is (= :invalid-parameter (:error-type (compile-dsl "(scale 1.23 456)"))))))

(deftest let-bindings
  (testing "Mathematical functions from clojure.core"
    (is (= "scale=width=4:height=1080" (to-ffmpeg (compile-dsl "(let [width (mod 10 6)] (scale width 1080))"))))
    (is (= "scale=width=1920:height=1920" (to-ffmpeg (compile-dsl "(let [size (max 1920 1080)] (scale size size))"))))
    (is (= "scale=width=10:height=100" (to-ffmpeg (compile-dsl "(let [offset (abs -10)] (scale offset 100))"))))
    (is (= "scale=width=1920:height=1080" (to-ffmpeg (compile-dsl "(let [next (inc 1919)] (scale next 1080))")))))
  (testing "Nested expressions work"
    (is (= "scale=width=5:height=100" (to-ffmpeg (compile-dsl "(let [result (inc (mod 10 6))] (scale result 100))")))))
  (testing "Negative numbers work properly"
    (is (= "scale=width=10:height=100" (to-ffmpeg (compile-dsl "(let [offset (abs -10)] (scale offset 100))")))))
  (testing "Unknown functions still become filters"
    (is (= :unresolved-function (:error-type (compile-dsl "(nonexistent 123 456)"))))))

(deftest real-world
  (testing "flip"
    (let [dsl "(let [out-left-tmp (output-labels \"left\" \"tmp\")
                     in-tmp (input-labels \"tmp\")
                     out-right (output-labels \"right\")
                     in-left-right (input-labels \"left\" \"right\")]
                 (graph (chain
                            (crop \"iw/2\" \"ih\" \"0\" \"0\")
                            (split  out-left-tmp))
                         (hflip in-tmp out-right)
                         (hstack in-left-right)))"]
      (is (= "crop=out_w=iw/2:w=ih:out_h=0:h=0,split[left][tmp];[tmp]hflip[right];[left][right]hstack"
             (to-ffmpeg (compile-dsl dsl))))))
  (testing "flip inline labels"
    (let [dsl "(graph
                  (chain
                     (crop \"iw/2\" \"ih\" \"0\" \"0\")
                     (split {:output \"left\"} {:output \"tmp\"}))
                  (hflip {:input \"tmp\"} {:output \"right\"})
                  (hstack {:input \"left\"} {:input \"right\"}))"]
      (is (= "crop=out_w=iw/2:w=ih:out_h=0:h=0,split[left][tmp];[tmp]hflip[right];[left][right]hstack"
             (to-ffmpeg (compile-dsl dsl))))))
  (testing "args as maps"
    (let [dsl "(color {:color \"blue\" :size \"1920x1080\" :rate 24 :duration \"10\" :sar \"16/9\"})"]
      (is "color=color=blue:size=1920x1080:rate=24:duration=10:sar=16/9" (to-ffmpeg (compile-dsl dsl))))))


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
    (let [dsl "(defgraph my-scale (scale 1920 1080))"]
      (compile-dsl dsl)
      (is (instance? FilterGraph (get-graph 'my-scale)))))
  (testing "Parsing a regular expression and a graph definition - only the regular expression is transformed and returned"
    (let [dsl "(defgraph my-crop (crop \"1920\" \"1080\"))\n(scale 1920 180)"]
      (is (= (compile-dsl dsl)
             (compile-dsl "(scale 1920 180)")))))
  (testing "If no regular expressions are present, return empty Filtergraph"
    (let [dsl "(defgraph my-scale (scale 1920 1080))"
          result (compile-dsl dsl)]
      (is (and (nil? (seq (.-chains result))) (instance? FilterGraph result)))))
  (testing "we can compose filtergraphs"
    (do (compile-dsl "(defgraph my-scale (scale 1920 1080))")
        (compile-dsl "(defgraph my-crop (crop \"1920\" \"1080\"))")
        (let [result (compile-dsl "(compose my-scale my-crop)")]
          (is (= 2 (count (.-chains result))))))))

(deftest name-shadowing
  (testing "When we use a the name of built-in function in a let binding, we shadow the built-in function so reject it"
    (testing "built-in reserved words"
      (let [dsl "(let [color red] (color {:c color}))"]
        (is (= :reserved-word (:error-type  (compile-dsl dsl))))))
    (testing "built-in Clojure names"
      (let [dsl "(let [map red] (color {:c map}))"]
        (is (= "color=c=red" (to-ffmpeg (compile-dsl dsl))))))))

(deftest padded-graph
  (testing "single filter"
    (let [dsl1 (compile-dsl "(defgraph my-crop (crop \"220\"))")
          dsl2 (compile-dsl "[[in][off] my-crop [out]]")]
      (is (= "[in][off]crop=out_w=220[out]" (to-ffmpeg dsl2)))))
  (testing "filterchain with two filters"
    (let [_ (compile-dsl "(defgraph my-scale (chain (scale 1920 1080) (crop \"220\")))")
          dsl2 (compile-dsl "[[in][off] my-scale [out]]")]
      (is (= "[in][off]scale=width=1920:height=1080,crop=out_w=220[out]" (to-ffmpeg dsl2)))))
  (testing "inline filterchain"
    (let [dsl (compile-dsl "[[in][off] (chain (scale 1920 1080) (crop \"220\")) [out]]")]
      (is (= "[in][off]scale=width=1920:height=1080,crop=out_w=220[out]" (to-ffmpeg dsl)))))
  (testing "composing"
    (let [dsl (compile-dsl "(compose [[0] (chain (scale 133 220)) [1]] [[0] (crop \"111\") [1]])")]
      (is (= "[0]scale=width=133:height=220[1];[0]crop=out_w=111[1]" (to-ffmpeg dsl))))))


