(ns bioscoop.ffmpeg-parser
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.pprint]
            [bioscoop.dsl :refer [with-input-labels with-output-labels]]
            [bioscoop.domain.records :refer [make-filter make-filterchain make-filtergraph]]
            [bioscoop.domain.spec :as spec]
            [bioscoop.built-in :as filters]))

(def ffmpeg-parser
  (insta/parser (io/resource "ffmpeg-grammar.bnf") :auto-whitespace :standard))


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
        [filter-name filter-args] (extract-filter-spec filter-spec)
        base-filter (if filter-args
                      ((ns-resolve 'bioscoop.built-in (symbol filter-name)) filter-args)
                      (make-filter filter-name))]
    ;; Add labels as metadata if present
    (cond-> base-filter
      (seq input-labels) (with-input-labels input-labels)
      (seq output-labels) (with-output-labels output-labels))))

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
        (let [args (-> (second inner-node)
                      (str/split  #":"))]
          (map (fn [s] (last (str/split s #"="))) args))
        (and (vector? inner-node) (= :quoted-string (first inner-node)))
        (second inner-node)
        :else (str inner-node)))))

(defn parse
  "Parse FFmpeg filter string and return Clojure records"
  [filter-string]
  (let [ast (ffmpeg-parser filter-string)]
    (if (insta/failure? ast)
      (throw (ex-info "FFmpeg parse error" {:error ast :input filter-string}))
      (ffmpeg-ast->records ast))))

