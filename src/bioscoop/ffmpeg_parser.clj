(ns bioscoop.ffmpeg-parser
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.pprint]
            [bioscoop.domain.records :refer [make-filter make-filterchain make-filtergraph with-input-labels with-output-labels]]))

(def ffmpeg-parser
  (insta/parser (io/resource "ffmpeg-grammar.bnf") :auto-whitespace :standard))

(def ffmpeg-parses (partial insta/parses ffmpeg-parser))


;; Helper functions for extraction
(defn extract-input-labels [parts]
  (when-let [inputs (first (filter #(= :input-linklabels (first %)) parts))]
    (mapv (fn [label-node]
            (if (and (vector? label-node) (= :linklabel (first label-node)))
              (second label-node)
              (str label-node)))
          (rest inputs))))

(defn extract-output-labels [parts]
  (when-let [outputs (first (filter #(= :output-linklabels (first %)) parts))]
    (mapv (fn [label-node]
            (if (and (vector? label-node) (= :linklabel (first label-node)))
              (second label-node)
              (str label-node)))
          (rest outputs))))

(defn transform-arg [arg]
  (or (parse-boolean arg) (parse-long arg) (parse-double arg)  arg))

(defn- parse-arg-node [arg-node]
  (let [inner (second arg-node)]
    (if (= (first inner) :key-value)
      ;; It's a map entry: [:key-value [:key "width"] [:unquoted-value "1920"]]
      (let [[_ key-node value-node] inner
            key (keyword (second key-node))
            val (if (= (first value-node) :quoted-string)
                  (second value-node)
                  (transform-arg (second value-node)))]
        [key val])
      ;; It's a positional arg: [:unquoted-value "1920"] or [:quoted-string "Hello"]
      (if (= (first inner) :quoted-string)
        (second inner)
        (transform-arg (second inner))))))

(defn extract-filter-args [args-node]
  (when args-node
    (let [arg-nodes (rest args-node)
          parsed-args (mapv parse-arg-node arg-nodes)]
      (if (every? vector? parsed-args)
        ;; All args are key-value pairs -> return a map
        (into {} parsed-args)
        ;; Args are positional -> return a vector of values
        parsed-args))))

(defn extract-filter-name [name-node]
  (cond
    (and (vector? name-node) (= :filter-name (first name-node)))
    (second name-node)
    (string? name-node) name-node
    :else (str name-node)))

(defn extract-filter-spec [filter-spec-node]
  (let [[_ name-node & rest] filter-spec-node
        filter-name (extract-filter-name name-node)
        args-node (first (filter #(= :filter-arguments (first %)) rest))
        filter-args (when args-node (extract-filter-args args-node))]
    [filter-name filter-args]))

(defmulti ffmpeg-ast->records first)

(defmethod ffmpeg-ast->records :filtergraph [[_ & content]]
  (let [chains (filter #(= :filterchain (first %)) content)]
    (make-filtergraph (mapv ffmpeg-ast->records chains))))

(defmethod ffmpeg-ast->records :filterchain [[_ & filters]]
  (make-filterchain (mapv ffmpeg-ast->records filters)))

(defmethod ffmpeg-ast->records :filter [[_ & parts]]
  (let [input-labels (extract-input-labels parts)
        output-labels (extract-output-labels parts)
        filter-spec (first (filter #(= :filter-spec (first %)) parts))
        [filter-name filter-args] (extract-filter-spec filter-spec)
        transformed-args (when filter-args
                           (if (map? filter-args)
                             [filter-args]
                             filter-args))
        base-filter (if filter-args
                      ((ns-resolve 'bioscoop.built-in (symbol filter-name)) transformed-args {:errors (atom [])})
                      (make-filter filter-name))]
    (cond-> base-filter
      (seq input-labels) (with-input-labels input-labels)
      (seq output-labels) (with-output-labels output-labels))))

(defn parse
  "Parse FFmpeg filter string and return Clojure records"
  [filter-string]
  (let [ast (ffmpeg-parser filter-string)]
    (if (insta/failure? ast)
      (throw (ex-info "FFmpeg parse error" {:error ast :input filter-string}))
      (ffmpeg-ast->records ast))))

