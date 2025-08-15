(ns bioscoop.core
  (:require
   [instaparse.core :as insta]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [bioscoop.dsl :as dsl :refer [dsl-parser compile-dsl]]
   [bioscoop.render :refer [to-ffmpeg to-dsl]]
   [bioscoop.ffmpeg-parser :as ffmpeg-parser]
   [bioscoop.parseable :refer [parse]]
   [bioscoop.macro :refer [bioscoop]]
   [bioscoop.ffmpeg :as ffmpeg])
  (:gen-class))

(defn the-haikou-diaries []
  (to-ffmpeg (bioscoop
               (let [background-color (color {:c "#0F172A" :size "1920x1280" :rate 25 :duration 3})
                     background-text (drawtext {:text "The Haikou Diaries" :x "(w-text_w)/2" :y "(h-text_h)/2" :fontsize 46 :fontcolor "#ebd999"
                                                :fontfile "/home/daniel/go/pkg/mod/github.com/u-root/u-root@v0.14.1-0.20250724181933-b01901710169/docs/src/fonts/SpaceGrotesk.woff2"})
                     zoom {:z "'min(zoom+0.0015,1.5)'" :d 400 :x "iw/2-(iw/zoom/2)" :y "ih/2-(ih/zoom/2)" :s "1920x1280"}
                     nozoom {:z "1" :d 400 :s "1920x1280"}
                     f {:type "out" :start_frame 320 :duration 1}
                     padding {:width "3/2*iw" :height "3/2*ih" :x "(ow-iw)/2" :y "(oh-ih)/2" :color "#0F172A"}]
                 (graph (chain (split {:input "0:v"} (output-labels "o0" "o1")))
                        (chain background-color background-text (format {:pix_fmts "rgb24"} {:output "c0"}) )
                        (chain (zoompan nozoom {:input "o0"}) (pad padding) (scale {:width 1920 :height -1} {:output "p1"}))
                        (chain (zoompan nozoom {:input "o1"}) (setsar {:sar "1"} {:output "p2"}))
                        (chain (zoompan zoom {:input "0:v"}) (fade f) (setsar {:sar "1"} {:output "v1"}))
                        (chain (zoompan zoom {:input "1:v"}) (fade f) (setsar {:sar "1"} {:output "v2"}))
                        (chain (concat {:n 5 :v 1 :a 0} (input-labels "c0" "p1" "p2" "v1" "v2")) (format {:pix_fmts "yuv420p"} {:output "out"})))))))

(defn bioscoop-ad []
  (to-ffmpeg (bioscoop (graph (chain (smptebars {:output "v0"}))
                              (chain (testsrc {:output "v1"}))
                              (chain (pad {:width "iw*2" :height "ih"} {:input "v0"} {:output "out0"}))
                              (chain (overlay {:x "w"} {:input "out0"} {:input "v1"}))))))

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
  (bioscoop-ad))


