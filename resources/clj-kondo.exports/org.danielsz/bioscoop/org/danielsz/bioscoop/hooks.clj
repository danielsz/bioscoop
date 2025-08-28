(ns org.danielsz.bioscoop.hooks
  (:require [clj-kondo.hooks-api :as api]))

;; ffmpeg -filters 2>&1 | awk '/^ [TSV\.]+/ {print $2}'
;; DSL functions available inside bioscoop macro

(def dsl-functions
  #{'compose
    'aap
    'abench
    'acompressor
    'acontrast
    'acopy
    'acue
    'acrossfade
    'acrossover
    'acrusher
    'adeclick
    'adeclip
    'adecorrelate
    'adelay
    'adenorm
    'aderivative
    'adrc
    'adynamicequalizer
    'adynamicsmooth
    'aecho
    'aemphasis
    'aeval
    'aexciter
    'afade
    'afftdn
    'afftfilt
    'afir
    'aformat
    'afreqshift
    'afwtdn
    'agate
    'aiir
    'aintegral
    'ainterleave
    'alatency
    'alimiter
    'allpass
    'aloop
    'amerge
    'ametadata
    'amix
    'amultiply
    'anequalizer
    'anlmdn
    'anlmf
    'anlms
    'anull
    'apad
    'aperms
    'aphaser
    'aphaseshift
    'apsnr
    'apsyclip
    'apulsator
    'arealtime
    'aresample
    'areverse
    'arls
    'arnndn
    'asdr
    'asegment
    'aselect
    'asendcmd
    'asetnsamples
    'asetpts
    'asetrate
    'asettb
    'ashowinfo
    'asidedata
    'asisdr
    'asoftclip
    'aspectralstats
    'asplit
    'astats
    'astreamselect
    'asubboost
    'asubcut
    'asupercut
    'asuperpass
    'asuperstop
    'atempo
    'atilt
    'atrim
    'axcorrelate
    'azmq
    'bandpass
    'bandreject
    'bass
    'biquad
    'bs2b
    'channelmap
    'channelsplit
    'chorus
    'compand
    'compensationdelay
    'crossfeed
    'crystalizer
    'dcshift
    'deesser
    'dialoguenhance
    'drmeter
    'dynaudnorm
    'earwax
    'ebur128
    'equalizer
    'extrastereo
    'firequalizer
    'flanger
    'haas
    'hdcd
    'headphone
    'highpass
    'highshelf
    'join
    'ladspa
    'loudnorm
    'lowpass
    'lowshelf
    'mcompand
    'pan
    'replaygain
    'rubberband
    'sidechaincompress
    'sidechaingate
    'silencedetect
    'silenceremove
    'speechnorm
    'stereotools
    'stereowiden
    'superequalizer
    'surround
    'tiltshelf
    'treble
    'tremolo
    'vibrato
    'virtualbass
    'volume
    'volumedetect
    'aevalsrc
    'afdelaysrc
    'afireqsrc
    'afirsrc
    'anoisesrc
    'anullsrc
    'hilbert
    'sinc
    'sine
    'anullsink
    'addroi
    'alphaextract
    'alphamerge
    'amplify
    'ass
    'atadenoise
    'avgblur
    'avgblur_opencl
    'avgblur_vulkan
    'backgroundkey
    'bbox
    'bench
    'bilateral
    'bilateral_cuda
    'bitplanenoise
    'blackdetect
    'blackframe
    'blend
    'blend_vulkan
    'blockdetect
    'blurdetect
    'bm3d
    'boxblur
    'boxblur_opencl
    'bwdif
    'bwdif_cuda
    'bwdif_vulkan
    'cas
    'ccrepack
    'chromaber_vulkan
    'chromahold
    'chromakey
    'chromakey_cuda
    'chromanr
    'chromashift
    'ciescope
    'codecview
    'colorbalance
    'colorchannelmixer
    'colorcontrast
    'colorcorrect
    'colorize
    'colorkey
    'colorkey_opencl
    'colorhold
    'colorlevels
    'colormap
    'colormatrix
    'colorspace
    'colorspace_cuda
    'colortemperature
    'convolution
    'convolution_opencl
    'convolve
    'copy
    'corr
    'cover_rect
    'crop
    'cropdetect
    'cue
    'curves
    'datascope
    'dblur
    'dctdnoiz
    'deband
    'deblock
    'decimate
    'deconvolve
    'dedot
    'deflate
    'deflicker
    'deinterlace_qsv
    'deinterlace_vaapi
    'dejudder
    'delogo
    'denoise_vaapi
    'deshake
    'deshake_opencl
    'despill
    'detelecine
    'dilation
    'dilation_opencl
    'displace
    'doubleweave
    'drawbox
    'drawgraph
    'drawgrid
    'drawtext
    'edgedetect
    'elbg
    'entropy
    'epx
    'eq
    'erosion
    'erosion_opencl
    'estdif
    'exposure
    'extractplanes
    'fade
    'feedback
    'fftdnoiz
    'fftfilt
    'field
    'fieldhint
    'fieldmatch
    'fieldorder
    'fillborders
    'find_rect
    'flip_vulkan
    'floodfill
    'format
    'fps
    'framepack
    'framerate
    'framestep
    'freezedetect
    'freezeframes
    'frei0r
    'fspp
    'fsync
    'gblur
    'gblur_vulkan
    'geq
    'gradfun
    'graphmonitor
    'grayworld
    'greyedge
    'guided
    'haldclut
    'hflip
    'hflip_vulkan
    'histeq
    'histogram
    'hqdn3d
    'hqx
    'hstack
    'hsvhold
    'hsvkey
    'hue
    'huesaturation
    'hwdownload
    'hwmap
    'hwupload
    'hwupload_cuda
    'hysteresis
    'identity
    'idet
    'il
    'inflate
    'interlace
    'interleave
    'kerndeint
    'kirsch
    'lagfun
    'latency
    'lenscorrection
    'libplacebo
    'libvmaf
    'limitdiff
    'limiter
    'loop
    'lumakey
    'lut
    'lut1d
    'lut2
    'lut3d
    'lutrgb
    'lutyuv
    'maskedclamp
    'maskedmax
    'maskedmerge
    'maskedmin
    'maskedthreshold
    'maskfun
    'mcdeint
    'median
    'mergeplanes
    'mestimate
    'metadata
    'midequalizer
    'minterpolate
    'mix
    'monochrome
    'morpho
    'mpdecimate
    'msad
    'multiply
    'negate
    'nlmeans
    'nlmeans_opencl
    'nlmeans_vulkan
    'nnedi
    'noformat
    'noise
    'normalize
    'null
    'oscilloscope
    'overlay
    'overlay_opencl
    'overlay_qsv
    'overlay_vaapi
    'overlay_vulkan
    'overlay_cuda
    'owdenoise
    'pad
    'pad_opencl
    'palettegen
    'paletteuse
    'perms
    'perspective
    'phase
    'photosensitivity
    'pixdesctest
    'pixelize
    'pixscope
    'pp
    'pp7
    'premultiply
    'prewitt
    'prewitt_opencl
    'procamp_vaapi
    'program_opencl
    'pseudocolor
    'psnr
    'pullup
    'qp
    'random
    'readeia608
    'readvitc
    'realtime
    'remap
    'remap_opencl
    'removegrain
    'removelogo
    'repeatfields
    'reverse
    'rgbashift
    'roberts
    'roberts_opencl
    'rotate
    'sab
    'scale
    'scale_cuda
    'scale_qsv
    'scale_vaapi
    'scale_vulkan
    'scale2ref
    'scdet
    'scharr
    'scroll
    'segment
    'select
    'selectivecolor
    'sendcmd
    'separatefields
    'setdar
    'setfield
    'setparams
    'setpts
    'setrange
    'setsar
    'settb
    'sharpness_vaapi
    'shear
    'showinfo
    'showpalette
    'shuffleframes
    'shufflepixels
    'shuffleplanes
    'sidedata
    'signalstats
    'signature
    'siti
    'smartblur
    'sobel
    'sobel_opencl
    'split
    'spp
    'ssim
    'ssim360
    'stereo3d
    'streamselect
    'subtitles
    'super2xsai
    'swaprect
    'swapuv
    'tblend
    'telecine
    'thistogram
    'threshold
    'thumbnail
    'thumbnail_cuda
    'tile
    'tiltandshift
    'tinterlace
    'tlut2
    'tmedian
    'tmidequalizer
    'tmix
    'tonemap
    'tonemap_opencl
    'tonemap_vaapi
    'tpad
    'transpose
    'transpose_opencl
    'transpose_vaapi
    'transpose_vulkan
    'trim
    'unpremultiply
    'unsharp
    'unsharp_opencl
    'untile
    'uspp
    'v360
    'vaguedenoiser
    'varblur
    'vectorscope
    'vflip
    'vflip_vulkan
    'vfrdet
    'vibrance
    'vidstabdetect
    'vidstabtransform
    'vif
    'vignette
    'vmafmotion
    'vpp_qsv
    'vstack
    'w3fdif
    'waveform
    'weave
    'xbr
    'xcorrelate
    'xfade
    'xfade_opencl
    'xfade_vulkan
    'xmedian
    'xpsnr
    'xstack
    'yadif
    'yadif_cuda
    'yaepblur
    'zmq
    'zoompan
    'zscale
    'hstack_vaapi
    'vstack_vaapi
    'xstack_vaapi
    'hstack_qsv
    'vstack_qsv
    'xstack_qsv
    'pad_vaapi
    'drawbox_vaapi
    'allrgb
    'allyuv
    'cellauto
    'color
    'color_vulkan
    'colorchart
    'colorspectrum
    'frei0r_src
    'gradients
    'haldclutsrc
    'life
    'mandelbrot
    'mptestsrc
    'nullsrc
    'openclsrc
    'pal75bars
    'pal100bars
    'perlin
    'rgbtestsrc
    'sierpinski
    'smptebars
    'smptehdbars
    'testsrc
    'testsrc2
    'yuvtestsrc
    'zoneplate
    'nullsink
    'a3dscope
    'abitscope
    'adrawgraph
    'agraphmonitor
    'ahistogram
    'aphasemeter
    'avectorscope
    'concat
    'showcqt
    'showcwt
    'showfreqs
    'showspatial
    'showspectrum
    'showspectrumpic
    'showvolume
    'showwaves
    'showwavespic
    'spectrumsynth
    'avsynctest
    'amovie
    'movie
    'abuffer
    'buffer
    'abuffersink
    'buffersink})

(defn dsl-function? [sym]
  (dsl-functions sym))

(defn transform-dsl-form
  "Transform DSL forms to prevent linting errors while preserving variable references"
  [form]
  (cond
    ;; Handle let bindings - these work like normal let, so pass through
    (and (api/list-node? form)
         (seq (:children form))
         (api/token-node? (first (:children form)))
         (= 'let (api/sexpr (first (:children form)))))
    form

    ;; Handle function calls
    (and (api/list-node? form)
         (seq (:children form))
         (api/token-node? (first (:children form))))
    (let [fn-name (api/sexpr (first (:children form)))
          args (rest (:children form))]
      (if (dsl-function? fn-name)
        ;; Transform DSL function to 'do' with arguments to preserve variable references
        ;; This ensures bound variables are still considered "used" by clj-kondo
        (if (seq args)
          (api/list-node (cons (api/token-node 'do) args))
          ;; If no args, just use a 'do' with nil
          (api/list-node (list (api/token-node 'do) (api/token-node 'nil))))
        ;; For unknown functions, let normal linting handle them
        form))

    ;; All other forms pass through unchanged
    :else form))

(defn walk-form
  "Recursively walk and transform nested forms"
  [form]
  (let [transformed (transform-dsl-form form)]
    (cond
      (api/list-node? transformed)
      (api/list-node (map walk-form (:children transformed)))

      (api/vector-node? transformed)
      (api/vector-node (map walk-form (:children transformed)))

      (api/map-node? transformed)
      (api/map-node (map walk-form (:children transformed)))

      :else transformed)))

(defn bioscoop
  "Hook for the bioscoop macro to provide proper linting.
   
   This transforms the macro call into a 'do' block where DSL function calls
   are replaced with identity calls to prevent unresolved-symbol warnings,
   while preserving the structure for other linting checks."
  [{:keys [node]}]
  (let [args (rest (:children node))
        ;; Transform each argument recursively
        transformed-args (map walk-form args)
        ;; Wrap in 'do' which accepts any number of forms
        new-node (api/list-node
                  (cons (api/token-node 'do) transformed-args))]
    {:node new-node}))

(defn defgraph
  "Hook for the defgraph macro to provide proper linting.
   
   This handles the defgraph macro which wraps bioscoop forms with additional
   registration and var definition. We transform it to a regular 'def' form
   to prevent unresolved symbol warnings for the graph name."
  [{:keys [node]}]
  (let [children (:children node)
        ;; defgraph has the pattern: (defgraph name & body-forms)
        name-node (second children)
        body-forms (drop 2 children)
        ;; Transform the body forms (which will be passed to bioscoop)
        transformed-body (map walk-form body-forms)
        ;; Create a 'def' form to properly declare the var
        new-node (api/list-node
                  [(api/token-node 'def)
                   name-node
                   (api/list-node
                    (cons (api/token-node 'do)
                          transformed-body))])]
    {:node new-node}))
