(ns bioscoop.core
  (:require
   [instaparse.core :as insta]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [bioscoop.dsl :as dsl :refer [dsl-parser compile-dsl]]
   [bioscoop.render :refer [to-ffmpeg]]
   [bioscoop.ffmpeg-parser :as ffmpeg]
   [bioscoop.parseable :refer [parse]]
   [bioscoop.macro :refer [bioscoop]])
  (:gen-class))

(defn -main [& args]
  (log/info "Hello, World daniel"))


"crop=iw/2:ih:0:0,split[left][tmp];[tmp]hflip[right];[left][right]hstack"

(to-ffmpeg (bioscoop 
             (let [out-left-tmp (output-labels "left" "tmp")
                   in-tmp (input-labels "tmp")
                   out-right (output-labels "right")
                   in-left-right (input-labels "left" "right")]
               (chain 
                (crop "iw/2" "ih" "0" "0")
                (filter "split" "" out-left-tmp))
               (filter "hflip" "" in-tmp out-right)
               (filter "hstack" "" in-left-right))))

(to-ffmpeg (parse "(let [out-left-tmp (output-labels \"left\" \"tmp\")
      in-tmp (input-labels \"tmp\")
      out-right (output-labels \"right\")
      in-left-right (input-labels \"left\" \"right\")]
  (chain (chain 
    (crop \"iw/2\" \"ih\" \"0\" \"0\")
    (filter \"split\" \"\" out-left-tmp))
  (filter \"hflip\" \"\" in-tmp out-right)
  (filter \"hstack\" \"\" in-left-right)))"))


