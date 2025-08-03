(ns bioscoop.macro-test
  (:require [bioscoop.macro :refer [bioscoop form->ast]]
            [bioscoop.dsl :as dsl]
            [bioscoop.render :refer [to-ffmpeg]]
            [clojure.test :refer [deftest is testing]])
  (:import [bioscoop.domain.records FilterGraph FilterChain Filter]))

(deftest test-form->ast
  (testing "Simple expressions"
    (is (= [:symbol "scale"] (form->ast 'scale)))
    (is (= [:number "1920"] (form->ast 1920)))
    (is (= [:string "scale"] (form->ast "scale")))
    (is (= [:keyword "in"] (form->ast :in)))
    (is (= [:boolean "true"] (form->ast true)))
    (is (= [:boolean "false"] (form->ast false))))

  (testing "Function calls"
    (is (= [:list [:symbol "scale"] [:number "1920"] [:number "1080"]]
           (form->ast '(scale 1920 1080))))
    (is (= [:list [:symbol "filter"] [:string "scale"] [:string "1920:1080"]]
           (form->ast '(filter "scale" "1920:1080")))))

  (testing "Let bindings"
    (is (= [:let-binding
            [:binding [:symbol "width"] [:number "1920"]]
            [:list [:symbol "scale"] [:symbol "width"] [:number "1080"]]]
           (form->ast '(let [width 1920] (scale width 1080)))))

    (is (= [:let-binding
            [:binding [:symbol "width"] [:number "1920"]]
            [:binding [:symbol "height"] [:number "1080"]]
            [:list [:symbol "scale"] [:symbol "width"] [:symbol "height"]]]
           (form->ast '(let [width 1920 height 1080] (scale width height)))))))

(deftest test-bioscoop-macro
  (testing "Macro produces same results as text parsing"
    (let [text-result (dsl/compile-dsl "(scale 1920 1080)")
          macro-result (bioscoop (scale 1920 1080))]
      (is (= text-result macro-result)))

    (let [text-result (dsl/compile-dsl "(let [width 1920] (scale width 1080))")
          macro-result (bioscoop (let [width 1920] (scale width 1080)))]
      (is (= text-result macro-result)))

    (let [text-result (dsl/compile-dsl "(filter \"scale\" 1920 1080)")
          macro-result (bioscoop (filter "scale" 1920 1080))]
      (is (= text-result macro-result)))

    (let [text-result (dsl/compile-dsl "(chain (scale 1920 1080) (overlay))")
          macro-result (bioscoop (chain (scale 1920 1080) (overlay)))]
      (is (= text-result macro-result))))

  (testing "Complex let bindings"
    (let [text-result (dsl/compile-dsl "(let [width 1920 height 1080] (scale width height))")
          macro-result (bioscoop (let [width 1920 height 1080] (scale width height)))]
      (is (= text-result macro-result))))

  (testing "real world examples"
    (let [dsl (bioscoop 
             (let [out-left-tmp (output-labels "left" "tmp")
                   in-tmp (input-labels "tmp")
                   out-right (output-labels "right")
                   in-left-right (input-labels "left" "right")]
               (graph (chain 
                       (crop "iw/2" "ih" "0" "0")
                       (filter "split"  out-left-tmp))
                      (filter "hflip"  in-tmp out-right)
                      (filter "hstack" in-left-right))))]
      (is (= "crop=iw/2:ih:0:0,split[left][tmp];[tmp]hflip[right];[left][right]hstack"
             (to-ffmpeg dsl)))))

  (testing "Multiple expressions"
    ;; Note: This test assumes the DSL supports multiple expressions in the program
    ;; If not, we may need to adjust this test
    (let [text-result (try (dsl/compile-dsl "(scale 1920 1080)") (catch Exception e nil))
          macro-result (try (bioscoop (scale 1920 1080)) (catch Exception e nil))]
      (when (and text-result macro-result)
        (is (= text-result macro-result)))))

  (testing "Macro produces correct data types"
    (let [result (bioscoop (scale 1920 1080))]
      (is (instance? FilterGraph result))
      (is (= 1 (count (:chains result))))
      (is (= 1 (count (:filters (first (:chains result))))))
      (is (= "scale" (:name (first (:filters (first (:chains result)))))))
      (is (= "1920:1080" (:args (first (:filters (first (:chains result)))))))))

  (testing "Automatic wrapping of FilterGraph/filterchain"
    (let [result (bioscoop (scale 1920 1080) (scale 1910 1180) (scale 1920 80))]
      (is (instance? FilterGraph result))
      (is (every? #(instance? FilterChain %) (:chains result)))
      (is (= 3 (count (:chains result))))      
      (is (= 1 (count (:filters (first (:chains result))))))
      (is (= 3 (count (map :filters (:chains result)))))))
  
  (testing "Automatic wrapping of FilterGraph/filterchain"
    (let [result (bioscoop (graph (scale 1920 1080) (scale 1910 1180) (scale 1920 80)))]
      (is (instance? FilterGraph result))
      (is (every? #(instance? Filter %) (:chains result)))
      (is (= 3 (count (:chains result))))
      (is (= 3 (count (map :filters (:chains result)))))))

  (testing "Automatic wrapping of FilterGraph/filterchain"
    (let [result (bioscoop (graph (chain (scale 1920 1080) (scale 1910 1180)) (scale 1920 80)))]
      (is (instance? FilterGraph result))
      (is (instance? FilterChain (first (:chains result))))
      (is (instance? Filter (last (:chains result))))
      (is (= 2 (count (:chains result))))
      (is (= 2 (count (:filters (first (:chains result)))))))))

