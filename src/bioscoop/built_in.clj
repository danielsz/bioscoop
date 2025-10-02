(ns bioscoop.built-in
  (:refer-clojure :exclude [format concat loop])
  (:require
   [bioscoop.domain.records :refer [make-filter]]
   [bioscoop.domain.spec :as spec]
   [bioscoop.domain.specs.color :as color]
   [bioscoop.domain.specs.hue :as hue]
   [bioscoop.domain.specs.format :as format]
   [bioscoop.domain.specs.drawtext :as drawtext]
   [bioscoop.domain.specs.zoompan :as zoompan]
   [bioscoop.domain.specs.concat :as concat]
   [bioscoop.domain.specs.fade :as fade]
   [bioscoop.domain.specs.scale :as scale]
   [bioscoop.domain.specs.crop :as crop]
   [bioscoop.domain.specs.pad :as pad]
   [bioscoop.domain.specs.overlay :as overlay]
   [bioscoop.domain.specs.sources :as sources]
   [bioscoop.domain.specs.layout :as layout]
   [bioscoop.domain.specs.negate :as negate]
   [bioscoop.domain.specs.threshold :as threshold]
   [bioscoop.domain.specs.edgedetect :as edgedetect]
   [bioscoop.domain.specs.gradients :as gradients]
   [bioscoop.domain.specs.palette :as palette]
   [bioscoop.domain.specs.blend :as blend]
   [bioscoop.domain.specs.curves :as curves]
   [bioscoop.domain.specs.lumakey :as lumakey]
   [bioscoop.domain.specs.geq :as geq]
   [bioscoop.domain.specs.flip :as flip]
   [bioscoop.domain.specs.loop :as loop]
   [bioscoop.domain.specs.split :as split]
   [bioscoop.domain.specs.trim :as trim]
   [bioscoop.domain.specs.fps :as fps]
   [bioscoop.domain.specs.setpts :as setpts]
   [bioscoop.domain.specs.life :as life]
   [bioscoop.domain.specs.cellauto :as cellauto]
   [bioscoop.domain.specs.boxblur :as boxblur]
   [bioscoop.domain.specs.lut :as lut]
   [bioscoop.domain.specs.colorchannelmixer :as colorchannelmixer]
   [bioscoop.domain.specs.effects :as effects]
   [clojure.spec.alpha :as s]
   [bioscoop.domain.specs.shared.image-size :as image-size]
   [bioscoop.error-handling :refer [accumulate-error]]))

(defn template [arg spec env]
  (if (seq arg)
    (if (map? (first arg))
      (let [m (first arg)]
        (if (s/valid? spec m)
          (make-filter (name spec) (spec/spec-aware-namespace-map spec m))
          (accumulate-error env m spec :invalid-parameter)))
      (let [formal-keys (last (s/form spec))
            m (zipmap formal-keys arg)]
        (if (s/valid? spec m)
          (make-filter (name spec) m)
          (accumulate-error env m spec :invalid-parameter))))
    (make-filter (name spec))))

(defn help [s]
  (let [spec (keyword (str "bioscoop.domain.specs." s) s)]
    (when (s/get-spec spec)
      (let [ks (last (s/describe spec))
            xs (reduce (fn [x y] (conj x y (s/describe y))) [] ks)]
        (apply array-map xs)))))

(def h help)

(defn scale [arg env]
  (template arg ::scale/scale env))

(defn crop [arg env]
  (template arg ::crop/crop env))

(defn fade [arg env]
  (template arg ::fade/fade env))

(defn overlay [arg env]
  (template arg ::overlay/overlay env))

(defn hflip [arg env]
  (template arg ::flip/hflip env))

(defn vflip [arg env]
  (template arg ::flip/vflip env))

(defn color [arg env]
  (template arg ::color/color env))

(defn format [arg env]
  (template arg ::format/format env))

(defn drawtext [arg env]
  (template arg ::drawtext/drawtext env))

(defn zoompan [arg env]
  (template arg ::zoompan/zoompan env))

(defn concat [arg env]
  (template arg ::concat/concat env))

(defn pad [arg env]
  (template arg ::pad/pad env))

(defn testsrc [arg env]
  (template arg ::sources/testsrc env))

(defn rgbtestsrc [arg env]
  (template arg ::sources/rgbtestsrc env))

(defn smptebars [arg env]
  (template arg ::sources/smptebars env))

(defn smptehdbars [arg env]
  (template arg ::sources/smptehdbars env))

(defn haldclutsrc [arg env]
  (template arg ::sources/haldclutsrc env))

(defn yuvtestsrc [arg env]
  (template arg ::sources/yuvtestsrc env))

(defn hstack [arg env]
  (template arg ::layout/hstack env))

(defn vstack [arg env]
  (template arg ::layout/vstack env))

(defn xstack [arg env]
  (template arg ::layout/xstack env))

(defn tile [arg env]
  (template arg ::layout/tile env))

(defn xfade [arg env]
  (template arg ::effects/xfade env))

(defn loop [arg env]
  (template arg ::loop/loop env))

(defn fps [arg env]
  (template arg ::fps/fps env))

(defn split [arg env]
  (template arg ::split/split env))

(defn trim [arg env]
  (template arg ::trim/trim env))

(defn setdar [arg env]
  (template arg ::image-size/setdar env))

(defn setsar [arg env]
  (template arg ::image-size/setsar env))

(defn setpts [arg env]
  (template arg ::setpts/setpts env))

(defn hue [arg env]
  (template arg ::hue/hue env))

(defn negate [arg env]
  (template arg ::negate/negate env))

(defn edgedetect [arg env]
  (template arg ::edgedetect/edgedetect env))

(defn gradients [arg env]
  (template arg ::gradients/gradients env))

(defn paletteuse [arg env]
  (template arg ::palette/paletteuse env))

(defn palettegen [arg env]
  (template arg ::palette/palettegen env))

(defn geq [arg env]
  (template arg ::geq/geq env))

(defn threshold [arg env]
  (template arg ::threshold/threshold env))

(defn curves [arg env]
  (template arg ::curves/curves env))

(defn blend [arg env]
  (template arg ::blend/blend env))

(defn lumakey [arg env]
  (template arg ::lumakey/lumakey env))

(defn life [arg env]
  (template arg ::life/life env))

(defn cellauto [arg env]
  (template arg ::cellauto/cellauto env))

(defn boxblur [arg env]
  (template arg ::boxblur/boxblur env))

(defn colorchannelmixer [arg env]
  (template arg ::colorchannelmixer/colorchannelmixer env))

(defn lut [arg env]
  (template arg ::lut/lut env))

(defn lutrgb [arg env]
  (template arg ::lut/lutrgb env))

(defn lutyuv [arg env]
  (template arg ::lut/lutyuv env))
