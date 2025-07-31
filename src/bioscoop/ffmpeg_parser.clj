(ns bioscoop.ffmpeg-parser
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint]
            [bioscoop.dsl :refer [make-filter make-filterchain make-filtergraph]]))

(def ffmpeg-parser
  (insta/parser (io/resource "ffmpeg-grammar.bnf")))

;; Fixed transform functions
(declare parse-link-label parse-filter-spec parse-filter-name parse-filter-args)
(defmulti parse-ffmpeg-ast first)

(defmethod parse-ffmpeg-ast :filtergraph [[_ & content]]
  (let [chains (filter #(= :filterchain (first %)) content)]
    (make-filtergraph (map parse-ffmpeg-ast chains))))

(defmethod parse-ffmpeg-ast :filterchain [[_ & filters]]
  (make-filterchain (map parse-ffmpeg-ast filters)))

(defmethod parse-ffmpeg-ast :filter [[_ & parts]]
  (let [input-labels (when-let [inputs (first (filter #(= :input-linklabels (first %)) parts))]
                      (vec (rest inputs)))  ; Just take the string names directly
        output-labels (when-let [outputs (first (filter #(= :output-linklabels (first %)) parts))]
                       (vec (rest outputs))) ; Just take the string names directly
        filter-spec (first (filter #(= :filter-spec (first %)) parts))]
    (parse-filter-spec filter-spec input-labels output-labels)))

(defmethod parse-ffmpeg-ast :filter-spec [[_ name-part & args-part]]
  ;; This method is called from parse-filter-spec, not directly
  nil)

(defn parse-filter-spec [filter-spec-node input-labels output-labels]
  (let [[_ name-node & rest] filter-spec-node
        filter-name (parse-filter-name name-node)
        ;; Now rest should directly contain the filter-arguments if present
        args-node (first rest)
        filter-args (when args-node (parse-filter-args args-node))]
    (make-filter filter-name filter-args (or input-labels []) (or output-labels []))))

(defn parse-filter-name [name-node]
  (cond
    ;; [:filter-name "crop"] -> "crop"
    (and (vector? name-node) (= :filter-name (first name-node)))
    (second name-node)
    
    ;; Direct string (if grammar changes)
    (string? name-node) name-node
    
    :else (str name-node)))

(defn parse-filter-args [args-node]
  (cond
    ;; [:filter-arguments [:unquoted-args "1920:800:0:140"]] -> "1920:800:0:140"
    (and (vector? args-node) (= :filter-arguments (first args-node)))
    (let [inner-node (second args-node)]
      (cond
        (and (vector? inner-node) (= :unquoted-args (first inner-node)))
        (second inner-node)
        
        (and (vector? inner-node) (= :quoted-string (first inner-node)))
        (second inner-node)
        
        :else (str inner-node)))
    
    :else (str args-node)))




(defn parse-single-linklabel [label-node]
  "Parse a single linklabel node"
  (cond
    ;; With hidden brackets: [:linklabel "in"]
    (and (vector? label-node) (= :linklabel (first label-node)))
    (second label-node)
    
    ;; Direct string (if grammar hides linklabel tag too)
    (string? label-node) label-node
    
    :else (str label-node)))

(defn parse-linklabels [linklabels-node]
  "Parse input-linklabels or output-linklabels node"
  (let [[_ & labels] linklabels-node]
    (mapv parse-single-linklabel labels)))

(defmethod parse-ffmpeg-ast :filter [[_ & parts]]
  (let [input-labels (when-let [inputs (first (filter #(= :input-linklabels (first %)) parts))]
                      (parse-linklabels inputs))
        output-labels (when-let [outputs (first (filter #(= :output-linklabels (first %)) parts))]
                       (parse-linklabels outputs))
        filter-spec (first (filter #(= :filter-spec (first %)) parts))]
    (parse-filter-spec filter-spec input-labels output-labels)))


(defn debug-ffmpeg-parse [filter-string]
  "Debug the FFmpeg parsing process"
  (println "Input:" filter-string)
  (let [ast (ffmpeg-parser filter-string)]
    (if (insta/failure? ast)
      (do
        (println "Parse error:" ast)
        ast)
      (do
        (println "Parse tree:")
        (clojure.pprint/pprint ast)
        (println "\nTransformed:")
        (let [result (parse-ffmpeg-ast ast)]  ; Fixed: added vector brackets and binding
          (clojure.pprint/pprint result)
          result)))))

(defmulti ffmpeg-ast->records first)

(defmethod ffmpeg-ast->records :filtergraph [[_ & content]]
  (let [chains (filter #(= :filterchain (first %)) content)]
    (make-filtergraph (mapv ffmpeg-ast->records chains))))

(defmethod ffmpeg-ast->records :filterchain [[_ & filters]]
  (make-filterchain (mapv ffmpeg-ast->records filters)))

(declare extract-input-labels extract-output-labels extract-filter-spec extract-filter-name extract-filter-args)
(defmethod ffmpeg-ast->records :filter [[_ & parts]]
  (let [input-labels (extract-input-labels parts)
        output-labels (extract-output-labels parts)
        filter-spec (first (filter #(= :filter-spec (first %)) parts))
        [filter-name filter-args] (extract-filter-spec filter-spec)]
    (make-filter filter-name filter-args input-labels output-labels)))

;; Helper functions for extraction
(defn extract-input-labels [parts]
  (when-let [inputs (first (filter #(= :input-linklabels (first %)) parts))]
    (vec (rest inputs))))

(defn extract-output-labels [parts]
  (when-let [outputs (first (filter #(= :output-linklabels (first %)) parts))]
    (vec (rest outputs))))

(defn extract-filter-spec [filter-spec-node]
  (let [[_ name-node & rest] filter-spec-node
        filter-name (extract-filter-name name-node)
        args-node (first (filter #(= :filter-arguments (first %)) rest))
        filter-args (when args-node (extract-filter-args args-node))]
    [filter-name filter-args]))

(defn extract-filter-name [name-node]
  (cond
    (and (vector? name-node) (= :filter-name (first name-node)))
    (second name-node)
    (string? name-node) name-node
    :else (str name-node)))

(defn extract-filter-args [args-node]
  (when args-node
    (let [[_ inner-node] args-node]
      (cond
        (and (vector? inner-node) (= :unquoted-args (first inner-node)))
        (second inner-node)
        (and (vector? inner-node) (= :quoted-string (first inner-node)))
        (second inner-node)
        :else (str inner-node)))))

;; Updated main parsing function
(defn parse-ffmpeg-filter 
  "Parse FFmpeg filter string and return Clojure records"
  [filter-string]
  (let [ast (ffmpeg-parser filter-string)]
    (if (insta/failure? ast)
      (throw (ex-info "FFmpeg parse error" {:error ast :input filter-string}))
      (ffmpeg-ast->records ast))))

;; For backward compatibility, keep raw parse tree function
(defn parse-ffmpeg-filter-raw
  "Parse FFmpeg filter string and return raw parse tree"  
  [filter-string]
  (let [ast (ffmpeg-parser filter-string)]
    (if (insta/failure? ast)
      (throw (ex-info "FFmpeg parse error" {:error ast :input filter-string}))
      (parse-ffmpeg-ast ast))))
