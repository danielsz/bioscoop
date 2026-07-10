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


(defn- node-type [node] (if (vector? node) (first node) :unknown))
(defn- node-content [node] (if (vector? node) (second node) (str node)))

(defn- extract-positional-args [parts]
  (->> parts
       (mapcat (fn [node]
                 (if (= (node-type node) :unquoted-args)
                   (str/split (node-content node) #":")
                   [(node-content node)])))
       (map str/trim)
       (remove str/blank?)
       vec))

(defn- extract-key-value-args [parts]
  (let [;; 1. Flatten AST nodes into a sequence of string tokens.
        ;;    Split unquoted args by ":", but keep quoted strings intact.
        tokens (mapcat (fn [node]
                         (if (= (node-type node) :unquoted-args)
                           (->> (str/split (node-content node) #":")
                                (map str/trim)
                                (remove str/blank?))
                           [(node-content node)]))
                       parts)

        ;; 2. Helper to save the current key-value pair into the result map
        finalize (fn [result current-key current-val]
                   (if current-key
                     (assoc result current-key (str/trim current-val))
                     result))]

    ;; 3. Reduce over the tokens. If a token has "=", it starts a new key.
    ;;    Otherwise, it is appended to the current value.
    (-> (reduce (fn [{:keys [result current-key current-val]} token]
                  (if (str/includes? token "=")
                    (let [[k v] (str/split token #"=" 2)]
                      {:result (finalize result current-key current-val)
                       :current-key (keyword k)
                       :current-val v})
                    {:result result
                     :current-key current-key
                     :current-val (str current-val token)}))
                {:result {} :current-key nil :current-val ""}
                tokens)
        ;; 4. Finalize the last key-value pair
        ((fn [{:keys [result current-key current-val]}]
           (finalize result current-key current-val))))))

(defn extract-filter-args [args-node]
  (when args-node
    (let [parts (rest args-node)]
      (if (empty? parts)
        nil
        (let [has-keys? (some (fn [p]
                                (and (= (node-type p) :unquoted-args)
                                     (str/includes? (node-content p) "=")))
                              parts)]
          (if has-keys?
            (extract-key-value-args parts)
            (extract-positional-args parts)))))))


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


(defn transform-arg [arg]
  (or (parse-boolean arg) (parse-long arg) (parse-double arg)  arg))

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
        transformed-args (cond
                           (map? filter-args) [(into {} (map (fn [[k v]] [k (transform-arg v)]) filter-args))]
                           (string? filter-args) [(transform-arg filter-args)]
                           (seq filter-args) (mapv transform-arg filter-args)
                           :else nil)
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

