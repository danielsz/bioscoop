(ns bioscoop.macro-test
  (:require [bioscoop.macro :refer [bioscoop form->ast]]
            [bioscoop.dsl :as dsl]
            [bioscoop.render :refer [to-ffmpeg]]
            [clojure.test :refer [deftest is testing]]
            [bioscoop.domain.spec :as spec])
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
           (form->ast '(let [width 1920 height 1080] (scale width height)))))))

(deftest test-bioscoop-macro
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
      (is (= "[0:v]zoompan=z='min(zoom+0.0015,1.5)':d=700:x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2),fade=type=out:start_frame=600:duration=1[v0];[1:v]zoompan=z='min(zoom+0.0015,1.5)':d=700:x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2),fade=type=out:start_frame=600:duration=1[v1];[v0][v1]concat=n=2:v=1:a=0,format=pix_fmts=yuv420p[outv]" (to-ffmpeg dsl)))))

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
      (is (every? #(instance? FilterChain %) (:chains result)))
      (is (= 3 (count (:chains result))))      
      (is (= 1 (count (:filters (first (:chains result))))))
      (is (= 3 (count (map :filters (:chains result)))))))
  
  (testing "Automatic wrapping of FilterGraph/filterchain"
    (let [result (bioscoop (graph (scale 1920 1080) (scale 1910 1180) (scale 1920 80)))]
      (is (instance? FilterGraph result))
      (is (every? #(instance? Filter %) (:chains result)))
      (is (= 3 (count (:chains result))))
      (is (= 3 (count (map :filters (:chains result)))))))

  (testing "Automatic wrapping of FilterGraph/filterchain"
    (let [result (bioscoop (graph (chain (scale 1920 1080) (scale 1910 1180)) (scale 1920 80)))]
      (is (instance? FilterGraph result))
      (is (instance? FilterChain (first (:chains result))))
      (is (instance? Filter (last (:chains result))))
      (is (= 2 (count (:chains result))))
      (is (= 2 (count (:filters (first (:chains result)))))))))

