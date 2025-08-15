(ns bioscoop.domain.filter-showcase-test
  "Comprehensive showcase of all implemented filter specs"
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [bioscoop.domain.spec :as ds]
            [bioscoop.domain.specs.drawtext :as drawtext]
            [bioscoop.domain.specs.scale :as scale]
            [bioscoop.domain.specs.fade :as fade]))

(deftest complete-filter-showcase-test
  (testing "Enhanced drawtext filter with comprehensive parameters"
    (let [advanced-drawtext {:text "%{localtime}"
                             :fontfile "/usr/share/fonts/TTF/arial.ttf"
                             :fontsize 48
                             :x "(w-tw)/2" ; center horizontally
                             :y "h-th-20" ; 20px from bottom
                             :fontcolor "white"
                             :box true
                             :boxcolor "black@0.7"
                             :boxborderw 8
                             :borderw 3
                             :bordercolor "yellow"
                             :shadowx 3
                             :shadowy 3
                             :shadowcolor "gray"
                             :expansion "strftime" ; enable time formatting
                             :fix_bounds true
                             :text_align "center"
                             :enable "gte(t,5)"}] ; show after 5 seconds
      (is (s/valid? ::drawtext/drawtext advanced-drawtext))
      (is (>= (count advanced-drawtext) 16)))) ; has at least 16 parameters

  (testing "Audio crossfade filter with different curves"
    (let [crossfade-configs [{:duration 1.5}
                             {:duration 2.0 :overlap true}
                             {:duration 3.0 :curve1 "tri" :curve2 "qsin"}
                             {:duration 1.0 :overlap false :curve1 "log" :curve2 "cbr"}]]
      (doseq [config crossfade-configs]
        (is (s/valid? ::ds/acrossfade config)))))

  (testing "Audio compressor with various settings"
    (let [compressor-presets [{:threshold -12 :ratio 3.0} ; basic compression
                              {:threshold -18 :ratio 4.0 :attack 2 :release 20} ; punchy
                              {:level_in 1.2 :mode "downward" :threshold -15
                               :ratio 2.5 :attack 10 :release 100 :makeup 3
                               :knee 2.0 :link "average" :detection "rms"
                               :level_sc 1.0 :mix 0.8}]] ; comprehensive
      (doseq [preset compressor-presets]
        (is (s/valid? ::ds/acompressor preset)))))

  (testing "Audio echo with different configurations"
    (let [echo-configs [{:in_gain 0.6 :out_gain 0.3
                         :delays [1000] :decays [0.5]} ; simple echo
                        {:in_gain 0.8 :out_gain 0.5
                         :delays [500 1200] :decays [0.4 0.2]} ; double echo  
                        {:in_gain 0.7 :out_gain 0.4
                         :delays [300 600 1000 1500]
                         :decays [0.6 0.4 0.3 0.2]}]] ; complex echo
      (doseq [config echo-configs]
        (is (s/valid? ::ds/aecho config)))))

  (testing "Integration with existing filters"
    (is (s/valid? ::scale/scale {:w "1920" :h "1080" :flags "lanczos"}))
    (is (s/valid? ::ds/crop {:out_w "1280" :out_h "720" :x "(iw-ow)/2" :y "(ih-oh)/2"}))
    (is (s/valid? ::fade/fade {:type "out" :duration 2.0 :color "black"}))

    ;; Test that all filters coexist without conflicts
    (let [all-filters-valid? (every? true?
                                     [(s/valid? ::drawtext/drawtext {:text "test"})
                                      (s/valid? ::ds/acrossfade {:duration 1.0})
                                      (s/valid? ::ds/acompressor {:threshold -10 :ratio 2.0})
                                      (s/valid? ::ds/aecho {:in_gain 0.5 :out_gain 0.3
                                                            :delays [800] :decays [0.4]})
                                      (s/valid? ::scale/scale {:w "640" :h "480"})
                                      (s/valid? ::ds/crop {:out_w "320" :out_h "240"})
                                      (s/valid? ::fade/fade {:type "in" :start_frame 0 :nb_frames 15})])]
      (is all-filters-valid?))))
