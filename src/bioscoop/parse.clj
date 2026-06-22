(ns bioscoop.parse
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]))

(def dsl-parser (insta/parser (io/resource "lisp-grammar.bnf") :auto-whitespace :standard))

(def dsl-parses (partial insta/parses dsl-parser))
