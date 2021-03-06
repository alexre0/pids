(ns pids.core
    (:require-macros [cljs.core.async.macros :refer [go go-loop]])
    (:require [rum.core :as rum]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]]))
(enable-console-print!)

(defn randstr [length]
  (let [possible "abcdefghijklmnopqrstuvwxyz"]
    (loop [accStr ""] (if (< (count accStr) length)
      (recur (str accStr (nth possible (rand-int (count possible))))) accStr))))

(defn getStatus [pid type]
  (go (:body (<! (http/get (str "http://stream.nbcsports.com/data/" type pid ".json") {:with-credentials? false})))))

(defn setAll [dataAtom statsAtom filterParam]
  (let [allPids (into [] (concat (range 200000 200100) (range 202600 204200) (range 204200 205200)))
         startTime (.now js/performance)]
   (go-loop [pidsArr allPids]
       (let [currentPid (first pidsArr)
             status (:status (<! (getStatus currentPid "event_status_")))
             sourceUrl (if (= filterParam status) (:sourceUrl (first (:videoSources (<! (getStatus currentPid "live_sources_"))))) nil)]
       (reset! statsAtom (str (* 100 (/ (- (count allPids) (count (rest pidsArr))) (count allPids))) "%"))
       (if (not-empty sourceUrl) (swap! dataAtom conj (str currentPid " " sourceUrl)))
       (if (not-empty pidsArr) (recur (rest pidsArr))
           (reset! statsAtom (str "Found " (count @dataAtom) " of " (count allPids) " pids in " (/ (- (.now js/performance) startTime) 1000) " seconds")))))))

(rum/defc progressBar < rum/reactive [percentStr]
  [:div {:style {:width "700px" :border "1px solid black"}}
  [:div {:style {:width (or (rum/react percentStr) 0)
                 :background-color "#DDD"
                 :height "20px"}} (rum/react percentStr)]])

(rum/defc linesDisp < rum/reactive [atomAsVector]
  [:div (for [line (rum/react atomAsVector)] [:div line])])

(rum/defc displayContainer []
  (let [dataAtom (atom [])
        statsAtom (atom nil)]
     [:div {:style {:width "100%" :height "100%"}}
     [:button {:on-click #(setAll dataAtom statsAtom "live")} "get"]
     [:button {:on-click #(reset! dataAtom [])} "clear"]
     (progressBar statsAtom)
     (linesDisp dataAtom)]))

(defn init []
  (rum/mount (displayContainer) (.getElementById js/document "app")))
(init)
