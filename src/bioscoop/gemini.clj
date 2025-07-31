(ns bioscoop.gemini
  (:require [instaparse.core :as insta]))


(def whitespace
  (insta/parser
   "whitespace = #'\\s+'"))

;; BNF Grammar for the DSL
(def video-dsl-grammar
  "
  S = expression+
  expression = list | number | string | identifier
  list = <'('> (op | expression)+ <')'>
  op = 'def' | 'fn' | 'let' | 'if' | filter-name
  filter-name = 'crop' | 'scale' | 'fade' | 'trim' | 'overlay' | 'concat' | 'rotate' | 'blur' | 'eq' | 'volume'
  identifier = #'[a-zA-Z_][a-zA-Z0-9_]*'
  number = #'[0-9]+(\\.[0-9]+)?'
  string = #'\"(?:\\\\\"|[^\"])*\"'
  ")

;; Create the parser from the grammar
(def dsl-parser (insta/parser video-dsl-grammar :auto-whitespace whitespace))

;; --- Compilation Logic ---

(defmulti compile-node (fn [node env] (first node)))

;; Helper to compile a sequence of expressions
(defn- compile-expressions [expressions env]
  (map #(compile-node % env) expressions))

;; Compile a number literal
(defmethod compile-node :number [[_ value] _]
  value)

;; Compile a string literal, removing quotes
(defmethod compile-node :string [[_ value] _]
  (subs value 1 (dec (count value))))

;; Compile an identifier by looking it up in the environment
(defmethod compile-node :identifier [[_ name] env]
  (or (get env (symbol name))
      (throw (Exception. (str "Undefined variable: " name)))))

;; Compile a filter expression
(defmethod compile-node :filter-name [[_ name] env]
  name)

;; Compile a list (function call or special form)
(defmethod compile-node :list [node env]
  (let [[_ & children] node
        [op & args] (compile-expressions children env)]
    (cond
      ;; --- Special Forms ---
      (= op "def")
      (let [[var-name value] args]
        (swap! (:bindings env) assoc (symbol var-name) value)
        "") ; `def` produces no direct output

      (= op "let")
      (let [[bindings & body] args
            let-bindings (apply hash-map bindings)
            local-env (update env :bindings merge let-bindings)]
        (last (compile-expressions body local-env)))

      ;; --- Filter Functions ---
      (contains? #{"crop" "scale" "fade" "trim" "rotate" "blur" "eq" "volume"} op)
      (str op "=" (clojure.string/join ":" args))

      ;; Default function call (assumes it's a filter)
      :else
      (throw (Exception. (str "Unknown operator or function: " op))))))

;; Main compilation function
(defn compile-dsl [input-string]
  (let [parse-tree (dsl-parser input-string)]
    (if (insta/failure? parse-tree)
      (throw (Exception. (str "Parse error: " (pr-str (insta/get-failure parse-tree)))))
      (let [env {:bindings (atom {})}]
        (->> (compile-expressions (:content parse-tree) env)
             (filter not-empty)
             (clojure.string/join ","))))))


;; --- Examples ---

(defn run-examples []
  (println "--- Example 1: Simple Scaling ---")
  (println "DSL: (scale 1280 720)")
  (println "FFmpeg: " (compile-dsl "(scale 1280 720)"))
  (println)

  (println "--- Example 2: Cropping and Fading ---")
  (println "DSL: (crop 640 480 100 50) (fade \"in\" 0 30)")
  (println "FFmpeg: " (compile-dsl "(crop 640 480 100 50) (fade \"in\" 0 30)"))
  (println)

  (println "--- Example 3: Using 'def' and 'let' ---")
  (let [dsl-code "(def width 1920) (def height 1080) (let [w (/ width 2) h (/ height 2)] (scale w h))"]
    (println "DSL:" dsl-code)
    ;; Note: For this to run, we'd need to implement arithmetic operators
    ;; This is a conceptual example showing the structure.
    ;; A real implementation would require adding +, -, *, / to the compiler.
    (println "Conceptual FFmpeg: scale=960:540")
    (println "This example highlights the potential for more complex scripts.")
    (println))
  
  (println "--- Example 4: Complex Chain ---")
  (let [dsl-code "(scale 1920 1080) (crop 1280 720 320 180) (rotate \"PI/6\") (blur 1.5)"]
    (println "DSL:" dsl-code)
    (println "FFmpeg:" (compile-dsl dsl-code))))

;; To run the examples from the REPL:
;; (run-examples)
