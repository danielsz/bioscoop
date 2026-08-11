;; -*- lexical-binding: t; -*-

(TeX-add-style-hook
 "bioscoop_paper"
 (lambda ()
   (TeX-add-to-alist 'LaTeX-provided-class-options
                     '(("IEEEtran" "conference") ("programming" "english" "submission")))
   (TeX-add-to-alist 'LaTeX-provided-package-options
                     '(("fancyhdr" "") ("graphicx" "") ("amsmath" "") ("amssymb" "") ("amsfonts" "") ("algorithmic" "") ("textcomp" "") ("xcolor" "") ("listings" "") ("booktabs" "") ("multirow" "") ("ulem" "normalem") ("hyperref" "") ("biblatex" "backend=biber") ("csquotes" "") ("amsthm" "")))
   (add-to-list 'LaTeX-verbatim-environments-local "lstlisting")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "href")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "hyperimage")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "hyperbaseurl")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "nolinkurl")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "url")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "path")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "lstinline")
   (add-to-list 'LaTeX-verbatim-macros-with-delims-local "path")
   (add-to-list 'LaTeX-verbatim-macros-with-delims-local "lstinline")
   (add-to-list 'LaTeX-verbatim-macros-with-delims-local "url")
   (TeX-run-style-hooks
    "latex2e"
    "programming"
    "programming10"
    "biblatex"
    "csquotes"
    "amsthm")
   (LaTeX-add-labels
    "sec:motivating-example"
    "fig:screenshot"
    "sec:ast-convergence"
    "tab:isomorphism"
    "sec:limitations"
    "sec:slideshow"
    "tab:slideshow-scale"
    "sec:music-video-code")
   (LaTeX-add-bibliographies
    "bioscoop")
   (LaTeX-add-amsthm-newtheorems
    "theorem"))
 :latex)

