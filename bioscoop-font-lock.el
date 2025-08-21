;;; bioscoop-font-lock.el --- Enhanced font-locking for Bioscoop DSL macros  -*- lexical-binding: t; -*-

;; Copyright (C) 2024 Bioscoop Project

;; Author: Bioscoop Team
;; URL: https://github.com/bioscoop/bioscoop
;; Version: 0.1.0
;; Package-Requires: ((emacs "26.1") (clojure-mode "5.17"))
;; Keywords: clojure, ffmpeg, dsl, font-lock

;; This file is not part of GNU Emacs.

;;; Commentary:
;;
;; This package provides enhanced font-locking for Bioscoop DSL macros in
;; clojure-mode. It adds proper syntax highlighting for:
;; - `bioscoop` macro (font-locked as a macro definition)
;; - `defgraph` macro (font-locked as a macro definition with function name highlighting)
;;
;; Installation:
;;
;; Add to your init.el:
;; (require 'bioscoop-font-lock)
;; (add-hook 'clojure-mode-hook #'bioscoop-font-lock-mode)
;;
;; Or use use-package:
;; (use-package bioscoop-font-lock
;;   :after clojure-mode
;;   :hook (clojure-mode . bioscoop-font-lock-mode))

;;; Code:

(require 'clojure-mode)
(require 'cl-lib)

(defgroup bioscoop-font-lock nil
  "Enhanced font-locking for Bioscoop DSL macros."
  :group 'clojure
  :prefix "bioscoop-font-lock-")

(defcustom bioscoop-font-lock-mode-lighter " Bioscoop-FL"
  "Mode line lighter for bioscoop-font-lock-mode."
  :type 'string
  :group 'bioscoop-font-lock)

(defvar bioscoop-font-lock-mode nil
  "Non-nil if Bioscoop Font Lock mode is enabled.
Use the command `bioscoop-font-lock-mode' to change this variable.")

(defconst bioscoop-font-lock--keywords
  `((,(rx "(" (or "bioscoop" "defgraph") symbol-end)
     (1 font-lock-keyword-face))
    
    ;; defgraph name highlighting - matches the pattern: (defgraph name ...)
    (,(rx "(" (group "defgraph") symbol-end
           (one-or-more space)
           (group (one-or-more (or word (syntax symbol)))))
     (2 font-lock-function-name-face))
    
    ;; Additional DSL function highlighting (optional)
    (,(rx "(" (or "scale" "crop" "overlay" "fade" "filter" "chain" "graph"
                  "input-labels" "output-labels") symbol-end)
     (1 font-lock-builtin-face)))
  "Font lock keywords for Bioscoop DSL macros.")

;;;###autoload
(define-minor-mode bioscoop-font-lock-mode
  "Minor mode for enhanced font-locking of Bioscoop DSL macros.

When enabled, this mode provides proper syntax highlighting for:
- `bioscoop` macro (highlighted as keyword)
- `defgraph` macro (highlighted as keyword with function name highlighting)
- Common DSL functions (highlighted as built-in functions)

This mode is designed to work with `clojure-mode'."
  :lighter bioscoop-font-lock-mode-lighter
  :group 'bioscoop-font-lock
  (if bioscoop-font-lock-mode
      (bioscoop-font-lock--enable)
    (bioscoop-font-lock--disable)))

(defun bioscoop-font-lock--enable ()
  "Enable Bioscoop font-locking."
  (font-lock-flush)
  (font-lock-add-keywords nil bioscoop-font-lock--keywords))

(defun bioscoop-font-lock--disable ()
  "Disable Bioscoop font-locking."
  (font-lock-flush)
  (font-lock-remove-keywords nil bioscoop-font-lock--keywords))

;;;###autoload
(defun bioscoop-font-lock-setup ()
  "Setup function to enable Bioscoop font-locking.
This is a convenience function that can be added to hooks."
  (bioscoop-font-lock-mode 1))

;; Add to clojure-mode hooks automatically when loaded
;;;###autoload
(add-hook 'clojure-mode-hook #'bioscoop-font-lock-setup)

(provide 'bioscoop-font-lock)

;;; bioscoop-font-lock.el ends here