(ns bioscoop.convergence-test
  (:require [bioscoop.dsl :refer [compile-dsl]]
            [bioscoop.macro :refer [bioscoop]]
            [clojure.test :refer [deftest is testing]]
            [bioscoop.render :refer [to-ffmpeg]]))


(deftest macro-string-convergence
  (testing "External (compile-dsl) and internal (bioscoop macro) paths produce identical FFmpeg output"
    (let [;; Each entry: [external-dsl-string internal-clojure-form description]
          cases
          [["(scale 1920 1080)"
            (bioscoop (scale 1920 1080))
            "basic filter"]

           ["[[\"0:v\"] (scale 1920 1080) [\"scaled\"]]"
            (bioscoop [["0:v"] (scale 1920 1080) ["scaled"]])
            "padded graph with labels"]

           ["(chain (scale 1920 1080) (hflip))"
            (bioscoop (chain (scale 1920 1080) (hflip)))
            "chain of two filters"]

           ["(compose [[\"0:v\"] (scale 1920 1080) [\"scaled\"]] [[\"1:v\"] (hflip)])"
            (bioscoop (compose [["0:v"] (scale 1920 1080) ["scaled"]] [["1:v"] (hflip)]))
            "compose two padded graphs"]

           ["(let [w 1920] (scale w 1080))"
            (bioscoop (let [w 1920] (scale w 1080)))
            "let binding with simple value"]

           ["(let [w (inc 1919)] (scale w 1080))"
            (bioscoop (let [w (inc 1919)] (scale w 1080)))
            "let binding with computed value"]

           ["(let [w 1920] (let [h 1080] (scale w h)))"
            (bioscoop (let [w 1920] (let [h 1080] (scale w h))))
            "nested let bindings"]

           ["(for [i (range 3)] (scale))"
            (bioscoop (for [i (range 3)] (scale)))
            "for binding with range"]

           ["(for [i (range 3)] [[(str \"v\" i)] (scale)])"
            (bioscoop (for [i (range 3)] [[(str "v" i)] (scale)]))
            "for binding with dynamic labels"]

           ["(let [data {:width 1920 :height 1080}] (scale {:width (:width data) :height (:height data)}))"
            (bioscoop (let [data {:width 1920 :height 1080}] (scale {:width (:width data) :height (:height data)})))
            "keywords as functions in map access"]

           ["(drawtext {:text \"hello\" :x 100 :y 50})"
            (bioscoop (drawtext {:text "hello" :x 100 :y 50}))
            "filter with named map arguments"]

           ["(color {:color \"blue\" :size \"1920x1080\" :rate 24 :duration \"5\"})"
            (bioscoop (color {:color "blue" :size "1920x1080" :rate 24 :duration "5"}))
            "color source with map arguments"]]]
      (doseq [[dsl-str macro-result desc] cases]
        (testing (str desc ": " dsl-str)
          (let [external-result (to-ffmpeg (compile-dsl dsl-str))
                macro-result-str (to-ffmpeg macro-result)]
            (is (= external-result macro-result-str))))))))
