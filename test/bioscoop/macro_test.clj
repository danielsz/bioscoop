(ns bioscoop.macro-test
  (:require [bioscoop.macro :refer [bioscoop form->ast defgraph]]
            [bioscoop.dsl :as dsl]
            [bioscoop.render :refer [to-ffmpeg]]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [bioscoop.registry :refer [clear-registry! get-graph]]
            [bioscoop.built-in]
            [clojure.tools.logging :as log])
  (:import [bioscoop.domain.records FilterGraph FilterChain Filter]))

(defn once-fixture [f]
  (f)
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
           (form->ast '(let [width 1920 height 1080] (scale width height)))))))

(deftest test-bioscoop-macro
  (testing "arithmetic expressions"
    (let [structures (bioscoop (let [base-width 1920]
                                 (scale (+ base-width 100) 1080)))]
      (is (= "scale=width=2020:height=1080" (to-ffmpeg structures)))))

  (testing "labels"
    (let [structures (bioscoop (scale 1920 1080
                                      (input-labels "input")
                                      (output-labels "scaled")))]
      (is (= "[input]scale=width=1920:height=1080[scaled]" (to-ffmpeg structures)))))
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
    (is (= :unresolved-function (:error-type (bioscoop (undefined-function 123))))))

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
    (let [text-result (to-ffmpeg (dsl/compile-dsl "(let [width 1920 height 1080] (scale width height {:input \"tmp\"}))"))
          macro-result (to-ffmpeg (bioscoop (let [width 1920 height 1080] (scale width height {:input "tmp"}))))
          ffmpeg-string "[tmp]scale=width=1920:height=1080"]
      (is (= text-result macro-result ffmpeg-string))))


  (testing "real world examples"
    (let [dsl (bioscoop 
             (let [out-left-tmp (output-labels "left" "tmp")
                   in-tmp (input-labels "tmp")
                   out-right (output-labels "right")
                   in-left-right (input-labels "left" "right")]
               (graph (chain 
                       (crop "iw/2" "ih" "0" "0")
                       (split  out-left-tmp))
                      (hflip  in-tmp out-right)
                      (hstack in-left-right))))]
      (is (= "crop=out_w=iw/2:w=ih:out_h=0:h=0,split[left][tmp];[tmp]hflip[right];[left][right]hstack" (to-ffmpeg dsl))))
    (let [dsl (bioscoop (graph (chain
                                (crop "iw/2" "ih" "0" "0")
                                (split {:output "left"} {:output "tmp"}))
                               (hflip {:input "tmp"} {:output "right"})
                               (hstack {:input "left"} {:input "right"})))]
      (is (= "crop=out_w=iw/2:w=ih:out_h=0:h=0,split[left][tmp];[tmp]hflip[right];[left][right]hstack"
             (to-ffmpeg dsl))))
    (let [dsl (bioscoop (chain (color "white" "480x480" 25 3) (format "rgb24") (drawtext {:fontcolor "black" :fontsize 600 :text "'%{eif\\:t\\:d}'" :x "(w-text_w)/2" :y "(h-text_h)/2"})))]
      (is (= "color=color=white:size=480x480:rate=25:duration=3,format=pix_fmts=rgb24,drawtext=fontcolor=black:fontsize=600:text='%{eif\\:t\\:d}':x=(w-text_w)/2:y=(h-text_h)/2" (to-ffmpeg dsl))))
    (let [dsl (bioscoop (zoompan {:z "'min(zoom+0.0015,1.5)'" :d 700 :x "iw/2-(iw/zoom/2)" :y "ih/2-(ih/zoom/2)"}))]
      (is (= "zoompan=z='min(zoom+0.0015,1.5)':d=700:x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2)" (to-ffmpeg dsl))))
    (let [dsl (bioscoop (let [zoom {:z "'min(zoom+0.0015,1.5)'" :d 700 :x "iw/2-(iw/zoom/2)" :y "ih/2-(ih/zoom/2)"}
                              f {:type "out" :start_frame 600 :duration 1}]
                          (graph (chain (zoompan zoom {:input "0:v"}) (fade f {:output "v0"}))
                                 (chain (zoompan zoom {:input "1:v"}) (fade f {:output "v1"}))
                                 (chain (concat {:n 2 :v 1 :a 0} {:input "v0"} {:input "v1"}) (format {:pix_fmts "yuv420p"} {:output "outv"})))))]
      (is (= "[0:v]zoompan=z='min(zoom+0.0015,1.5)':d=700:x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2),fade=type=out:start_frame=600:duration=1[v0];[1:v]zoompan=z='min(zoom+0.0015,1.5)':d=700:x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2),fade=type=out:start_frame=600:duration=1[v1];[v0][v1]concat=n=2:v=1:a=0,format=pix_fmts=yuv420p[outv]" (to-ffmpeg dsl))))
    (let [dsl (bioscoop (let [zoom {:z "'min(zoom+0.0015,1.5)'" :d 700 :x "iw/2-(iw/zoom/2)" :y "ih/2-(ih/zoom/2)"}
                              f {:type "out" :start_frame 600 :duration 1}]
                          (graph (chain (zoompan zoom {:input "0:v"}) (fade f {:output "v0"}))
                                 (chain (zoompan zoom {:input "1:v"}) (fade f {:output "v1"}))
                                 (chain (concat {:n 2 :v 1 :a 0} (input-labels "v0" "v1")) (format {:pix_fmts "yuv420p"} {:output "outv"})))))]
      (is (= "[0:v]zoompan=z='min(zoom+0.0015,1.5)':d=700:x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2),fade=type=out:start_frame=600:duration=1[v0];[1:v]zoompan=z='min(zoom+0.0015,1.5)':d=700:x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2),fade=type=out:start_frame=600:duration=1[v1];[v0][v1]concat=n=2:v=1:a=0,format=pix_fmts=yuv420p[outv]" (to-ffmpeg dsl)))))

  (testing "bioscoop ad"
    (is (= "smptebars[v0];testsrc[v1];[v0]pad=width=iw*2:height=ih[out0];[out0][v1]overlay=x=w"
         (to-ffmpeg (bioscoop (graph (chain (smptebars {:output "v0"}))
                              (chain (testsrc {:output "v1"}))
                              (chain (pad {:width "iw*2" :height "ih"} {:input "v0"} {:output "out0"}))
                              (chain (overlay {:x "w"} {:input "out0"} {:input "v1"}))))))))

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
    (let [result (bioscoop (graph (scale 1920 1080) (scale 1910 1180) (scale 1920 80)))]
      (is (instance? FilterGraph result))
      (is (true? (every? #(instance? Filter %) (:chains result))))
      (is (= 3 (count (:chains result))))
      (is (= 3 (count (map :filters (:chains result)))))))

  (testing "Automatic wrapping of FilterGraph/filterchain"
    (let [result (bioscoop (graph (chain (scale 1920 1080) (scale 1910 1180)) (scale 1920 80)))]
      (is (instance? FilterGraph result))
      (is (instance? FilterChain (first (:chains result))))
      (is (instance? Filter (last (:chains result))))
      (is (= 2 (count (:chains result))))
      (is (= 2 (count (:filters (first (:chains result))))))))

  (testing "the name following defgraph cannot be a built-in name"
    (is (thrown? AssertionError (defgraph split (split)))))
  (testing "the name following defgraph cannot be a known clojure.core name"
    (is (thrown? AssertionError (defgraph map (split)))))
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
            (is (= 2 (count (.-chains result))))))))
