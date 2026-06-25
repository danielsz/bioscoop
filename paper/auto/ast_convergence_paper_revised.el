;; -*- lexical-binding: t; -*-

(TeX-add-style-hook
 "ast_convergence_paper_revised"
 (lambda ()
   (TeX-add-to-alist 'LaTeX-provided-class-options
                     '(("IEEEtran" "conference")))
   (TeX-add-to-alist 'LaTeX-provided-package-options
                     '(("fancyhdr" "") ("graphicx" "") ("amsmath" "") ("amssymb" "") ("amsfonts" "") ("amsthm" "") ("algorithmic" "") ("textcomp" "") ("xcolor" "") ("listings" "") ("booktabs" "") ("multirow" "") ("ulem" "normalem") ("hyperref" "")))
   (add-to-list 'LaTeX-verbatim-environments-local "lstlisting")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "lstinline")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "path")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "url")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "nolinkurl")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "hyperbaseurl")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "hyperimage")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "href")
   (add-to-list 'LaTeX-verbatim-macros-with-delims-local "lstinline")
   (add-to-list 'LaTeX-verbatim-macros-with-delims-local "path")
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
    "instaparse"
    "gll"
    "taha"
    "reynolds"
    "clojurespec"
    "mps"
    "xtext"
    "nanopass"
    "carette")
   (LaTeX-add-bibliographies
    "references")
   (LaTeX-add-amsthm-newtheorems
    "theorem")
   (LaTeX-add-xcolor-definecolors
    "codegreen"
    "codegray"
    "codepurple"
    "backcolour"))
 :latex)

