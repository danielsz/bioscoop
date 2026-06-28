(ns bioscoop.macro-test
  (:require [bioscoop.macro :refer [bioscoop form->ast defgraph]]
            [bioscoop.dsl :as dsl :refer [last-errors]]
            [bioscoop.render :refer [to-ffmpeg]]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [bioscoop.registry :refer [clear-registry! get-graph]]
            [bioscoop.built-in]
            [bioscoop.domain.records :refer [with-labels]])
  (:import [bioscoop.domain.records FilterGraph FilterChain Filter]))

(defn once-fixture [f]
  (f)
  (remove-ns 'user)
  (clear-registry!))

(use-fixtures :once once-fixture)

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
    (is (= (to-ffmpeg (get-graph 'foo)) "color=c=red:size=1920x1280:rate=25:duration=16,scale=width=450:height=300"))    
    (defgraph foo (let [shade "red"
                        background-color (color {:c shade :size "1920x1280" :rate 25 :duration 16})]
                    (chain background-color (scale 450 300))))
    (is (= (to-ffmpeg (get-graph 'foo)) "color=c=red:size=1920x1280:rate=25:duration=16,scale=width=450:height=300"))))

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
      (eval '(defgraph masterpiece (testsrc))))
    (is (= "[0]testsrc[1];[3]crop=out_w=111[2]" (to-ffmpeg (bioscoop (compose [["0"] bioscoop.masterpiece/masterpiece ["1"]] [["3"] (crop "111") ["2"]]))))))
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
