(ns bioscoop.resolve-var-test
  (:require [clojure.test :refer [deftest testing is]]
            [bioscoop.macro :refer [bioscoop]]
            [bioscoop.dsl :refer [last-errors]]
            [bioscoop.domain.records :refer [make-filtergraph make-filterchain make-filter]])
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
  (bioscoop (let [config-map {:text "shadowed"}]
              (fake-title config-map)))
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
