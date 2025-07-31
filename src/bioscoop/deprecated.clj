(ns bioscoop.deprecated
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(def whitespace
  (insta/parser
   "whitespace = #'\\s+'"))

(def ambiguous  "<program> = expression+
     expression = literal | symbol
     <literal> = number | string
     number = #'[0-9]+'
     string = <'\"'> #'[^\"]*' <'\"'>
     symbol = #'[a-zA-Z_$][a-zA-Z0-9_$]*'")


(def auto-whitespace
  (insta/parser
   ambiguous
   :auto-whitespace whitespace))

(def filtergraph-grammar (insta/parser (io/resource "filtergraph.bnf")))
(def lisp-grammar (insta/parser (io/resource "lisp-grammar.bnf") :auto-whitespace whitespace))

(def parser (insta/parser (io/resource "grammar.bnf")))

(def parser-test (insta/parser "S = #'^(?!(?:let|if|input)$)[a-zA-Z][a-zA-Z0-9-]*[?]?'"))

(def as-and-bs
  (insta/parser
    "S = AB*
     AB = A B
     A = 'a'+
     B = 'b'+"))


(defn transform-ast
  "Transform instaparse tree into Clojure data structures"
  [tree]
  (insta/transform
   {:program vector
    :expression (fn [x] (do (println "expression" x)
                       (identity x)))
    :let-binding (fn [bindings body] {:type :let :bindings bindings :body body})
    :binding (fn [sym expr] [sym expr])
    :conditional (fn [test then else] {:type :if :test test :then then :else else})
    :function-call (fn [op & args] (cons op args))
    :input-ref (fn [filename] {:type :input :file filename})
    :number #(if (str/includes? % ".") 
               (Double/parseDouble %) 
               (Long/parseLong %))
    :keyword keyword
    :string str
    :symbol symbol
    :vector (fn [& xs] (do (println "vector" xs)
                       (vec xs)))}
   tree))


;; protocol

(defprotocol FilterNode
  (to-filter [this context] "Convert node to FFmpeg filter string"))

(defrecord Input [file]
  FilterNode
  (to-filter [this context]
    {:input file
     :label (get-in context [:inputs file] (str "[" (count (:labels context)) "]"))}))

(defrecord Filter [name args input]
  FilterNode
  (to-filter [this context]
    (let [input-label (if (satisfies? FilterNode input)
                        (:label (to-filter input context))
                        (str input))
          output-label (str "[" (gensym "f") "]")
          filter-str (str name "=" (str/join ":" (map str args)))]
      {:filter (str input-label filter-str output-label)
       :label output-label})))

(defrecord Chain [operations]
  FilterNode
  (to-filter [this context]
    (reduce (fn [acc op]
              (let [result (to-filter op (assoc context :input acc))]
                result))
            nil
            operations)))


;;; compiler

(defmulti compile-expression 
  "Compile DSL expressions to filter graph components"
  (fn [expr context] 
    (cond
      (map? expr) (:type expr)
      (seq? expr) (first expr)
      (symbol? expr) :symbol
      :else :literal)))

(defmethod compile-expression :let [expr context]
  (let [{:keys [bindings body]} expr
        ;; Process bindings sequentially, building up context
        final-context (reduce (fn [ctx [sym val]]
                               (let [compiled-val (compile-expression val ctx)]
                                 (assoc-in ctx [:bindings sym] compiled-val)))
                             context
                             bindings)]
    ;; Compile body with full binding context
    (compile-expression body final-context)))

(defmethod compile-expression :if [expr context]
  (let [{:keys [test then else]} expr]
    ;; For now, conditionals are compile-time only
    ;; In a more advanced version, we could support runtime conditionals
    ;; based on video properties like resolution, duration, etc.
    (if (compile-expression test context)
      (compile-expression then context)
      (compile-expression else context))))

(defmethod compile-expression :symbol [expr context]
  ;; Look up symbol in binding context
  (if-let [bound-value (get-in context [:bindings expr])]
    bound-value
    (throw (ex-info (str "Unbound symbol: " expr) {:symbol expr :context context}))))

(defmethod compile-expression :input [expr context]
  (let [filename (:file expr)]
    (->Input filename)))

(defmethod compile-expression 'scale [expr context]
  (let [[_ input width height & opts] expr
        compiled-input (compile-expression input context)
        args [width height]]
    (->Filter "scale" args compiled-input)))

(defmethod compile-expression 'crop [expr context]
  (let [[_ input width height x y] expr
        compiled-input (compile-expression input context)
        args [width height x y]]
    (->Filter "crop" args compiled-input)))

(defmethod compile-expression 'eq [expr context]
  (let [[_ input & params] expr
        compiled-input (compile-expression input context)
        param-pairs (partition 2 params)
        args (mapcat (fn [[k v]] [(name k) v]) param-pairs)]
    (->Filter "eq" args compiled-input)))

(defmethod compile-expression 'overlay [expr context]
  (let [[_ bg fg & opts] expr
        compiled-bg (compile-expression bg context)
        compiled-fg (compile-expression fg context)
        opt-map (apply hash-map opts)
        x (get opt-map :x 0)
        y (get opt-map :y 0)]
    {:type :overlay
     :background compiled-bg
     :foreground compiled-fg
     :x x :y y}))

(defmethod compile-expression 'blur [expr context]
  (let [[_ input radius] expr
        compiled-input (compile-expression input context)]
    (->Filter "boxblur" [radius] compiled-input)))

(defmethod compile-expression 'denoise [expr context]
  (let [[_ input & opts] expr
        compiled-input (compile-expression input context)
        opt-map (apply hash-map opts)
        strength (get opt-map :strength 0.5)]
    (->Filter "hqdn3d" [strength] compiled-input)))

(defmethod compile-expression '-> [expr context]
  (let [[_ input & operations] expr]
    (reduce (fn [acc op]
              (compile-expression (list (first op) acc (rest op)) context))
            (compile-expression input context)
            operations)))

(defmethod compile-expression :literal [expr context]
  expr)

(defn build-filter-graph
  "Build complete FFmpeg filter graph from compiled expressions"
  [compiled-expr]
  (let [inputs (atom {})
        filters (atom [])
        counter (atom 0)]
    
    (letfn [(process-node [node]
              (cond
                (instance? Input node)
                (let [label (str "in" (swap! counter inc))]
                  (swap! inputs assoc (:file node) label)
                  (str "[" label "]"))
                
                (instance? Filter node)
                (let [input-label (process-node (:input node))
                      output-label (str "f" (swap! counter inc))
                      filter-str (str input-label (:name node) 
                                    (when (seq (:args node))
                                      (str "=" (str/join ":" (:args node))))
                                    "[" output-label "]")]
                  (swap! filters conj filter-str)
                  (str "[" output-label "]"))
                
                (map? node) ; Handle complex operations like overlay
                (case (:type node)
                  :overlay
                  (let [bg-label (process-node (:background node))
                        fg-label (process-node (:foreground node))
                        output-label (str "overlay" (swap! counter inc))
                        overlay-filter (str bg-label fg-label "overlay=" 
                                          (:x node) ":" (:y node) 
                                          "[" output-label "]")]
                    (swap! filters conj overlay-filter)
                    (str "[" output-label "]")))
                
                :else (str node)))]
      
      (let [output-label (process-node compiled-expr)]
        {:inputs @inputs
         :filters @filters
         :output output-label}))))

(defn generate-ffmpeg-command
  "Generate complete FFmpeg command from filter graph"
  [graph output-file]
  (let [input-args (mapcat (fn [[file label]] 
                            ["-i" file]) 
                          (:inputs graph))
        filter-complex (str/join ";" (:filters graph))
        output-map ["-map" (:output graph)]]
    
    (concat ["ffmpeg"] 
            input-args
            ["-filter_complex" filter-complex]
            output-map
            [output-file])))
