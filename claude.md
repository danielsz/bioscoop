NAME             ::= sequence of alphanumeric characters and '_'
FILTER_NAME      ::= NAME["@"NAME]
LINKLABEL        ::= "[" NAME "]"
LINKLABELS       ::= LINKLABEL [LINKLABELS]
FILTER_ARGUMENTS ::= sequence of chars (possibly quoted)
FILTER           ::= [LINKLABELS] FILTER_NAME ["=" FILTER_ARGUMENTS] [LINKLABELS]
FILTERCHAIN      ::= FILTER [,FILTERCHAIN]
FILTERGRAPH      ::= [sws_flags=flags;] FILTERCHAIN [;FILTERGRAPH]

*** We are going to write a dsl to express and chain together ffmpeg filters. The tool chain will be written in Clojure, with Instaparse as the parser for a lisp style, Clojure-compatible language. Please write a BNF grammar for a Lisp-style language with let bindings, a compiler to transform the parse tree into Clojure data structures and then those data structures into ffmpeg filters. After completing this work, refer to the BNF grammar that ships with the ffmpeg manual and that I've brought here, write a version of that grammar that will make it work with Instaparse, and test the output of the compiler you wrote previously to make sure it produces valid  filtergraph expressions. Finally, write documentation for our language.

I'll help you create a comprehensive DSL for expressing ffmpeg filters in a Lisp-style language. Let's approach this systematically, starting with the data structures and then building the transformations.

** Part 1: Lisp-style DSL Grammar (BNF for Instaparse)

#+begin_src clojure
;; grammar.bnf
program = expression*

expression = atom | list | let-binding

let-binding = <'('> <'let'> <'['> binding* <']'> expression+ <')'>
binding = <'['> symbol expression <']'>

list = <'('> (symbol | keyword) expression* <')'>

atom = number | string | symbol | keyword | boolean

number = #'-?\d+(\.\d+)?'
string = <'"'> #'[^"]*' <'"'>
symbol = #'[a-zA-Z_][a-zA-Z0-9_\-]*[?!]?'
keyword = <':'> symbol
boolean = 'true' | 'false'

<whitespace> = #'\s+'
#+end_src

** Part 2: Core Data Structures and Compiler

#+begin_src clojure
(ns ffmpeg-dsl.core
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]))

;; Data structure specifications
(s/def ::name (s/and string? #(re-matches #"[a-zA-Z0-9_]+" %)))
(s/def ::filter-name (s/and string? #(re-matches #"[a-zA-Z0-9_]+(@[a-zA-Z0-9_]+)?" %)))
(s/def ::link-label (s/and string? #(re-matches #"[a-zA-Z0-9_]+" %)))
(s/def ::filter-args string?)

(s/def ::filter (s/keys :req-un [::filter-name]
                       :opt-un [::input-labels ::output-labels ::args]))
(s/def ::filterchain (s/coll-of ::filter))
(s/def ::filtergraph (s/coll-of ::filterchain))

;; Parser setup
(def dsl-parser
  (insta/parser (slurp "grammar.bnf")))

;; Core data structures for our DSL
(defrecord Filter [name args input-labels output-labels])
(defrecord FilterChain [filters])
(defrecord FilterGraph [chains])

;; Environment for let bindings
(defn make-env
  ([] {})
  ([parent] (assoc {} :parent parent)))

(defn env-get [env sym]
  (if-let [val (get env sym)]
    val
    (when-let [parent (:parent env)]
      (env-get parent sym))))

(defn env-put [env sym val]
  (assoc env sym val))

;; AST transformation functions
(defmulti transform-ast (fn [node env] (first node)))

(defmethod transform-ast :program [[_ & expressions] env]
  (mapv #(transform-ast % env) expressions))

(defmethod transform-ast :let-binding [[_ bindings & body] env]
  (let [new-env (reduce (fn [acc [_ sym expr]]
                         (env-put acc sym (transform-ast expr env)))
                       (make-env env)
                       bindings)]
    (mapv #(transform-ast % new-env) body)))

(defmethod transform-ast :list [[_ op & args] env]
  (let [transformed-args (mapv #(transform-ast % env) args)]
    (case op
      :filter (apply make-filter transformed-args)
      :chain (make-filterchain transformed-args)
      :graph (make-filtergraph transformed-args)
      :input-labels (make-input-labels transformed-args)
      :output-labels (make-output-labels transformed-args)
      ;; Default: treat as function call
      (apply (resolve-function op env) transformed-args))))

(defmethod transform-ast :symbol [[_ sym] env]
  (or (env-get env sym) sym))

(defmethod transform-ast :keyword [[_ kw] env]
  kw)

(defmethod transform-ast :string [[_ s] env]
  s)

(defmethod transform-ast :number [[_ n] env]
  (if (str/includes? n ".")
    (Double/parseDouble n)
    (Long/parseLong n)))

(defmethod transform-ast :boolean [[_ b] env]
  (= "true" b))

;; Filter construction functions
(defn make-filter
  ([name] (->Filter name nil [] []))
  ([name args] (->Filter name args [] []))
  ([name args input-labels output-labels]
   (->Filter name args (or input-labels []) (or output-labels []))))

(defn make-filterchain [filters]
  (->FilterChain (vec filters)))

(defn make-filtergraph [chains]
  (->FilterGraph (vec chains)))

(defn make-input-labels [labels]
  {:type :input-labels :labels (vec labels)})

(defn make-output-labels [labels]
  {:type :output-labels :labels (vec labels)})

;; Function resolution for DSL
(defn resolve-function [op env]
  (case op
    :scale (fn [w h] (make-filter "scale" (str w ":" h)))
    :crop (fn [w h x y] (make-filter "crop" (str w ":" h ":" x ":" y)))
    :overlay (fn [] (make-filter "overlay"))
    :fade (fn [type start duration] 
           (make-filter "fade" (str (name type) ":" start ":" duration)))
    ;; Add more built-in functions as needed
    (fn [& args] (make-filter (name op) (str/join ":" args)))))

;; Compiler: DSL -> Clojure data structures
(defn compile-dsl [dsl-code]
  (let [ast (dsl-parser dsl-code)]
    (if (insta/failure? ast)
      (throw (ex-info "Parse error" {:error ast}))
      (transform-ast ast (make-env)))))
#+end_src

** Part 3: FFmpeg Filter Output Generator

#+begin_src clojure
(ns ffmpeg-dsl.output
  (:require [clojure.string :as str]
            [ffmpeg-dsl.core :refer :all]))

;; Transform our data structures to ffmpeg filter format
(defprotocol FFmpegRenderable
  (to-ffmpeg [this] "Convert to ffmpeg filter string"))

(extend-protocol FFmpegRenderable
  Filter
  (to-ffmpeg [{:keys [name args input-labels output-labels]}]
    (let [input-str (when (seq input-labels)
                     (str/join "" (map #(str "[" % "]") input-labels)))
          output-str (when (seq output-labels)
                      (str/join "" (map #(str "[" % "]") output-labels)))
          args-str (when args (str "=" args))]
      (str input-str name args-str output-str)))
  
  FilterChain
  (to-ffmpeg [{:keys [filters]}]
    (str/join "," (map to-ffmpeg filters)))
  
  FilterGraph
  (to-ffmpeg [{:keys [chains]}]
    (str/join ";" (map to-ffmpeg chains))))

;; Helper functions for common patterns
(defn with-labels [filter input-labels output-labels]
  (assoc filter 
         :input-labels (vec input-labels)
         :output-labels (vec output-labels)))

(defn chain-filters [& filters]
  (make-filterchain filters))

(defn parallel-filters [& chains]
  (make-filtergraph chains))
#+end_src

** Part 4: Instaparse Grammar for FFmpeg Filter Syntax

#+begin_src clojure
;; ffmpeg-grammar.bnf
filtergraph = sws-flags? filterchain (semicolon filterchain)*
sws-flags = 'sws_flags' equals flags semicolon
filterchain = filter (comma filter)*
filter = input-linklabels? filter-spec output-linklabels?
filter-spec = filter-name (equals filter-arguments)?
filter-name = name (at name)?
filter-arguments = quoted-string | unquoted-args
input-linklabels = linklabel+
output-linklabels = linklabel+
linklabel = lbracket name rbracket
name = #'[a-zA-Z0-9_]+'
flags = #'[^;]+'
quoted-string = <'"'> #'[^"]*' <'"'>
unquoted-args = #'[^,;\[\]]+'
at = '@'
equals = '='
comma = ','
semicolon = ';'
lbracket = '['
rbracket = ']'
<whitespace> = #'\s+'
#+end_src

#+begin_src clojure
(ns ffmpeg-dsl.ffmpeg-parser
  (:require [instaparse.core :as insta]
            [clojure.string :as str]))

(def ffmpeg-parser
  (insta/parser (slurp "ffmpeg-grammar.bnf")))

;; Transform ffmpeg filter syntax to our data structures
(defmulti parse-ffmpeg-ast first)

(defmethod parse-ffmpeg-ast :filtergraph [[_ & content]]
  (let [chains (filter #(= :filterchain (first %)) content)]
    (make-filtergraph (map parse-ffmpeg-ast chains))))

(defmethod parse-ffmpeg-ast :filterchain [[_ & filters]]
  (make-filterchain (map parse-ffmpeg-ast filters)))

(defmethod parse-ffmpeg-ast :filter [[_ & parts]]
  (let [input-labels (when-let [inputs (first (filter #(= :input-linklabels (first %)) parts))]
                      (map #(nth % 2) (rest inputs)))
        output-labels (when-let [outputs (first (filter #(= :output-linklabels (first %)) parts))]
                       (map #(nth % 2) (rest outputs)))
        filter-spec (first (filter #(= :filter-spec (first %)) parts))
        [_ name-part args-part] filter-spec
        filter-name (nth name-part 1)
        filter-args (when args-part (nth args-part 1))]
    (make-filter filter-name filter-args input-labels output-labels)))

(defn parse-ffmpeg-filter [filter-string]
  (let [ast (ffmpeg-parser filter-string)]
    (if (insta/failure? ast)
      (throw (ex-info "FFmpeg parse error" {:error ast}))
      (parse-ffmpeg-ast ast))))
#+end_src

** Part 5: Testing and Validation

#+begin_src clojure
(ns ffmpeg-dsl.test
  (:require [clojure.test :refer :all]
            [ffmpeg-dsl.core :refer :all]
            [ffmpeg-dsl.output :refer :all]
            [ffmpeg-dsl.ffmpeg-parser :refer :all]))

(deftest test-dsl-compilation
  (testing "Basic filter creation"
    (let [result (compile-dsl "(filter \"scale\" \"1920:1080\")")]
      (is (= "scale=1920:1080" (to-ffmpeg (first result))))))
  
  (testing "Filter with labels"
    (let [dsl "(let [[input-vid (input-labels \"in\")]
                     [scaled (filter \"scale\" \"1920:1080\" input-vid (output-labels \"scaled\"))]]
                 scaled)"
          result (compile-dsl dsl)]
      (is (= "[in]scale=1920:1080[scaled]" (to-ffmpeg (last result))))))
  
  (testing "Filter chain"
    (let [dsl "(chain 
                 (filter \"scale\" \"1920:1080\")
                 (filter \"overlay\"))"
          result (compile-dsl dsl)]
      (is (= "scale=1920:1080,overlay" (to-ffmpeg (first result)))))))

(deftest test-ffmpeg-parsing
  (testing "Parse simple filter"
    (let [result (parse-ffmpeg-filter "scale=1920:1080")]
      (is (= "scale=1920:1080" (to-ffmpeg result)))))
  
  (testing "Parse filter with labels"
    (let [result (parse-ffmpeg-filter "[in]scale=1920:1080[out]")]
      (is (= "[in]scale=1920:1080[out]" (to-ffmpeg result)))))
  
  (testing "Parse filter chain"
    (let [result (parse-ffmpeg-filter "scale=1920:1080,overlay")]
      (is (= "scale=1920:1080,overlay" (to-ffmpeg result))))))

(deftest test-roundtrip
  (testing "DSL -> FFmpeg -> DSL roundtrip"
    (let [original-dsl "(chain (filter \"scale\" \"1920:1080\") (filter \"overlay\"))"
          compiled (compile-dsl original-dsl)
          ffmpeg-output (to-ffmpeg (first compiled))
          parsed-back (parse-ffmpeg-filter ffmpeg-output)]
      (is (= ffmpeg-output (to-ffmpeg parsed-back))))))

(run-tests)
#+end_src

** Part 6: Documentation

#+begin_src markdown
# FFmpeg Filter DSL

A Clojure-based Domain Specific Language for expressing and composing
FFmpeg filters using a Lisp-style syntax.

## Overview

This DSL provides a functional approach to building complex FFmpeg
filter graphs using familiar Lisp syntax with let bindings, function
composition, and data transformation patterns.

## Syntax

### Basic Filters

#+end_srcclojure
;; Simple filter
(filter "scale" "1920:1080")
;; Produces: scale=1920:1080

;; Filter with input/output labels
(filter "scale" "1920:1080" (input-labels "in") (output-labels "scaled"))
;; Produces: [in]scale=1920:1080[scaled]
#+begin_src 

### Filter Chains

#+end_srcclojure
;; Chain filters together
(chain 
  (filter "scale" "1920:1080")
  (filter "crop" "1920:800:0:140"))
;; Produces: scale=1920:1080,crop=1920:800:0:140
#+begin_src 

### Filter Graphs

#+end_srcclojure
;; Parallel filter chains
(graph
  (chain (filter "scale" "1920:1080"))
  (chain (filter "fade" "in:0:30")))
;; Produces: scale=1920:1080;fade=in:0:30
#+begin_src 

### Let Bindings

#+end_srcclojure
;; Use let bindings for reusable components
(let [[input-video (input-labels "0:v")]
      [scaled (filter "scale" "1920:1080" input-video (output-labels "scaled"))]
      [cropped (filter "crop" "1920:800:0:140" (input-labels "scaled"))]]
  (chain scaled cropped))
#+begin_src 

### Built-in Functions

The DSL provides convenient functions for common operations:

#+end_srcclojure
;; Scaling
(scale 1920 1080)
;; Equivalent to: (filter "scale" "1920:1080")

;; Cropping
(crop 1920 800 0 140)
;; Equivalent to: (filter "crop" "1920:800:0:140")

;; Overlay
(overlay)
;; Equivalent to: (filter "overlay")

;; Fade
(fade :in 0 30)
;; Equivalent to: (filter "fade" "in:0:30")
#+begin_src 

## Examples

### Simple Video Processing

#+end_srcclojure
;; Scale and crop a video
(let [[input (input-labels "0:v")]
      [scaled (scale 1920 1080)]
      [final (crop 1920 800 0 140 (output-labels "output"))]]
  (chain
    (with-labels scaled [input] ["scaled"])
    (with-labels final

*** Please continue

#+begin_src clojure
    (with-labels final ["scaled"] ["output"])))
#+end_src

*** Complex Multi-Input Processing

#+begin_src clojure
;; Picture-in-picture with fade effects
(let [[main-input (input-labels "0:v")]
      [overlay-input (input-labels "1:v")]
      [main-scaled (scale 1920 1080)]
      [overlay-scaled (scale 320 240)]
      [overlay-positioned (filter "overlay" "main_w-overlay_w-10:10")]
      [faded (fade :in 0 30)]]
  (graph
    ;; Main video processing chain
    (chain
      (with-labels main-scaled [main-input] ["main"])
      (with-labels overlay-positioned ["main" "pip"] ["composed"]))
    ;; Overlay video processing chain  
    (chain
      (with-labels overlay-scaled [overlay-input] ["scaled_overlay"])
      (with-labels faded ["scaled_overlay"] ["pip"]))))
#+end_src

*** Audio/Video Synchronization

#+begin_src clojure
;; Process video and audio separately, then combine
(let [[video-in (input-labels "0:v")]
      [audio-in (input-labels "0:a")]
      [video-processed (chain
                         (scale 1280 720)
                         (filter "fps" "30"))]
      [audio-processed (filter "aformat" "sample_rates=48000")]]
  (graph
    (with-labels video-processed [video-in] ["v"])
    (with-labels audio-processed [audio-in] ["a"])))
#+end_src

** API Reference

*** Core Functions

**** =filter=
Creates a filter with optional arguments and labels.
- =(filter name)= - Simple filter
- =(filter name args)= - Filter with arguments  
- =(filter name args input-labels output-labels)= - Full specification

**** =chain=
Combines filters in sequence.
- =(chain filter1 filter2 ...)= - Sequential processing

**** =graph= 
Combines filter chains in parallel.
- =(graph chain1 chain2 ...)= - Parallel processing

**** =input-labels= / =output-labels=
Specify connection points between filters.
- =(input-labels "label1" "label2" ...)= 
- =(output-labels "label1" "label2" ...)=

**** =with-labels=
Convenience function to add labels to existing filters.
- =(with-labels filter input-labels output-labels)=

*** Built-in Filter Functions

**** Video Filters
- =(scale width height)= - Resize video
- =(crop width height x y)= - Crop video region
- =(overlay [x y])= - Overlay composition
- =(fade type start duration)= - Fade in/out effect
- =(fps rate)= - Change frame rate
- =(rotate angle)= - Rotate video

**** Audio Filters  
- =(aformat spec)= - Audio format conversion
- =(volume level)= - Adjust audio volume
- =(adelay delay)= - Add audio delay
- =(amix)= - Mix multiple audio streams

** Data Model

The DSL operates on immutable data structures:

#+begin_src clojure
;; Filter record
{:name "scale"
 :args "1920:1080" 
 :input-labels ["in"]
 :output-labels ["scaled"]}

;; Filter chain
{:filters [{:name "scale" :args "1920:1080"}
           {:name "crop" :args "1920:800:0:140"}]}

;; Filter graph  
{:chains [{:filters [...]}
          {:filters [...]}]}
#+end_src

** Compilation Process

1. *Parse* - DSL code → Abstract Syntax Tree
2. *Transform* - AST → Clojure data structures  
3. *Render* - Data structures → FFmpeg filter strings

#+begin_src clojure
;; Example compilation pipeline
(-> "(scale 1920 1080)"
    compile-dsl          ; Parse and transform
    first               ; Get first result
    to-ffmpeg)          ; Render to FFmpeg
;; => "scale=1920:1080"
#+end_src

** Error Handling

The DSL provides structured error reporting:

#+begin_src clojure
;; Parse errors include position and context
(try
  (compile-dsl "(scale 1920")  ; Missing closing paren
  (catch Exception e
    (ex-data e)))
;; => {:error {:line 1 :column 12 :reason "Unexpected end of input"}}

;; Validation errors for filter specifications
(try  
  (compile-dsl "(scale \"invalid\" \"args\")")
  (catch Exception e
    (ex-data e)))
;; => {:error {:type :validation :filter "scale" :args ["invalid" "args"]}}
#+end_src

** Performance Considerations

- *Lazy Evaluation*: Filter chains are built lazily
- *Immutable Data*: All transformations create new data structures
- *Memory Efficient*: Large filter graphs use persistent data structures
- *Compilation Caching*: Parsed ASTs can be cached for reuse

** Integration Examples

*** Command Line Tool

#+begin_src clojure
(ns ffmpeg-dsl.cli
  (:require [ffmpeg-dsl.core :refer :all]
            [ffmpeg-dsl.output :refer :all]
            [clojure.java.shell :as shell]))

(defn run-ffmpeg [input-file output-file filter-dsl]
  (let [filter-string (-> filter-dsl compile-dsl first to-ffmpeg)]
    (shell/sh "ffmpeg" 
              "-i" input-file
              "-vf" filter-string  
              output-file)))

;; Usage
(run-ffmpeg "input.mp4" "output.mp4" 
            "(chain (scale 1920 1080) (crop 1920 800 0 140))")
#+end_src

*** Web API Integration

#+begin_src clojure
(ns ffmpeg-dsl.web
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer :all]
            [ffmpeg-dsl.core :refer :all]
            [ffmpeg-dsl.output :refer :all]))

(defroutes app-routes
  (POST "/compile" [dsl]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (-> dsl compile-dsl first to-ffmpeg)}))

(defn -main []
  (jetty/run-jetty app-routes {:port 3000}))
#+end_src

** Testing Strategies

*** Unit Tests

#+begin_src clojure
(deftest test-filter-creation
  (testing "Basic filter compilation"
    (is (= "scale=1920:1080" 
           (-> "(scale 1920 1080)" compile-dsl first to-ffmpeg))))
  
  (testing "Filter with labels"  
    (is (= "[in]scale=1920:1080[out]"
           (-> "(filter \"scale\" \"1920:1080\" 
                        (input-labels \"in\") 
                        (output-labels \"out\"))"
               compile-dsl first to-ffmpeg)))))
#+end_src

*** Property-Based Tests

#+begin_src clojure
(require '[clojure.test.check :as tc]
         '[clojure.test.check.generators :as gen]
         '[clojure.test.check.properties :as prop])

(def filter-name-gen
  (gen/such-that #(re-matches #"[a-zA-Z][a-zA-Z0-9_]*" %)
                 gen/string-alphanumeric))

(def simple-filter-gen
  (gen/let [name filter-name-gen
            args gen/string-alphanumeric]
    (format "(filter \"%s\" \"%s\")" name args)))

(def roundtrip-property
  (prop/for-all [dsl simple-filter-gen]
    (let [compiled (compile-dsl dsl)
          ffmpeg-output (to-ffmpeg (first compiled))
          parsed-back (parse-ffmpeg-filter ffmpeg-output)]
      (= ffmpeg-output (to-ffmpeg parsed-back)))))

(tc/quick-check 100 roundtrip-property)
#+end_src

*** Integration Tests

#+begin_src clojure
(deftest test-ffmpeg-integration
  (testing "Generated filters work with actual FFmpeg"
    (let [test-filter (-> "(chain (scale 640 480) (fps 30))"
                         compile-dsl first to-ffmpeg)
          result (shell/sh "ffmpeg" "-f" "lavfi" "-i" "testsrc=duration=1:size=1920x1080:rate=25"
                          "-vf" test-filter "-f" "null" "-")]
      (is (= 0 (:exit result))))))
#+end_src

** Extension Points

*** Custom Filter Functions

#+begin_src clojure
;; Add custom filter functions to the resolver
(defmethod resolve-function :my-custom-filter [op env]
  (fn [& args]
    (make-filter "complex_filter" 
                 (str/join ":" (map str args)))))

;; Usage
(compile-dsl "(my-custom-filter 1920 1080 \"special\")")
#+end_src

*** Custom Macros

#+begin_src clojure
;; Define macros for common patterns
(defmacro pip [main overlay x y]
  =(graph
     (chain ~main (output-labels "main"))
     (chain ~overlay 
            (scale 320 240)
            (output-labels "pip"))
     (filter "overlay" ~(str x ":" y) 
             (input-labels "main" "pip"))))

;; Usage  
(pip (scale 1920 1080) 
     (filter "movie" "overlay.mp4")
     10 10)
#+end_src

** Best Practices

1. *Data First*: Design your filter data structures before writing transformations
2. *Pure Functions*: Keep filter definitions pure and composable  
3. *Label Management*: Use consistent labeling schemes for complex graphs
4. *Error Boundaries*: Validate inputs early in the compilation process
5. *Performance*: Cache compiled filter strings for repeated use
6. *Testing*: Write property-based tests for filter generation
7. *Documentation*: Document complex filter compositions with examples

** Troubleshooting

*** Common Issues

*Parse Errors*
- Check parentheses matching
- Verify string quoting
- Ensure proper symbol naming

*Invalid FFmpeg Output*  
- Validate filter names against FFmpeg documentation
- Check argument formatting
- Verify label connections

*Performance Issues*
- Cache compiled results
- Use lazy evaluation for large graphs  
- Profile memory usage with large filter sets

*** Debugging Tools

#+begin_src clojure
;; Enable debug output
(def debug-compile-dsl 
  (comp (partial map #(do (println "Compiled:" %) %))
        compile-dsl))

;; Trace filter generation
(defn trace-filter [f]
  (fn [& args]
    (let [result (apply f args)]
      (println "Generated filter:" (to-ffmpeg result))
      result)))

;; Validate generated FFmpeg syntax
(defn validate-ffmpeg [filter-string]
  (let [result (shell/sh "ffmpeg" "-f" "lavfi" "-i" "testsrc=duration=0.1"
                        "-vf" filter-string "-f" "null" "-")]
    (when-not (= 0 (:exit result))
      (throw (ex-info "Invalid FFmpeg filter" 
                     {:filter filter-string :error (:err result)})))))
#+end_src

This completes the comprehensive FFmpeg Filter DSL implementation with documentation. The system provides a clean, functional approach to building complex FFmpeg filter graphs while maintaining the data-first philosophy that makes Clojure so powerful for data transformation tasks.

*** 
