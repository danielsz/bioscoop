(ns bioscoop.built-in
  (:refer-clojure :exclude [format concat loop])
  (:require
   [bioscoop.domain.records :refer [make-filter]]
   [bioscoop.domain.spec :as spec]
   [bioscoop.domain.specs.color :as color]
   [bioscoop.domain.specs.hue :as hue]
   [bioscoop.domain.specs.format :as format]
   [bioscoop.domain.specs.drawtext :as drawtext]
   [bioscoop.domain.specs.drawgrid :as drawgrid]
   [bioscoop.domain.specs.drawbox :as drawbox]
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
   [bioscoop.domain.specs.eq :as eq]
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
   [bioscoop.domain.specs.gblur :as gblur]
   [bioscoop.domain.specs.lut :as lut]
   [bioscoop.domain.specs.lagfun :as lagfun]
   [bioscoop.domain.specs.colorchannelmixer :as colorchannelmixer]
   [bioscoop.domain.specs.colorbalance :as colorbalance]
   [bioscoop.domain.specs.effects :as effects]
   [bioscoop.domain.specs.dilation :as dilation]
   [bioscoop.domain.specs.tmix :as tmix]
   [bioscoop.domain.specs.noise :as noise]
   [bioscoop.domain.specs.dctdnoiz :as dctdnoiz]
   [bioscoop.domain.specs.rgbashift :as rgbashift]
   [bioscoop.domain.specs.tinterlace :as tinterlace]
   [bioscoop.domain.specs.shuffleplanes :as shuffleplanes]
   [bioscoop.domain.specs.random :as random]
   [bioscoop.domain.specs.frei0r :as frei0r]
   [bioscoop.domain.specs.subtitles :as subtitles]
   [bioscoop.domain.specs.adelay :as adelay]
   [bioscoop.domain.specs.volume :as volume]
   [bioscoop.domain.specs.amix :as amix]
   [bioscoop.domain.specs.afade :as afade]
   [bioscoop.domain.specs.atempo :as atempo]
   [bioscoop.domain.specs.aecho :as aecho]
   [bioscoop.domain.specs.asetrate :as asetrate]
   [bioscoop.domain.specs.asplit :as asplit]
   [bioscoop.domain.specs.atrim :as atrim]
   [bioscoop.domain.specs.afir :as afir]
   [bioscoop.domain.specs.rubberband :as rubberband]
   [bioscoop.domain.specs.sidechaincompress :as sidechaincompress]
   [bioscoop.domain.specs.afftdn :as afftdn]
   [bioscoop.domain.specs.aresample :as aresample]
   [bioscoop.domain.specs.bass :as bass]
   [bioscoop.domain.specs.compand :as compand]
   [bioscoop.domain.specs.flanger :as flanger]
   [bioscoop.domain.specs.loudnorm :as loudnorm]
   [bioscoop.domain.specs.lowpass :as lowpass]
   [bioscoop.domain.specs.treble :as treble]
   [bioscoop.domain.specs.setparams :as setparams]
   [bioscoop.domain.specs.nlmeans :as nlmeans]
   [bioscoop.domain.specs.hqdn3d :as hqdn3d]
   [bioscoop.domain.specs.arnndn :as arnndn]
   [bioscoop.domain.specs.fftdnoiz :as fftdnoiz]
   [bioscoop.domain.specs.atadenoise :as atadenoise]
   [bioscoop.domain.specs.tpad :as tpad]
   [bioscoop.domain.specs.apad :as apad]
   [bioscoop.domain.specs.settb :as settb]
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

(defn help* [s]
  (let [spec (keyword (str "bioscoop.domain.specs." s) s)]
    (when (s/get-spec spec)
      (let [ks (last (s/describe spec))
            xs (reduce (fn [x y] (conj x y (s/describe y))) [] ks)]
        (apply array-map xs)))))

(defmacro help [s]
  (let [n# (name s)]
    `(help* ~n#)))

(defn scale [arg env]
  (template arg ::scale/scale env))

(defn scale2ref [arg env]
  (template arg ::scale/scale2ref env))

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

(defn drawgrid [arg env]
  (template arg ::drawgrid/drawgrid env))

(defn drawbox [arg env]
  (template arg ::drawbox/drawbox env))

(defn zoompan [arg env]
  (template arg ::zoompan/zoompan env))

(defn concat [arg env]
  (template arg ::concat/concat env))

(defn pad [arg env]
  (template arg ::pad/pad env))

(defn testsrc [arg env]
  (template arg ::sources/testsrc env))

(defn testsrc2 [arg env]
  (template arg ::sources/testsrc2 env))

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

(defn asetpts [arg env]
  (template arg ::setpts/asetpts env))

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

(defn eq [arg env]
  (template arg ::eq/eq env))

(defn geq [arg env]
  (template arg ::geq/geq env))

(defn threshold [arg env]
  (template arg ::threshold/threshold env))

(defn curves [arg env]
  (template arg ::curves/curves env))

(defn blend [arg env]
  (template arg ::blend/blend env))

(defn tblend [arg env]
  (template arg ::blend/tblend env))

(defn lumakey [arg env]
  (template arg ::lumakey/lumakey env))

(defn life [arg env]
  (template arg ::life/life env))

(defn cellauto [arg env]
  (template arg ::cellauto/cellauto env))

(defn boxblur [arg env]
  (template arg ::boxblur/boxblur env))

(defn gblur [arg env]
  (template arg ::gblur/gblur env))

(defn colorchannelmixer [arg env]
  (template arg ::colorchannelmixer/colorchannelmixer env))

(defn colorbalance [arg env]
  (template arg ::colorbalance/colorbalance env))

(defn lut [arg env]
  (template arg ::lut/lut env))

(defn lut1d [arg env]
  (template arg ::lut/lut1d env))

(defn lut3d [arg env]
  (template arg ::lut/lut3d env))

(defn lutrgb [arg env]
  (template arg ::lut/lutrgb env))

(defn lutyuv [arg env]
  (template arg ::lut/lutyuv env))

(defn lagfun [arg env]
  (template arg ::lagfun/lagfun env))

(defn dilation [arg env]
  (template arg ::dilation/dilation env))

(defn erosion [arg env]
  (template arg ::dilation/erosion env))

(defn tmix [arg env]
  (template arg ::tmix/tmix env))

(defn noise [arg env]
  (template arg ::noise/noise env))

(defn dctdnoiz [arg env]
  (template arg ::dctdnoiz/dctdnoiz env))

(defn rgbashift [arg env]
  (template arg ::rgbashift/rgbashift env))

(defn tinterlace [arg env]
  (template arg ::tinterlace/tinterlace env))

(defn shuffleplanes [arg env]
  (template arg ::shuffleplanes/shuffleplanes env))

(defn random [arg env]
  (template arg ::random/random env))

(defn frei0r [arg env]
  (template arg ::frei0r/frei0r env))

(defn subtitles [arg env]
  (template arg ::subtitles/subtitles env))

(defn ass [arg env]
  (template arg ::subtitles/ass env))

(defn adelay [arg env]
  (template arg ::adelay/adelay env))

(defn volume [arg env]
  (template arg ::volume/volume env))

(defn amix [arg env]
  (template arg ::amix/amix env))

(defn afade [arg env]
  (template arg ::afade/afade env))

(defn atempo [arg env]
  (template arg ::atempo/atempo env))

(defn aecho [arg env]
  (template arg ::aecho/aecho env))

(defn asetrate [arg env]
  (template arg ::asetrate/asetrate env))

(defn afir [arg env]
  (template arg ::afir/afir env))

(defn rubberband [arg env]
  (template arg ::rubberband/rubberband env))

(defn sidechaincompress [arg env]
  (template arg ::sidechaincompress/sidechaincompress env))

(defn asplit [arg env]
  (template arg ::asplit/asplit env))

(defn atrim [arg env]
  (template arg ::atrim/atrim env))

(defn afftdn [arg env]
  (template arg ::afftdn/afftdn env))

(defn aresample [arg env]
  (template arg ::aresample/aresample env))

(defn bass [arg env]
  (template arg ::bass/bass env))

(defn treble [arg env]
  (template arg ::treble/treble env))

(defn compand [arg env]
  (template arg ::compand/compand env))

(defn flanger [arg env]
  (template arg ::flanger/flanger env))

(defn loudnorm [arg env]
  (template arg ::loudnorm/loudnorm env))

(defn lowpass [arg env]
  (template arg ::lowpass/lowpass env))

(defn setparams [arg env]
  (template arg ::setparams/setparams env))

(defn nlmeans [arg env]
  (template arg ::nlmeans/nlmeans env))

(defn hqdn3d [arg env]
  (template arg ::hqdn3d/hqdn3d env))

(defn arnndn [arg env]
  (template arg ::arnndn/arnndn env))

(defn fftdnoiz [arg env]
  (template arg ::fftdnoiz/fftdnoiz env))

(defn atadenoise [arg env]
  (template arg ::atadenoise/atadenoise env))

(defn tpad [arg env]
  (template arg ::tpad/tpad env))

(defn apad [arg env]
  (template arg ::apad/apad env))

(defn settb [arg env]
  (template arg ::settb/settb env))

(defn asettb [arg env]
  (template arg ::settb/asettb env))
