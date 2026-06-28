;; -*- lexical-binding: t; -*-

(TeX-add-style-hook
 "bioscoop_paper"
 (lambda ()
   (TeX-add-to-alist 'LaTeX-provided-class-options
                     '(("IEEEtran" "conference")))
   (TeX-add-to-alist 'LaTeX-provided-package-options
                     '(("fancyhdr" "") ("graphicx" "") ("amsmath" "") ("amssymb" "") ("amsfonts" "") ("amsthm" "") ("algorithmic" "") ("textcomp" "") ("xcolor" "") ("listings" "") ("booktabs" "") ("multirow" "") ("ulem" "normalem") ("hyperref" "")))
   (TeX-run-style-hooks
    "latex2e"
    "IEEEtran"
    "IEEEtran10"
    "fancyhdr"
    "graphicx"
    "amsmath"
    "amssymb"
    "amsfonts"
    "amsthm"
    "algorithmic"
    "textcomp"
    "xcolor"
    "listings"
    "booktabs"
    "multirow"
    "ulem"
    "hyperref")
   (LaTeX-add-labels
    "tab:isomorphism")
   (LaTeX-add-bibitems
    "ffmpegpython"
    "fluentffmpeg"
    "moviepy"
    "hudak"
    "racket"
    "clojure"
    "krishnamurthi"
    "instaparse"
    "gll"
    "taha"
    "reynolds"
    "clojurespec"
    "mps"
    "xtext"
    "nanopass"
    "carette"
    "ffmpegio"
    "sass"
    "hiccup")
   (LaTeX-add-environments
    "theorem")
   (LaTeX-add-bibliographies
    "references"))
 :latex)

