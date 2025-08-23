(ns bioscoop.core
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [bioscoop.dsl :as dsl :refer [dsl-parser compile-dsl]]
   [bioscoop.render :refer [to-ffmpeg to-dsl]]
   [bioscoop.ffmpeg-parser :as ffmpeg-parser]
   [bioscoop.parseable :refer [parse]]
   [bioscoop.registry :refer [debug clear-registry!]]
   [bioscoop.macro :refer [bioscoop defgraph]]
   [bioscoop.domain.records :refer [join-filtergraphs compose+]]
   [bioscoop.built-in]
   [bioscoop.ffmpeg :as ffmpeg])
  (:gen-class))

(defn gcd 
  "(gcd a b) computes the greatest common divisor of a and b."
  [x y]
  (loop [a x
         b y]
    (if (zero? b)
      a
      (recur b (mod a b)))))

(defn aspect-ratio [width height]
  (let [n  (gcd width height)]
    (/ (/ width n) (/ height n))))


(defgraph title
  (let [background-color (color {:c "black" :size "1920x1280" :rate 25 :duration 16})
        base-text {:fontfile "/home/daniel/go/pkg/mod/github.com/u-root/u-root@v0.14.1-0.20250724181933-b01901710169/docs/src/fonts/SpaceGrotesk.woff2" :fontsize 66}
        part1 (drawtext (merge base-text {:textfile "/tmp/sentence1.txt" :x "(w-text_w)/2" :y "(h-text_h)/2" :fontcolor "white" :enable "'between(t,0,6)'"}))
        part2 (drawtext (merge base-text {:textfile "/tmp/sentence2.txt" :x "(w-text_w)/2" :y "(h-text_h)/2" :fontcolor_expr "'FF0000%{eif\\: clip(255 * (t/5), 0, 255) \\:x\\:2}'" :enable "'between(t,0,6)'"}))
        part3 (drawtext (merge base-text {:textfile "/tmp/sentence1.txt" :x "(w-text_w)/2" :y "(h-text_h)/2" :fontcolor_expr "'FFFFFF%{eif\\: clip(255 * (1 - (t - 6)/6), 0, 255) \\:x\\:2}'" :enable "'between(t,6,12)'"}))
        part4 (drawtext (merge base-text {:textfile "/tmp/sentence2.txt" :x "(w-text_w)/2" :y "(h-text_h)/2" :fontcolor "red" :enable "'between(t,6,12)'"}))
        part5 (drawtext (merge base-text {:textfile "/tmp/sentence2.txt" :x "(w-text_w)/2" :y "(h-text_h)/2" :fontcolor_expr "'FF0000%{eif\\: clip(255 * (1 - (t - 12)/4), 0, 255) \\:x\\:2}'" :enable "'between(t,12,16)'"}) {:output "intro"})]
    (spit "/tmp/sentence0.txt" "The Saul Haikou Lewicz Diaries")
    (spit "/tmp/sentence1.txt" " The        Haikou            Diaries ")
    (spit "/tmp/sentence2.txt" "    Saul            Lewicz         ")
    (chain background-color part1 part2 part3 part4 part5)))


(defgraph chinese-opera-woman (let [nozoom {:z "1" :d 800 :s "1920x1280"}]
                                (zoompan nozoom {:input "0:v"} {:output "v2"})))

(defgraph chinese-opera-woman-zoom (let [zoom {:z "'min(zoom+0.0015,1.5)'" :d 400 :x "iw/2-(iw/zoom/2)" :y "ih/2-(ih/zoom/2)" :s "1920x1280"}]
                                     (zoompan zoom {:input "0:v"} {:output "f2"})))

(defgraph chinese-opera-woman-trimmed (chain (trim {:duration 5} {:input "0:v"}) (setpts {:expr "PTS-STARTPTS"}) (fps {:fps "25"}) (scale {:width 1920 :height 1280 :force_original_aspect_ratio "decrease"} {:output "t1"})))

(defgraph splitting (split {:input "0:v"} (output-labels "o0" "o1")))
(defgraph chinese-opera-woman-padded (let [nozoom {:z "1" :d 300 :s "1920x1280"}
                                           padding  {:width "1920" :height "1280" :x "(ow-iw)/2" :y "(oh-ih)/2" :color "#0F172A"}]
                                      (chain (scale {:width 1152 :height 768 :force_original_aspect_ratio "decrease"} {:input "0:v"}) (pad padding) (zoompan nozoom {:output "v1"}))))

(defgraph cross-fade (xfade {:transition "fade" :duration 8 :offset 6} (input-labels "intro" "v1") {:output "c1"}))
(defgraph smoothleft (xfade {:transition "smoothleft" :duration 1 :offset 16} (input-labels "c1" "t1") {:output "p1"}))

(defgraph assembly2
  (chain (concat {:n 2 :v 1 :a 0} (input-labels "p1" "f2")) (format {:pix_fmts "yuv420p"} {:output "out"})))

(comment (def bar (let [filter (to-ffmpeg (bioscoop (compose part1 chinese-opera-woman-zoom assembly2)))]
                    (ffmpeg/with-inputs filter "/home/daniel/Pictures/chinese-opera/DSC09323.JPG")) ))

(comment (def foo (let [filter (to-ffmpeg (bioscoop (compose title chinese-opera-woman-padded cross-fade chinese-opera-woman-trimmed smoothleft)))]
                    (ffmpeg/with-inputs filter "/home/daniel/Pictures/chinese-opera/DSC09323.JPG")) ))

(defgraph part-one (compose title chinese-opera-woman-padded cross-fade chinese-opera-woman-trimmed smoothleft))

(defgraph the-haikou-diaries
  (let [base-text {:fontfile "/home/daniel/go/pkg/mod/github.com/u-root/u-root@v0.14.1-0.20250724181933-b01901710169/docs/src/fonts/SpaceGrotesk.woff2" :fontcolor "white"}
        untitled-text (drawtext (merge base-text {:text "Untitled" :x "w-text_w-400" :y "h-text_h-350" :fontsize 46}))
        zoom {:z "'min(zoom+0.0015,1.5)'" :d 400 :x "iw/2-(iw/zoom/2)" :y "ih/2-(ih/zoom/2)" :s "1920x1280"}
        nozoom {:z "1" :d 400 :s "1920x1280"}
        f {:type "out" :start_frame 320 :duration 1}
        padding {:width "3/2*iw" :height "3/2*ih" :x "(ow-iw)/5" :y "(oh-ih)/2" :color "#0F172A"}]
    (graph (chain (split {:input "0:v"} (output-labels "o0" "o1")))
           (chain (zoompan nozoom {:input "o0"}) (pad padding) untitled-text (scale {:width 1920 :height -1} {:output "p1"}))
           (chain (zoompan nozoom {:input "o1"}) (setsar {:sar "1"} {:output "p2"}))
           (chain (zoompan zoom {:input "0:v"}) (fade f) (setsar {:sar "1"} {:output "v1"}))
           (chain (zoompan zoom {:input "1:v"}) (fade f) (setsar {:sar "1"} {:output "v2"})))))

(defgraph assembly
  (chain (concat {:n 5 :v 1 :a 0} (input-labels "intro" "p1" "p2" "v1" "v2")) (format {:pix_fmts "yuv420p"} {:output "out"})))

(defgraph bioscoop-ad
  (graph (chain (smptebars {:output "v0"}))
         (chain (testsrc {:output "v1"}))
         (chain (pad {:width "iw*2" :height "ih"} {:input "v0"} {:output "out0"}))
         (chain (overlay {:x "w"} {:input "out0"} {:input "v1"}))))

(defn xstack []
  (to-ffmpeg (bioscoop (let [space-grotesk "/home/daniel/go/pkg/mod/github.com/u-root/u-root@v0.14.1-0.20250724181933-b01901710169/docs/src/fonts/SpaceGrotesk.woff2"
                             background-text (drawtext {:text "This is a video" :x "(w-text_w)/2" :y "(h-text_h)/2" :fontsize 400 :fontcolor "#ebd999"
                                                        :fontfile space-grotesk})
                             background-color (color {:c "#0F172A" :size "3840x2160" :sar "3/4" :rate 25 :duration 3})]
                         (graph (chain (testsrc {:duration 4}) (scale "hd1080" {:output "a"}))
                                (chain (rgbtestsrc {:duration 4}) (scale "hd1080" {:output "b"}))
                                (chain (smptebars {:duration 4}) (scale "hd1080" {:output "c"}))
                                (chain (yuvtestsrc {:duration 4}) (scale "hd1080" {:output "d"}))
                                (chain background-color background-text (format {:pix_fmts "rgb24"} {:output "out1"}))
                                (chain background-color background-text (format {:pix_fmts "rgb24"} {:output "out3"}))
                                (chain (xstack {:inputs 4 :layout "0_0|0_h0|w0_0|w0_h0"} {:input "a"} {:input "b"} {:input "c"} {:input "d"} {:output "out2"}))
                                (chain (concat {:n 3 :v 1 :a 0} {:input "out1"} {:input "out2"} {:input "out3"}) (format {:pix_fmts "yuv420p"} {:output "out"})))))))

(defn -main [& args]
  (log/info "Hello, World daniel")
  (println (to-ffmpeg (compile-dsl (first args)))))


(comment (let [filter (to-ffmpeg (bioscoop (compose title the-haikou-diaries assembly)))]
           (ffmpeg/with-inputs filter "/home/daniel/Pictures/thehaikoudiaries/DSCF3793_01.jpg" "/home/daniel/Pictures/thehaikoudiaries/DSCF3804_02.jpg")))

(comment (def foo (let [filter (to-ffmpeg (bioscoop (compose title the-haikou-diaries assembly)))]
  (ffmpeg/with-inputs filter "/home/daniel/Pictures/thehaikoudiaries/DSCF3793_01.jpg" "/home/daniel/Pictures/thehaikoudiaries/DSCF3804_02.jpg"))))

