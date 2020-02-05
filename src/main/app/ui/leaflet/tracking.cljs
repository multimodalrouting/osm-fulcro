(ns app.ui.leaflet.tracking
  (:require
    [app.background-geolocation :as bg-geo]
    [com.fulcrologic.fulcro.components :refer [defsc factory transact!]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    ["react-leaflet-control" :default Control]
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp]
    [app.ui.leaflet.state :refer [mutate-datasets mutate-layers]]
    [app.application :refer [SPA]]
    ))

(def control (react-factory Control))

(defn load-all! []

  ;; TODO don't load :leaflet/datasets but use ::gf/source from pathom-remote instead
  (transact! SPA [(mutate-datasets {:data {:vvo {:source {:comment "VVO stops+lines"
                                                          :remote :pathom :type :geojson}}
                                           :overpass-example {:source {:remote :overpass :type :geojson
                                                                       :args ["area[name=\"Dresden\"]->.city;"
                                                                              "nwr(area.city)[operator=\"DVB\"]->.connections;"
                                                                              "node.connections[public_transport=stop_position];"]}}
                                           :mvt-loschwitz {:source {:remote :mvt :type :geojson
                                                                    :args {:uri "http://localhost:8989/mvt/13/4410/2740.mvt"
                                                                           :layer "roads"} }}}})])

  (transact! SPA [(mutate-layers {:data {:hexbin-example {:overlays [{:class :hexbin
                                                                      :dataset :vvo
                                                                      :filter {[:geometry :type] #{"Point"}
                                                                               [:properties :public_transport] #{"stop_position"}}}]}
                                         :vectorGrid-loschwitz {:prechecked false
                                                                :overlays [{:class :vectorGrid
                                                                            :dataset :mvt-loschwitz}]}
                                         :vectorGrid-vvo-connections {:overlays [{:class :vectorGrid
                                                                                  :dataset :vvo
                                                                                  :filter {[:geometry :type] #{"LineString"}}}]}
                                         :lines-vvo-connections {:prechecked false
                                                                 :overlays [{:class :d3SvgLines
                                                                             :dataset :vvo
                                                                             :filter {[:geometry :type] #{"LineString"}}}]}
                                         :points-vvo-stops {:prechecked false
                                                            :overlays [{:class :d3SvgPoints
                                                                        :dataset :vvo
                                                                        :filter {[:geometry :type] #{"Point"}
                                                                                 [:properties :public_transport] #{"stop_position"}}}]}
                                         :pieChart-vvo-stops {:prechecked false
                                                              :overlays [{:class :d3SvgPieChart
                                                                          :dataset :vvo
                                                                          :filter {[:geometry :type] #{"Point"}
                                                                                   [:properties :public_transport] #{"stop_position"}}}]}
                                         :routes {:prechecked false
                                                  :overlays [{:class :d3SvgLines
                                                              :dataset :routes}]}}})]))


(defsc ControlToggleTracking [this {:keys [enabled triggerActivities isPaused] :as props}]
  {:query [:enabled :triggerActivities :isPaused]}
       (control {:position "topleft"}
                (dom/button {
                             :onClick #(load-all!)
                             :style   {:height "26px" :width "26px"}
                             }
                            (dom/i {:classes ["fa" "fa-arrow-down"]}))
                (dom/button {:onClick (fn []
                                        (if enabled
                                          (comp/transact! this [(bg-geo/stop-tracking nil)])
                                          (comp/transact! this [(bg-geo/start-tracking nil)])
                                          )
                                        )
                             :style   {:height "26px" :width "26px"}}
                            (dom/i {:classes ["fa" (if enabled "fa-square" "fa-circle")]}))

                (when (and enabled (not isPaused))
                  (dom/button {:onClick (fn []
                                          (comp/transact! this [(comp/transact! this [(bg-geo/pause-tracking nil)])]))
                               :style   {:height "26px" :width "26px"}}
                              (dom/i {:classes ["fa" "fa-pause"]})))
                ))

(def controlToggleTracking (factory ControlToggleTracking))

