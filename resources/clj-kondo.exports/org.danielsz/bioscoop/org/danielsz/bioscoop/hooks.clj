(ns org.danielsz.bioscoop.hooks
  (:require [clj-kondo.hooks-api :as api]))

;; DSL functions available inside bioscoop macro
(def dsl-functions
  #{'hflip
    'pad
    'smptehdbars
    'crop
    'haldclutsrc
    'scale
    'fade
    'xstack
    'rgbtestsrc
    'testsrc
    'smptebars
    'tile
    'split
    'trim
    'xfade
    'concat
    'yuvtestsrc
    'vstack
    'zoompan
    'overlay
    'hstack
    'color
    'drawtext
    'format
    'loop
    'fps
    'chain
    'graph
    'setdar
    'setsar
    'input-labels
    'output-labels
    'compose
    'setpts})

(defn dsl-function? [sym]
  (dsl-functions sym))

(defn transform-dsl-form
  "Transform DSL forms to prevent linting errors while preserving variable references"
  [form]
  (cond
    ;; Handle let bindings - these work like normal let, so pass through
    (and (api/list-node? form)
         (seq (:children form))
         (api/token-node? (first (:children form)))
         (= 'let (api/sexpr (first (:children form)))))
    form

    ;; Handle function calls
    (and (api/list-node? form)
         (seq (:children form))
         (api/token-node? (first (:children form))))
    (let [fn-name (api/sexpr (first (:children form)))
          args (rest (:children form))]
      (if (dsl-function? fn-name)
        ;; Transform DSL function to 'do' with arguments to preserve variable references
        ;; This ensures bound variables are still considered "used" by clj-kondo
        (if (seq args)
          (api/list-node (cons (api/token-node 'do) args))
          ;; If no args, just use a 'do' with nil
          (api/list-node (list (api/token-node 'do) (api/token-node 'nil))))
        ;; For unknown functions, let normal linting handle them
        form))

    ;; All other forms pass through unchanged
    :else form))

(defn walk-form
  "Recursively walk and transform nested forms"
  [form]
  (let [transformed (transform-dsl-form form)]
    (cond
      (api/list-node? transformed)
      (api/list-node (map walk-form (:children transformed)))

      (api/vector-node? transformed)
      (api/vector-node (map walk-form (:children transformed)))

      (api/map-node? transformed)
      (api/map-node (map walk-form (:children transformed)))

      :else transformed)))

(defn bioscoop
  "Hook for the bioscoop macro to provide proper linting.
   
   This transforms the macro call into a 'do' block where DSL function calls
   are replaced with identity calls to prevent unresolved-symbol warnings,
   while preserving the structure for other linting checks."
  [{:keys [node]}]
  (let [args (rest (:children node))
        ;; Transform each argument recursively
        transformed-args (map walk-form args)
        ;; Wrap in 'do' which accepts any number of forms
        new-node (api/list-node
                  (cons (api/token-node 'do) transformed-args))]
    {:node new-node}))

(defn defgraph
  "Hook for the defgraph macro to provide proper linting.
   
   This handles the defgraph macro which wraps bioscoop forms with additional
   registration and var definition. We transform it to a regular 'def' form
   to prevent unresolved symbol warnings for the graph name."
  [{:keys [node]}]
  (let [children (:children node)
        ;; defgraph has the pattern: (defgraph name & body-forms)
        name-node (second children)
        body-forms (drop 2 children)
        ;; Transform the body forms (which will be passed to bioscoop)
        transformed-body (map walk-form body-forms)
        ;; Create a 'def' form to properly declare the var
        new-node (api/list-node
                  [(api/token-node 'def)
                   name-node
                   (api/list-node
                    (cons (api/token-node 'do)
                          transformed-body))])]
    {:node new-node}))
