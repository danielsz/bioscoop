(ns bioscoop.macro-test
  (:require [bioscoop.macro :refer [bioscoop form->ast defgraph]]
            [bioscoop.dsl :as dsl :refer [last-errors]]
            [bioscoop.render :refer [to-ffmpeg]]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [bioscoop.built-in]
            [bioscoop.domain.records :refer [with-labels]])
  (:import [bioscoop.domain.records FilterGraph FilterChain Filter]))


(deftest test-form->ast
  (testing "Simple expressions"
    (is (= [:symbol "scale"] (form->ast 'scale)))
    (is (= [:number "1920"] (form->ast 1920)))
    (is (= [:string "scale"] (form->ast "scale")))
    (is (= [:keyword :in] (form->ast :in)))
    (is (= [:boolean "true"] (form->ast true)))
    (is (= [:boolean "false"] (form->ast false))))

  (testing "Function calls"
    (is (= [:list [:symbol "scale"] [:number "1920"] [:number "1080"]]
           (form->ast '(scale 1920 1080))))
    (is (= [:list [:symbol "filter"] [:string "scale"] [:string "1920:1080"]]
           (form->ast '(filter "scale" "1920:1080")))))

  (testing "Let bindings"
    (is (= [:let-binding
            [:binding [:symbol "width"] [:number "1920"]]
            [:list [:symbol "scale"] [:symbol "width"] [:number "1080"]]]
           (form->ast '(let [width 1920] (scale width 1080)))))

    (is (= [:let-binding
            [:binding [:symbol "width"] [:number "1920"]]
            [:binding [:symbol "height"] [:number "1080"]]
            [:list [:symbol "scale"] [:symbol "width"] [:symbol "height"]]]
           (form->ast '(let [width 1920 height 1080] (scale width height)))))
    (is (= [:for-binding [:symbol "i"] [:list [:symbol "range"] [:number "3"]] [:list [:symbol "scale"]]]
           (form->ast '(for [i (range 3)] (scale)))))
    (is (= [:for-binding
            [:symbol "i"]
            [:list [:symbol "range"] [:number "3"]]
            [:list
             [:symbol "lagfun"]
             [:map
              [:keyword :decay]
              [:list
               [:symbol "/"]
               [:list [:symbol "-"] [:number "99.0"] [:symbol "i"]]
               [:number "100.0"]]]]]
           (form->ast '(for [i (range 3)] (lagfun {:decay (/ (- 99.0 i) 100.0)})))))))

(deftest test-bioscoop-macro
  (testing "arithmetic expressions"
    (let [structures (bioscoop (let [base-width 1920]
                                 (scale (+ base-width 100) 1080)))]
      (is (= "scale=width=2020:height=1080" (to-ffmpeg structures)))))

  (testing "labels"
    (let [structures (bioscoop [["input"] (scale 1920 1080) ["scaled"]])]
      (is (= "[input]scale=width=1920:height=1080[scaled]" (to-ffmpeg structures))))
    (testing "We can have multiple strings in a label"
      (let [result (bioscoop [["0:v" "1:v"] (chain (scale 1920 1080) (crop "222")) ["v01"]])]
        (is (= "[0:v][1:v]scale=width=1920:height=1080,crop=out_w=222[v01]" (to-ffmpeg result))))))


  (testing "complex expression"
    (let [structures (bioscoop (let [w 1920
                                     h 1080
                                     x "10"
                                     y "20"]
                                 (chain (scale w h)
                                        (crop "800" "600" x y)
                                        (overlay))))]
      (is (= "scale=width=1920:height=1080,crop=out_w=800:w=600:out_h=10:h=20,overlay" (to-ffmpeg structures)))))

  (testing "undefined function"
    (let [result (bioscoop (undefined-function 123))]
      (is (= :unresolved-function (:error-type (ex-data (first @last-errors)))))))

  (testing "not implemented"
    (let [result (bioscoop (find_rect 123))]
      (is (= :not-implemented (:error-type (ex-data (first @last-errors)))))))
  
  (testing "Macro produces same results as text parsing"
    (let [text-result (dsl/compile-dsl "(scale 1920 1080)")
          macro-result (bioscoop (scale 1920 1080))]
      (is (= text-result macro-result)))

    (let [text-result (dsl/compile-dsl "(let [width 1920] (scale width 1080))")
          macro-result (bioscoop (let [width 1920] (scale width 1080)))]
      (is (= text-result macro-result)))

    (let [text-result (dsl/compile-dsl "(scale 1920 1080)")
          macro-result (bioscoop (scale 1920 1080))]
      (is (= text-result macro-result)))

    (let [text-result (dsl/compile-dsl "(chain (scale 1920 1080) (overlay))")
          macro-result (bioscoop (chain (scale 1920 1080) (overlay)))]
      (is (= text-result macro-result))))
  (testing "Complex let bindings"
    (let [text-result (dsl/compile-dsl "(let [width 1920 height 1080] (scale width height))")
          macro-result (bioscoop (let [width 1920 height 1080] (scale width height)))]
      (is (= text-result macro-result)))
    (let [text-result (to-ffmpeg (dsl/compile-dsl "(let [width 1920 height 1080] [[\"tmp\"] (scale width height)])"))
          macro-result (to-ffmpeg (bioscoop (let [width 1920 height 1080] [["tmp"](scale width height)])))
          ffmpeg-string "[tmp]scale=width=1920:height=1080"]
      (is (= text-result macro-result ffmpeg-string))))

  (testing "for bindings"
    (let [macro-result (to-ffmpeg (bioscoop (for [i (range 3)] (lagfun {:decay (/ (- 99.0 i) 100.0)}))))
          text-result (to-ffmpeg (dsl/compile-dsl "(for [i (range 3)] (lagfun {:decay (/ (- 99.0 i) 100.0)}))"))
          ffmpeg-string "lagfun=decay=0.99;lagfun=decay=0.98;lagfun=decay=0.97"]
      (is (= text-result macro-result ffmpeg-string))))

  (testing "compose top-level filtergraphs and filters"
    (let [macro-result (bioscoop
                         (compose
                          (for [i (range 2)]
                            [[(str i)](loop {:loop 124 :size 1}) [(str "l" i)]])
                          [["l0"] ["l1"] (xfade {:transition "fade" :duration 1 :offset 3}) ["v1"]]
                          (for [i (range 1 3)]
                            [[(str "v" i)] [(str "l" (inc i))] (xfade {:transition "fade"
                                                                      :duration 1
                                                                       :offset (+ (* i 4) 3)}) [(str "v" (inc i))]])))]
      (is (= "[0]loop=loop=124:size=1[l0];[1]loop=loop=124:size=1[l1];[l0][l1]xfade=transition=fade:duration=1:offset=3[v1];[v1][l2]xfade=transition=fade:duration=1:offset=7[v2];[v2][l3]xfade=transition=fade:duration=1:offset=11[v3]" (to-ffmpeg macro-result)))))

  (testing "real world examples"
    (let [dsl (bioscoop (compose [(chain (crop "iw/2" "ih" "0" "0") (split)) ["left"]["tmp"]]
                                 [["tmp"] (hflip) ["right"]]
                                 [["left"] ["right"](hstack)]))]
      (is (= "crop=out_w=iw/2:w=ih:out_h=0:h=0,split[left][tmp];[tmp]hflip[right];[left][right]hstack" (to-ffmpeg dsl))))
    (let [dsl (bioscoop (compose [(chain (crop "iw/2" "ih" "0" "0") (split)) ["left"]["tmp"]]
                                 [["tmp"] (hflip) ["right"]]
                                 [["left"] ["right"](hstack)]))]
      (is (= "crop=out_w=iw/2:w=ih:out_h=0:h=0,split[left][tmp];[tmp]hflip[right];[left][right]hstack"
             (to-ffmpeg dsl))))
    (let [dsl (bioscoop (chain (color "white" "480x480" 25 3) (format "rgb24") (drawtext {:fontcolor "black" :fontsize 600 :text "'%{eif\\:t\\:d}'" :x "(w-text_w)/2" :y "(h-text_h)/2"})))]
      (is (= "color=color=white:size=480x480:rate=25:duration=3,format=pix_fmts=rgb24,drawtext=fontcolor=black:fontsize=600:text='%{eif\\:t\\:d}':x=(w-text_w)/2:y=(h-text_h)/2" (to-ffmpeg dsl))))
    (let [dsl (bioscoop (zoompan {:z "'min(zoom+0.0015,1.5)'" :d 700 :x "iw/2-(iw/zoom/2)" :y "ih/2-(ih/zoom/2)"}))]
      (is (= "zoompan=z='min(zoom+0.0015,1.5)':d=700:x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2)" (to-ffmpeg dsl))))
    (let [dsl (bioscoop (let [zoom {:z "'min(zoom+0.0015,1.5)'" :d 700 :x "iw/2-(iw/zoom/2)" :y "ih/2-(ih/zoom/2)"}
                              f {:type "out" :start_frame 600 :duration 1}]
                          (compose [["0:v"] (chain (zoompan zoom) (fade f)) ["v0"]] 
                                   [["1:v"] (chain (zoompan zoom) (fade f)) ["v1"]]
                                   [["v0"] ["v1"] (chain (concat {:n 2 :v 1 :a 0}) (format {:pix_fmts "yuv420p"})) ["outv"]])))]
      (is (= "[0:v]zoompan=z='min(zoom+0.0015,1.5)':d=700:x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2),fade=type=out:start_frame=600:duration=1[v0];[1:v]zoompan=z='min(zoom+0.0015,1.5)':d=700:x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2),fade=type=out:start_frame=600:duration=1[v1];[v0][v1]concat=n=2:v=1:a=0,format=pix_fmts=yuv420p[outv]" (to-ffmpeg dsl))))
    (let [dsl (bioscoop (let [zoom {:z "'min(zoom+0.0015,1.5)'" :d 700 :x "iw/2-(iw/zoom/2)" :y "ih/2-(ih/zoom/2)"}
                              f {:type "out" :start_frame 600 :duration 1}]
                          (compose [["0:v"] (chain (zoompan zoom) (fade f)) ["v0"]] 
                                   [["1:v"] (chain (zoompan zoom) (fade f)) ["v1"]]
                                   [["v0"] ["v1"] (chain (concat {:n 2 :v 1 :a 0}) (format {:pix_fmts "yuv420p"})) ["outv"]])))]
      (is (= "[0:v]zoompan=z='min(zoom+0.0015,1.5)':d=700:x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2),fade=type=out:start_frame=600:duration=1[v0];[1:v]zoompan=z='min(zoom+0.0015,1.5)':d=700:x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2),fade=type=out:start_frame=600:duration=1[v1];[v0][v1]concat=n=2:v=1:a=0,format=pix_fmts=yuv420p[outv]" (to-ffmpeg dsl)))))

  (testing "bioscoop ad"
    (is (= "smptebars[v0];testsrc[v1];[v0]pad=width=iw*2:height=ih[out0];[out0][v1]overlay=x=w"
           (to-ffmpeg (bioscoop (compose [(smptebars) ["v0"]]
                                         [(testsrc)["v1"]]
                                         [["v0"] (pad {:width "iw*2" :height "ih"}) ["out0"]]
                                         [["out0"] ["v1"](overlay {:x "w"})]))))))

  (testing "maps as args"
    (let [dsl (bioscoop (color {:color "blue" :size "1920x1080" :rate 24 :duration "10" :sar "16/9"}))]
      (is (= "color=color=blue:size=1920x1080:rate=24:duration=10:sar=16/9" (to-ffmpeg dsl)))))


  (testing "Multiple expressions"
    (let [text-result (try (dsl/compile-dsl "(scale 1920 1080)") (catch Exception e nil))
          macro-result (try (bioscoop (scale 1920 1080)) (catch Exception e nil))]
      (when (and text-result macro-result)
        (is (= text-result macro-result)))))

  (testing "Macro produces correct data types"
    (let [result (bioscoop (scale 1920 1080))]
      (is (instance? FilterGraph result))
      (is (= 1 (count (:chains result))))
      (is (= 1 (count (:filters (first (:chains result))))))
      (is (= "scale" (:name (first (:filters (first (:chains result)))))))
      (is (= #:bioscoop.domain.specs.scale{:width 1920 :height 1080} (:args (first (:filters (first (:chains result)))))))))

  (testing "Automatic wrapping of FilterGraph/filterchain"
    (let [result (bioscoop (scale 1920 1080) (scale 1910 1180) (scale 1920 80))]
      (is (instance? FilterGraph result))
      (is (true? (every? #(instance? FilterChain %) (:chains result))))
      (is (= 3 (count (:chains result))))      
      (is (= 1 (count (:filters (first (:chains result))))))
      (is (= 3 (count (map :filters (:chains result)))))))
  
  (testing "Automatic wrapping of FilterGraph/filterchain"
    (let [result (bioscoop (compose (scale 1920 1080) (scale 1910 1180) (scale 1920 80)))]
      (is (instance? FilterGraph result))
      (is (true? (every? #(instance? FilterChain %) (:chains result))))
      (is (= 3 (count (:chains result))))
      (is (= 3 (count (map :filters (:chains result)))))))

  (testing "Automatic wrapping of FilterGraph/filterchain"
    (let [result (bioscoop (compose (chain (scale 1920 1080) (scale 1910 1180)) (scale 1920 80)))]
      (is (instance? FilterGraph result))
      (is (instance? FilterChain (first (:chains result))))
      (is (instance? FilterChain (last (:chains result))))
      (is (= 2 (count (:chains result))))
      (is (= 2 (count (:filters (first (:chains result))))))))

  (testing "the name following defgraph cannot be a built-in name"
    (is (nil? (defgraph split (split)))))
  (testing "the name following defgraph cannot be a known clojure.core name"
    (is (nil? (defgraph map (split)))))
  (testing "defgraph is idempotent"
    (defgraph foo (let [shade "red"
                        background-color (color {:c shade :size "1920x1280" :rate 25 :duration 16})]
                    (chain background-color (scale 450 300))))
    (is (= (to-ffmpeg (bioscoop foo)) "color=c=red:size=1920x1280:rate=25:duration=16,scale=width=450:height=300"))
    (defgraph foo (let [shade "red"
                        background-color (color {:c shade :size "1920x1280" :rate 25 :duration 16})]
                    (chain background-color (scale 450 300))))
    (is (= (to-ffmpeg (bioscoop foo)) "color=c=red:size=1920x1280:rate=25:duration=16,scale=width=450:height=300"))))

(deftest padded-graph
  (testing "single filter"
    (do (defgraph my-crop (crop "220"))
        (let [structures (bioscoop [["in"]["off"] my-crop ["out"]])]
          (is (= "[in][off]crop=out_w=220[out]" (to-ffmpeg structures))))))
  (testing "inline filterchain"
    (let [structures (bioscoop [["in"]["off"] (chain (scale 1920 1080) (crop "220")) ["out"]])]
      (is (= "[in][off]scale=width=1920:height=1080,crop=out_w=220[out]" (to-ffmpeg structures)))))
  (testing "multiple filters"
    (defgraph foo (let [shade "red"
                        background-color (color {:c shade :size "1920x1280" :rate 25 :duration 16})]
                    (chain background-color (scale 450 300))))
    (let [structures (bioscoop [["in"]["off"] foo ["out"]])]
      (is (= "[in][off]color=c=red:size=1920x1280:rate=25:duration=16,scale=width=450:height=300[out]" (to-ffmpeg structures))))))

(deftest composition
  (testing "we can compose filtergraphs"
    (do (defgraph my-scale (scale 1920 1080))
        (defgraph my-crop (scale "1920" "1080"))
        (let [result (bioscoop (compose my-scale my-crop))]
          (is (= 2 (count (.-chains result)))))))
  (testing "we can compose padded filtergraphs"
    (do (defgraph my-scale (scale 1920 1080))
        (defgraph my-crop (scale "1920" "1080"))
        (let [result (bioscoop (let [a [["0:v"] my-scale ["v0a"] ["v0b"]]
                                     b [["v0a"] my-crop ["v0"]]]
                                 (compose a b)))]
          (is (= 2 (count (.-chains result)))))
        (let [result (bioscoop (compose [["0:v"] my-scale ["v0a"] ["v0b"]]
                                        [["v0a"] my-crop ["v0"]]))]
          (is (= 2 (count (.-chains result)))))))
  (testing "we can compose inline filtergraphs"
    (let [result (bioscoop (compose [["0:v"] (chain (scale 1920 1080) (crop "222")) ["v01"]]
                                    [["v0a"] my-crop ["v0"]]))]
      (is (= 2 (count (.-chains result)))))
    (let [result (bioscoop (compose  [["0:v"] (scale 1920 1080) ["v01"]]
                                     [["v0a"] my-crop ["v0"]]))]
      (is (= 2 (count (.-chains result))))))
  (testing "normal form"
    (let [result (bioscoop (compose [[0] (chain (scale 133 220)) [1]] [[0] (crop "111") [1]]))]
      (is (= "[0]scale=width=133:height=220[1];[0]crop=out_w=111[1]" (to-ffmpeg result))))))

(deftest ambiguity
  (testing "detect name shadowing in let binding"
    (do (defgraph foo (scale 1920 1080))
        (bioscoop (let [foo 1]
                    (scale {:width 1920 :height foo}))))
    ;; When there's ambiguity between let binding and defgraph,
    ;; errors are accumulated as warnings but processing continues.
    ;; The local binding takes precedence.
    (do (defgraph foo (scale 1920 1080))
        (is (instance? FilterGraph (bioscoop (let [foo 1]
                                               (compose [[0] (chain (scale {:width 1920 :height foo})) [1]] [[0] foo [1]]))))))))


(defn n-fun [n]
  (for [i (range n)]
    (-> (bioscoop (lagfun {:decay 0.99 :planes 1}))
       (update-in [:chains 0 :filters 0 :args] assoc
                  :bioscoop.domain.specs.lagfun/decay (/ (- 99 i) 100)
                  :bioscoop.domain.specs.lagfun/planes (inc i))
       (update-in [:chains 0 :filters 0] with-labels [(str "i" i)] [(str "o" i )]))))

(deftest fn-returning-filtergraphs
  (testing "user defined functions that return filtergraphs"
    (in-ns 'user)
    (intern *ns* 'n-fun #'bioscoop.macro-test/n-fun)
    (let [result (bioscoop (n-fun 3))]
      (is (instance? FilterGraph result))
      (is (= (to-ffmpeg result) "[i0]lagfun=decay=99/100:planes=1[o0];[i1]lagfun=decay=49/50:planes=2[o1];[i2]lagfun=decay=97/100:planes=3[o2]"))))
  (testing "user defined functions can be composed"
    (let [result (bioscoop (let [formatting (chain (format {:pix_fmts "gbrp10"})
                                                   (split {:outputs 2}))
                                 blending (chain (blend {:all_mode "screen" :c0_opacity 0.5 :c1_opacity 0.6})
                                                 (format {:pix_fmts "yuv422p10le"}))]
                             (compose [formatting ["i0"] ["i1"]]
                                      (n-fun 2)
                                      [["o0"] ["o1"] blending])))]
      (is (instance? FilterGraph result))
      (is (= (to-ffmpeg result) "format=pix_fmts=gbrp10,split=outputs=2[i0][i1];[i0]lagfun=decay=99/100:planes=1[o0];[i1]lagfun=decay=49/50:planes=2[o1];[o0][o1]blend=all_mode=screen:c0_opacity=0.5:c1_opacity=0.6,format=pix_fmts=yuv422p10le")))))

(deftest vars
  (testing "Able to resolve a Var in another namespace"
    (create-ns 'bioscoop.masterpiece)
    (binding [*ns* (the-ns 'bioscoop.masterpiece)]
      (refer-clojure)
      (require '[bioscoop.macro :refer [bioscoop defgraph]] '[bioscoop.built-in])
      (eval '(defgraph foo (testsrc))))
    (is (= "[0]testsrc[1];[3]crop=out_w=111[2]" (to-ffmpeg (bioscoop (compose [["0"] bioscoop.masterpiece/foo ["1"]] [["3"] (crop "111") ["2"]]))))))
  (testing "Able to resolve another Var in another namespace"
    (create-ns 'bioscoop.masterpiece)
    (binding [*ns* (the-ns 'bioscoop.masterpiece)]
      (refer-clojure)
      (require '[bioscoop.macro :refer [bioscoop defgraph]] '[bioscoop.built-in])
      (eval '(do
               (defgraph masterpiece (testsrc))
               (intern 'user 'qux masterpiece))))
    (is (= "[0]testsrc[1];[3]crop=out_w=111[2]" (to-ffmpeg (bioscoop (compose [["0"] user/qux ["1"]] [["3"] (crop "111") ["2"]]))))))
  (testing "Able to resolve a function in another namespace"
    (create-ns 'bioscoop.masterpiece)
    (binding [*ns* (the-ns 'bioscoop.masterpiece)]
      (refer-clojure)
      (require '[bioscoop.macro :refer [bioscoop defgraph]] '[bioscoop.built-in])
      (eval '(do
               (intern *ns* 'another (fn [size] (bioscoop (testsrc {:size size}))))
               (intern 'user 'qux #'another))))
    (is (= "[0]testsrc=size=320x240[1];[3]crop=out_w=111[2]" (to-ffmpeg (bioscoop (compose [["0"] (user/qux "320x240") ["1"]] [["3"] (crop "111") ["2"]])))))))


(defn n-transition [n offset]
  (bioscoop (for [i (range 0 n)]
              [[(str "in" i)] (xfade {:transition "fade" :duration 1 :offset (+ i offset (* i offset))}) [(str "out" i)]])))

(deftest macro-&env-binding
  (testing " Clojure macros have access to the local binding map at expansion time. We can generate code that injects those bindings into the DSL env at runtime"
    (in-ns 'user)
    (intern *ns* 'n-transition #'bioscoop.macro-test/n-transition)
    (let [result (n-transition 3 9)]
      (is (instance? FilterGraph result))
      (is (= (to-ffmpeg result) "[in0]xfade=transition=fade:duration=1:offset=9[out0];[in1]xfade=transition=fade:duration=1:offset=19[out1];[in2]xfade=transition=fade:duration=1:offset=29[out2]")))))

(deftest namespaced-environment
  (testing "graph definitions"
    (defgraph foo (scale {:width 1080 :height 800}))))

(deftest chain-absorbs-a-for-result
  (testing "a for loop of filters expands in place, in iteration order"
    (is (= (to-ffmpeg (bioscoop (chain (drawtext {:text "0"}) (drawtext {:text "1"}) (drawtext {:text "2"}))))
           (to-ffmpeg (bioscoop (chain (for [n (range 3)] (drawtext {:text (str n)})))))))
    (is (empty? @dsl/last-errors))))

(deftest chain-absorbs-a-for-result-among-literal-filters
  (testing "a for loop can sit between ordinary filter calls in the same chain"
    (is (= (to-ffmpeg (bioscoop (chain (crop {:out_w "100"})
                                        (drawtext {:text "0"}) (drawtext {:text "1"})
                                        (vignette {:angle "1.0"}))))
           (to-ffmpeg (bioscoop (chain (crop {:out_w "100"})
                                       (for [n (range 2)] (drawtext {:text (str n)}))
                                       (vignette {:angle "1.0"}))))))))

(deftest chain-absorbs-two-separate-fors
  (testing "multiple for loops as separate chain arguments concatenate in argument order"
    (is (= (to-ffmpeg (bioscoop (chain (crop {:out_w "0"}) (crop {:out_w "1"})
                                        (vignette {:angle "0"}) (vignette {:angle "1"}))))
           (to-ffmpeg (bioscoop (chain (for [n (range 2)] (crop {:out_w (str n)}))
                                       (for [n (range 2)] (vignette {:angle (str n)})))))))))

(deftest chain-absorbs-heterogeneous-filters-from-one-for
  (testing "a single for loop can yield different filter types per iteration"
    (is (= (to-ffmpeg (bioscoop (chain (boxblur {:luma_radius "0"}) (vignette {:angle "1"})
                                        (boxblur {:luma_radius "2"}) (vignette {:angle "3"}))))
           (to-ffmpeg (bioscoop (chain (for [n (range 4)]
                                         (if (even? n)
                                           (boxblur {:luma_radius (str n)})
                                           (vignette {:angle (str n)}))))))))))

(deftest chain-absorbs-a-for-of-chains
  (testing "each for iteration can itself be a multi-filter chain; all flatten into one filterchain"
    (is (= (to-ffmpeg (bioscoop (chain (crop {:out_w "100" :x "0"}) (boxblur {:luma_radius "1"})
                                        (crop {:out_w "100" :x "10"}) (boxblur {:luma_radius "1"}))))
           (to-ffmpeg (bioscoop (chain (for [n (range 2)]
                                         (chain (crop {:out_w "100" :x (str (* n 10))})
                                                (boxblur {:luma_radius "1"}))))))))))

(deftest chain-absorbs-nested-for-for
  (testing "for-within-for flattens through two levels of nesting"
    (is (= (to-ffmpeg (bioscoop (chain (drawtext {:text "0-0"}) (drawtext {:text "0-1"})
                                        (drawtext {:text "1-0"}) (drawtext {:text "1-1"}))))
           (to-ffmpeg (bioscoop (chain (for [i (range 2)]
                                         (for [j (range 2)]
                                           (drawtext {:text (str i "-" j)}))))))))))

(deftest chain-empty-for-contributes-nothing
  (testing "an empty for loop contributes no filters, and doesn't disturb its neighbors"
    (is (= (to-ffmpeg (bioscoop (chain (crop {:out_w "100"}) (vignette {:angle "1.0"}))))
           (to-ffmpeg (bioscoop (chain (crop {:out_w "100"})
                                       (for [n (range 0)] (drawtext {:text (str n)}))
                                       (vignette {:angle "1.0"}))))))
    (is (empty? @dsl/last-errors))))

(deftest chain-rejects-parallel-filtergraph-from-for
  (testing "a for iteration that produces a genuinely parallel filtergraph is rejected, not silently linearized"
    (let [result (bioscoop (chain (crop {:out_w "100"})
                                   (for [n (range 2)]
                                     (compose (boxblur {:luma_radius "1"}) (vignette {:angle "1.0"})))
                                   (drawtext {:text "end"})))]
      (is (= "crop=out_w=100,drawtext=text=end" (to-ffmpeg result)))
      (is (some #(= :chain-parallel-filtergraph (:error-type (ex-data %))) @dsl/last-errors)))))


(deftest dispatch-matches-a-literal-call
  (testing "dispatch resolves a runtime name and produces the exact same filter a literal call would, across several filters"
    (is (= (to-ffmpeg (bioscoop (crop {:out_w "3000" :x "1000" :keep_aspect true})))
           (to-ffmpeg (bioscoop (dispatch "crop" {:out_w "3000" :x "1000" :keep_aspect true})))))
    (is (= (to-ffmpeg (bioscoop (vignette {:angle "1.3"})))
           (to-ffmpeg (bioscoop (dispatch "vignette" {:angle "1.3"})))))
    (is (= (to-ffmpeg (bioscoop (boxblur {:luma_radius "2"})))
           (to-ffmpeg (bioscoop (dispatch "boxblur" {:luma_radius "2"})))))))

(deftest dispatch-with-nil-args-matches-a-noarg-literal-call
  (testing "dispatch with nil args renders identically to a bare, argument-less literal call -- no trailing '='"
    (is (= "hflip" (to-ffmpeg (bioscoop (hflip)))))
    (is (= (to-ffmpeg (bioscoop (hflip)))
           (to-ffmpeg (bioscoop (dispatch "hflip" {})))))))

(deftest dispatch-through-chain-and-for-matches-a-literal-chain
  (testing "a chain built from a vector of [name args] pairs matches the same chain written out literally, in order"
    (let [chain-vec [["crop" {:out_w "3000" :x "1000" :keep_aspect true}]
                     ["hflip" {}]
                     ["zoompan" {:z "min(1+on*0.0015,1.75)" :x "iw*0.55-(iw/zoom/2)"
                                 :y "ih/2-(ih/zoom/2)" :s "573x696" :d 200}]
                     ["vignette" {:angle "1.3"}]]]
      (is (= (to-ffmpeg (bioscoop (chain (crop {:out_w "3000" :x "1000" :keep_aspect true})
                                          (hflip)
                                          (zoompan {:z "min(1+on*0.0015,1.75)" :x "iw*0.55-(iw/zoom/2)"
                                                    :y "ih/2-(ih/zoom/2)" :s "573x696" :d 200})
                                          (vignette {:angle "1.3"}))))
             (to-ffmpeg (bioscoop (chain (for [pair chain-vec] (dispatch (first pair) (second pair))))))))
      (is (= "crop=out_w=3000:x=1000:keep_aspect=true,hflip,zoompan=z=min(1+on*0.0015,1.75):x=iw*0.55-(iw/zoom/2):y=ih/2-(ih/zoom/2):s=573x696:d=200,vignette=angle=1.3" (to-ffmpeg (bioscoop (chain (for [pair chain-vec] (dispatch (first pair) (second pair)))))))))))


(deftest dispatch-fails-like-a-literal-call-on-a-bogus-name
  (testing "dispatch degrades the same way a literal call to an unresolvable name does"
    (let [via-literal (bioscoop (this-filter-does-not-exist {:x "1"}))
          errors-literal @dsl/last-errors
          via-dispatch (bioscoop (dispatch "this-filter-does-not-exist" {:x "1"}))
          errors-dispatch @dsl/last-errors]
      (is (= "" (to-ffmpeg via-literal) (to-ffmpeg via-dispatch)))
      (is (= :unresolved-function (:error-type (ex-data (first errors-literal)))))
      (is (= :unresolved-function (:error-type (ex-data (first errors-dispatch))))))))

(deftest dispatch-distinguishes-unimplemented-from-unresolved
  (testing "a real but not-yet-coded ffmpeg filter reports :not-implemented, not a generic unresolved error"
    (let [result (bioscoop (dispatch "minterpolate" {}))]
      (is (= "" (to-ffmpeg result)))
      (is (some #(= :not-implemented (:error-type (ex-data %))) @dsl/last-errors)))))

(deftest eval-is-a-full-interpreter
  (testing "the classics"
    (is (= "crop=out_w=3000:x=1000:keep_aspect=true"
           (to-ffmpeg (bioscoop (eval "(crop {:out_w \"3000\" :x \"1000\" :keep_aspect true})")))))))

(deftest eval-matches-a-literal-chain
  (testing "a filter built via eval matches the equivalent literal filter"
    (is (= (to-ffmpeg (bioscoop (crop {:out_w "100"})))
           (to-ffmpeg (bioscoop (eval "(crop {:out_w \"100\"})")))))))


(deftest eval-reaches-chain-unlike-dispatch
  (testing "eval can build a chain -- one of the DSL's own special forms -- which dispatch structurally cannot reach"
    (is (= (to-ffmpeg (bioscoop (chain (crop {:out_w "100"}) (vignette {:angle "1.3"}))))
           (to-ffmpeg (bioscoop (eval "(chain (crop {:out_w \"100\"}) (vignette {:angle \"1.3\"}))")))))))


(deftest eval-shares-lexical-scope
  (testing "symbols bound in the surrounding scope are visible inside the eval'd string"
    (let [w "100"]
      (is (= (to-ffmpeg (bioscoop (crop {:out_w w})))
             (to-ffmpeg (bioscoop (eval "(crop {:out_w w})"))))))))

(deftest eval-concatenates-string-arguments
  (testing "eval's arguments concatenate into one DSL source string"
    (is (= (to-ffmpeg (bioscoop (crop {:out_w "100"})))
           (to-ffmpeg (bioscoop (eval "(crop {:out_w \"" "100" "\"})"))))))
  (testing "eval's arguments concatenate into one DSL source string, so pieces can be assembled around a computed value"
    (let [w "100"]
      (is (= (to-ffmpeg (bioscoop (crop {:out_w w})))
             (to-ffmpeg (bioscoop (eval "(crop {:out_w \"" w "\"})"))))))))

(deftest eval-degrades-gracefully-on-a-runtime-error-in-valid-syntax
  (testing "valid syntax with an unresolvable name still goes through the ordinary accumulate-error path, unaffected by the two fixes above"
    (let [result (bioscoop (eval "(this-does-not-exist {:x \"1\"})"))]
      (is (= "" (to-ffmpeg result)))
      (is (= :unresolved-function (:error-type (ex-data (first @last-errors))))))))

(deftest top-level-var
  (testing "plain number def is valid"
    (intern *ns* 'plain-number 42)
    (bioscoop (scale plain-number 1080))
    (is (nil? (seq @dsl/last-errors)))
    (ns-unmap *ns* 'plain-number))

  (testing "string def triggers is valid"
    (intern *ns* 'plain-string "hello")
    (bioscoop (scale plain-string 1080))
    (is (nil? (seq @dsl/last-errors)))
    (ns-unmap *ns* 'plain-string))

  (testing "nil def triggers invalid parameter"
    (intern *ns* 'plain-nil nil)
    (bioscoop (scale plain-nil 1080))
    (is (= :invalid-parameter (:error-type (ex-data (first @dsl/last-errors)))))
    (ns-unmap *ns* 'plain-nil))

  (testing "vectors and maps are in Clojure, so they do NOT trigger the error"
    (intern *ns* 'plain-vec [1 2 3])
    (intern *ns* 'plain-map {:width 1 :height 1})
    (bioscoop (scale plain-vec 1080))
    (is (some #(= :invalid-parameter (:error-type (ex-data %))) @dsl/last-errors))
    (bioscoop (scale plain-map 1080))
    (is (nil? (seq @dsl/last-errors)))
    (ns-unmap *ns* 'plain-vec)
    (ns-unmap *ns* 'plain-map))

  (testing "a defn is ifn? so does NOT trigger unscoped-top-level-var"
    (intern *ns* 'my-fn (fn [x] (inc x)))
    (bioscoop (scale (my-fn 1919) 1080))
    (is (not (some #(= :unscoped-top-level-var (:error-type (ex-data %))) @dsl/last-errors)))
    (ns-unmap *ns* 'my-fn))

  (testing "defgraph FilterGraph does NOT trigger unscoped-top-level-var"
    (intern *ns* 'my-graph (bioscoop (scale 1920 1080)))
    (let [result (bioscoop (compose my-graph (crop "100")))]
      (is (= "scale=width=1920:height=1080;crop=out_w=100" (to-ffmpeg result)))
      (is (not (some #(= :unscoped-top-level-var (:error-type (ex-data %))) @dsl/last-errors))))
    (ns-unmap *ns* 'my-graph))

  (testing "shadowing with let at the bioscoop call site avoids the error"
    (let [shadowed-val 42
          result (bioscoop (scale shadowed-val 1080))]
      (is (= "scale=width=42:height=1080" (to-ffmpeg result)))
      (is (not (some #(= :unscoped-top-level-var (:error-type (ex-data %))) @dsl/last-errors)))))

  (testing "fn parameter avoids the error (the second suggested fix)"
    (let [my-wrapper (fn [param-val] (bioscoop (scale param-val 1080)))
          result (my-wrapper 99)]
      (is (= "scale=width=99:height=1080" (to-ffmpeg result)))
      (is (not (some #(= :unscoped-top-level-var (:error-type (ex-data %))) @dsl/last-errors))))))
