(ns bioscoop.ffmpeg-test
  (:require [clojure.test :refer [deftest is testing]]
            [bioscoop.ffmpeg :as ffmpeg]))

(deftest build-command-without-hw-devices
  (testing "existing command shape is preserved"
    (is (= [ffmpeg/ffmpeg-bin "-y" "-nostdin" "-hide_banner"
            "-i" "in.mp4"
            "-filter_complex" "scale=1280:-2[v]"
            "output.mp4"]
           (ffmpeg/build-command
            {:filtergraph "scale=1280:-2[v]"}
            ["in.mp4"])))))

(deftest build-command-with-hw-devices
  (testing "init and filter devices are emitted before inputs"
    (is (= [ffmpeg/ffmpeg-bin "-y" "-nostdin" "-hide_banner"
            "-init_hw_device" "vulkan=vk:0"
            "-filter_hw_device" "vk"
            "-i" "in.mp4"
            "-filter_complex" "hwupload=derive_device=vulkan[out]"
            "-map" "[out]"
            "-g" "12"
            "-c:v" "libx264" "-preset" "slow" "-crf" "18" "-pix_fmt" "yuv420p"
            "output.mp4"]
           (ffmpeg/build-command
            {:filtergraph "hwupload=derive_device=vulkan[out]"
             :maps ["-map" "[out]"]
             :gop 12
             :preset [:h264]
             :init-hw-device "vulkan=vk:0"
             :filter-hw-device "vk"}
            ["in.mp4"]))))

  (testing "multiple init devices are emitted in order"
    (is (= [ffmpeg/ffmpeg-bin "-y" "-nostdin" "-hide_banner"
            "-init_hw_device" "vaapi=va:/dev/dri/renderD128"
            "-init_hw_device" "qsv=hw@va"
            "-filter_hw_device" "hw"
            "-i" "in.mp4"
            "-filter_complex" "null"
            "output.mp4"]
           (ffmpeg/build-command
            {:filtergraph "null"
             :init-hw-device ["vaapi=va:/dev/dri/renderD128"
                              "qsv=hw@va"]
             :filter-hw-device "hw"}
            ["in.mp4"])))))
