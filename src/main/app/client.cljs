(ns app.client
  (:require
    [app.application :refer [SPA]]
    [com.fulcrologic.fulcro.application :as app]
    [app.ui.root :as root]
    [com.fulcrologic.fulcro.networking.http-remote :as net]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro-css.css-injection :as cssi]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.inspect.inspect-client :as inspect]
    [com.fulcrologic.fulcro.rendering.keyframe-render :refer [render!]]
    [app.ui.root]))

(defn load! []
  (comp/transact! SPA [(app.ui.root/mutate-datasets {:data {:example-vvo {:source {:comment "VVO stops+lines"
                                                                                   :remote :pathom :type :geojson
                                                                                   :query :geojson.vvo/geojson}}
                                                            :example-overpass {:source {:remote :overpass :type :geojson
                                                                                        :query ["area[name=\"Dresden\"]->.city;"
                                                                                                "nwr(area.city)[operator=\"DVB\"]->.connections;"
                                                                                                "node.connections[public_transport=stop_position];"]}}}})])
  (df/load! SPA :geojson.vvo/geojson nil {:remote :pathom
                                          :target [:leaflet/datasets :example-vvo :data :geojson]})
  (df/load! SPA :geojson.vvo/geojson nil {:remote :overpass
                                          :target [:leaflet/datasets :example-overpass :data :geojson]}))

(comp/transact! SPA [(app.ui.root/mutate-layers {:data {:example-pie-chart {:overlays [{:class :d3SvgPieChart
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
  (dr/initialize! SPA)
  (app/mount! SPA root/Root "app" {:initialize-state? false})
  (load!))
