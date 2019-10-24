(ns app.client
  (:require
    [app.application :refer [SPA]]
    [app.ui.root :as root]
    [app.ui.leaflet.state :refer [mutate-datasets mutate-layers]]
    [com.fulcrologic.fulcro.components :refer [transact!]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro-css.css-injection :as cssi]
    [taoensso.timbre :as log]))

(defn load-map! []
  (if (nil? js/navigator.graphhopper)
    nil
    (.loadMap js/navigator.graphhopper "sachsen-latest" (fn [success] (prn success))))
  )

(defn load-overpass! []
  (transact! SPA [(mutate-datasets {:data {
                                           :overpass-example {:source {:remote :overpass :type :geojson
                                                                       :query ["area[name=\"Dresden\"]->.city;"
                                                                               "nwr(area.city)[operator=\"DVB\"]->.connections;"
                                                                               "node.connections[public_transport=stop_position];"]}}
                                           }}  )])
  )

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
                                         :vectorGrid-loschwitz {:prechecked true
                                                                :overlays [{:class :vectorGrid
                                                                            :dataset :mvt-loschwitz}]}
                                         :vectorGrid-vvo-connections {:overlays [{:class :vectorGrid
                                                                                  :dataset :vvo
                                                                                  :filter {[:geometry :type] #{"LineString"}}}]}
                                         :lines-vvo-connections {:prechecked true
                                                                 :overlays [{:class :d3SvgLines
                                                                             :dataset :vvo
                                                                             :filter {[:geometry :type] #{"LineString"}}}]}
                                         :points-vvo-stops {:prechecked true
                                                            :overlays [{:class :d3SvgPoints
                                                                        :dataset :vvo
                                                                        :filter {[:geometry :type] #{"Point"}
                                                                                 [:properties :public_transport] #{"stop_position"}}}]}
                                         :pieChart-vvo-stops {:prechecked true
                                                              :overlays [{:class :d3SvgPieChart
                                                                          :dataset :vvo
                                                                          :filter {[:geometry :type] #{"Point"}
                                                                                   [:properties :public_transport] #{"stop_position"}}}]}
                                         :routes {:prechecked true
                                                  :overlays [{:class :d3SvgLines
                                                              :dataset :routes}]}}})]))

(defn ^:export refresh []
  (js/console.clear)
  (log/info "Hot code Remount")
  (cssi/upsert-css "componentcss" {:component root/Root})
  (app/mount! SPA root/Root "app"))

(defn ^:export init []
  (log/info "Application starting.")
  (cssi/upsert-css "componentcss" {:component root/Root})
  ;(inspect/app-started! SPA)
  (app/set-root! SPA root/Root {:initialize-state? true})
  ;(dr/initialize! SPA)
  (app/mount! SPA root/Root "app" {:initialize-state? false})
  #_(load-all!)
  #_(load-map!)
  )
