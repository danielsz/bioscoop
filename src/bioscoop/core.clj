(ns bioscoop.core
  (:require
   [instaparse.core :as insta]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [bioscoop.dsl :as dsl :refer [dsl-parser compile-dsl]]
   [bioscoop.render :refer [to-ffmpeg to-dsl]]
   [bioscoop.ffmpeg-parser :as ffmpeg]
   [bioscoop.parseable :refer [parse]]
   [bioscoop.macro :refer [bioscoop]])
  (:gen-class))

(defn -main [& args]
  (log/info "Hello, World daniel"))


(defn the-haikou-diaries []
  (to-ffmpeg (bioscoop
               (let [background-color (color {:c "#0F172A" :size "1280x720" :rate 25 :duration 3})
                     background-text (drawtext {:text "The Haikou Diaries" :x "(w-text_w)/2" :y "(h-text_h)/2" :fontsize 46 :fontcolor "#ebd999"
                                                :fontfile "/home/daniel/go/pkg/mod/github.com/u-root/u-root@v0.14.1-0.20250724181933-b01901710169/docs/src/fonts/SpaceGrotesk.woff2"} )
                     zoom {:z "'min(zoom+0.0015,1.5)'" :d 400 :x "iw/2-(iw/zoom/2)" :y "ih/2-(ih/zoom/2)"}
                     f {:type "out" :start_frame 320 :duration 1}]
                 (graph (chain background-color background-text (format {:pix_fmts "rgb24"} {:output "v0"}))
                        (chain (zoompan zoom {:input "0:v"}) (fade f {:output "v1"}))
                        (chain (zoompan zoom {:input "1:v"}) (fade f {:output "v2"}))
                        (chain (concat {:n 3 :v 1 :a 0} {:input "v0"} {:input "v1"} {:input "v2"}) (format {:pix_fmts "yuv420p"} {:output "outv"})))))))


