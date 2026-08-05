(ns bioscoop.resolve-var-test
  (:require [clojure.test :refer [deftest testing is]]
            [bioscoop.macro :refer [bioscoop]]
            [bioscoop.dsl :refer [last-errors]]
            [bioscoop.domain.records :refer [make-filtergraph make-filterchain make-filter]]
            [bioscoop.config :refer [*warn-on-shadowing*]])
  (:import [bioscoop.domain.records FilterGraph]))

;; Captured at namespace load time, when *ns* is guaranteed to be this
;; file's own namespace. This project's test runner invokes deftest bodies
;; without rebinding *ns* back to the test namespace first — confirmed via
;; (println *ns*) inside a deftest, which prints the runner's own ns, not
;; bioscoop.resolve-var-test. this-ns keeps var setup targeting the same
;; namespace `bioscoop` itself resolves against (it captures *ns* the same
;; way, at macro-expansion/file-load time — see bioscoop.macro).

(def this-ns *ns*)

(deftest data-vars-resolve-to-real-values
  (testing "vectors and maps are data in Clojure, so top-level Vars holding
            them resolve to their actual value inside bioscoop — bad types
            are then caught for free by the filter's own spec, no special
            casing needed in the resolver"
    (intern this-ns 'plain-vec [1 2 3])
    (intern this-ns 'plain-map {:width 1 :height 1})
    (bioscoop (scale plain-vec 1080))
    (is (some #(= :invalid-parameter (:error-type (ex-data %))) @last-errors))
    (bioscoop (scale plain-map 1080))
    (is (nil? (seq @last-errors)))
    (ns-unmap this-ns 'plain-vec)
    (ns-unmap this-ns 'plain-map)))

(deftest function-vars-stay-on-the-dynamic-dispatch-path
  (testing "a top-level Var holding a real function is left as a name for
            resolve-function to look up and invoke — it must NOT be
            substituted for 'its value' the way data is"
    (intern this-ns 'plain-fn
            (fn [data]
              (make-filtergraph
               [(make-filterchain
                 [(make-filter "drawtext" {:text (str (:text data))})])])))
    (bioscoop (plain-fn {:text "hello"}))
    (is (nil? (seq @last-errors)))
    (ns-unmap this-ns 'plain-fn)))


(deftest shadowed-top-level-var-is-still-ambiguous
  (testing "a symbol that's both a local DSL binding and a top-level Var is
            genuinely ambiguous, regardless of what the Var holds"
    (intern this-ns 'shadow-me {:width 640 :height 480})
    (binding [*warn-on-shadowing* true]
      (bioscoop (let [shadow-me {:width 320 :height 240}]
                  (scale shadow-me))))
    (is (some #(= :ambiguous-symbol (:error-type (ex-data %))) @last-errors))
    (ns-unmap this-ns 'shadow-me)))

(comment (deftest data-var-in-call-head-position-errors-cleanly
           (testing "a data-holding Var used where a filter/function name is
            expected fails with a clear error, not a raw ClassCastException"
             (intern this-ns 'plain-map {:width 1 :height 1})
             (bioscoop (plain-map "x"))
             (is (some #(= :op-not-callable (:error-type (ex-data %))) @dsl/last-errors))
             (ns-unmap this-ns 'plain-map))))

(comment (deftest multimethod-vars-also-stay-on-the-dynamic-dispatch-path
  (testing "MultiFns are callable but don't satisfy fn? — must be
            recognized explicitly or they'd be wrongly treated as data"
    (let [mm (clojure.lang.MultiFn. "plain-multi" (fn [_] :default) :default #'clojure.core/global-hierarchy)]
      (.addMethod mm :default (fn [data] (records/make-filtergraph
                                           [(records/make-filterchain
                                             [(records/make-filter "drawtext" {:text (str (:text data))})])])))
      (intern this-ns 'plain-multi mm))
    (bioscoop (plain-multi {:text "hi"}))
    (is (nil? (seq @dsl/last-errors)))
    (ns-unmap this-ns 'plain-multi))))



(deftest resolution-is-independent-of-ambient-ns-at-call-time
  (testing "the original bug, reproduced directly: rebind *ns* to something
            unrelated right before invoking an already-compiled bioscoop
            call — simulating exactly what this test runner does to every
            deftest body — and confirm resolution still succeeds"
    (intern this-ns 'drift-data {:text "still resolves"})
    (intern this-ns 'drift-fn
            (fn [data]
              (make-filtergraph
               [(make-filterchain
                 [(make-filter "drawtext" {:text (str (:text data))})])])))
    (let [run-it (fn [] (bioscoop (drift-fn drift-data)))]
      (binding [*ns* (find-ns 'clojure.core)]
        (run-it)
        (is (nil? (seq @last-errors)))))
    (ns-unmap this-ns 'drift-data)
    (ns-unmap this-ns 'drift-fn)))


(def received (atom nil))

(defn fake-title [data]
  (reset! received data)
  (bioscoop (drawtext {:text (str (:text data))})))

(def config-map {:text "hello" :x 10 :y 20})

(deftest top-level-map-var-resolves-to-real-value
  (bioscoop (fake-title config-map))
  (is (= config-map @bioscoop.resolve-var-test/received))
  (is (empty? @last-errors)))

(deftest shadowing-any-top-level-var-is-still-ambiguous
  (binding [*warn-on-shadowing* true]
    (bioscoop (let [config-map {:text "shadowed"}]
                (fake-title config-map))))
  (is (some #{:ambiguous-symbol} (map (comp :error-type ex-data) @last-errors))))

(comment (deftest data-var-in-call-head-position-errors-cleanly
           (let [result (bioscoop (config-map "x"))]
             (is (instance? FilterGraph result))
             (is (some #{:op-not-callable} (map (comp :error-type ex-data) @last-errors))))))

(deftest filter-names-and-macros-still-dispatch-by-string
  ;; sanity check that `when`/`eval`/built-in filter names still hit the
  ;; case-dispatch / string-keyed lookup paths, not the new value branch
  (let [result (bioscoop (when true (drawtext {:text "ok"})))]
    (is (instance? FilterGraph result))
    (is (empty? @last-errors))))

(deftest cross-namespace-defgraph-reference-resolves
  (testing "a defgraph'd var in another namespace, referenced by a
            qualified symbol, resolves via find-var"
    (let [fixture-ns (create-ns 'bioscoop.fixture-ns)]
      (intern fixture-ns 'imported-graph
              (make-filtergraph
               [(make-filterchain
                 [(make-filter "drawtext" {:text "cross-ns"})])]))
      (let [result (bioscoop bioscoop.fixture-ns/imported-graph)]
        (is (instance? FilterGraph result))
        (is (empty? @last-errors)))
      (remove-ns 'bioscoop.fixture-ns))))
