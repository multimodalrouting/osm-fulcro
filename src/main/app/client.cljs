(ns app.client
  (:require
    [app.application :refer [SPA]]
    [app.ui.root :as root]
    [app.ui.leaflet :refer [mutate-datasets mutate-layers]]
    [com.fulcrologic.fulcro.components :refer [transact!]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [com.fulcrologic.fulcro-css.css-injection :as cssi]
    [taoensso.timbre :as log]))

(defn load-all! []
  (transact! SPA [(mutate-datasets {:data {:example-vvo {:source {:comment "VVO stops+lines"
                                                                  :remote :pathom :type :geojson
                                                                  :query :geojson.vvo/geojson}}
                                           :example-overpass {:source {:remote :overpass :type :geojson
                                                                       :query ["area[name=\"Dresden\"]->.city;"
                                                                               "nwr(area.city)[operator=\"DVB\"]->.connections;"
                                                                               "node.connections[public_transport=stop_position];"]}}}})])
  (load! SPA :geojson.vvo/geojson nil {:remote :pathom
                                       :target [:leaflet/datasets :example-vvo :data :geojson]})
  (load! SPA :geojson.vvo/geojson nil {:remote :overpass
                                       :target [:leaflet/datasets :example-overpass :data :geojson]}))

(transact! SPA [(mutate-layers {:data {:example-vectorGrid {:prechecked true
                                                            :overlays [{:class :vectorGrid
                                                                        :dataset :example-vvo
                                                                        :filter {[:geometry :type] #{"Point"}
                                                                                 [:properties :public_transport] #{"stop_position"}}}]}
                                       :example-hexbin {:overlays [{:class :hexbin
                                                                    :dataset :example-vvo
                                                                    :filter {[:geometry :type] #{"Point"}
                                                                             [:properties :public_transport] #{"stop_position"}}}]}
                                       :example-pieChart {:prechecked true
                                                          :overlays [{:class :d3SvgPieChart
                                                                      :dataset :example-vvo
                                                                      :filter {[:geometry :type] #{"Point"}
                                                                               [:properties :public_transport] #{"stop_position"}}}]}}})])

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
  (load-all!))
