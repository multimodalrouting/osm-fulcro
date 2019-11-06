(ns app.ui.leaflet
  (:require
    [app.routing.wip :refer [routing-example]]
    [app.ui.leaflet.sidebar :refer [FulcroSidebar fulcroSidebar controlOpenSidebar]]
    [app.ui.leaflet.layers :refer [overlay-class->component]]
    [app.ui.leaflet.state :as state]
    [app.ui.leaflet.layers.extern.base :refer [baseLayers]]
    [app.ui.leaflet.layers.extern.mvt :refer [mvtLayer]]
    [com.fulcrologic.fulcro.components :refer [defsc factory get-query]]
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    ["react-leaflet" :refer [withLeaflet Map LayersControl LayersControl.Overlay Marker Popup GeoJSON Polyline]]
    ["leaflet" :as l]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp]
    [app.model.geofeatures :as gf]))

(def leafletMap (react-factory Map))
(def layersControl (react-factory LayersControl))
(def layersControlOverlay (react-factory LayersControl.Overlay))
(def marker (react-factory Marker))
(def popup (react-factory Popup))
(def geoJson (react-factory GeoJSON))
(def polyline (react-factory Polyline))

(defn overlay-filter-rule->filter [filter-rule]
  (if (empty? filter-rule)
      (constantly true)
      (fn [feature]
          (->> (map (fn [[path set_of_accepted_vals]]
                        (set_of_accepted_vals (get-in feature path)))
                    filter-rule)
          (reduce #(and %1 %2))))))

(defsc StartStopMarker
  [this {:keys [lat lng]}]
  {:query         [:lat :lng]
   :initial-state {:lat 51 :lng 13}
   }
  (marker {:position [lat lng]
           :icon     (.icon. l (clj->js
                                 {
                                  :iconUrl     "https://cdn.rawgit.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png"
                                  :shadowUrl   "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png"
                                  :iconSize    [25, 41]
                                  :iconAnchor  [12, 41]
                                  :popupAnchor [1, -34]
                                  :shadowSize  [41, 41]
                                  }
                                 ))}))

(def startStopMarker (factory StartStopMarker {:key-fn #((hash [(:lat %) (:lng %)]))}))

(defsc Leaflet
  [this props]
  {:query [:gf/id
           :leaflet/datasets
           :leaflet/layers
           :selected/points
           :graphhopper/route
           :sensors/LOCATION
           ]}
  #_(routing-example (get-in props [:leaflet/datasets :vvo :data :geojson]))

  (leafletMap {:style {:height "100%" :width "100%"}
               :center [51.055 13.74] :zoom 12}
    (controlOpenSidebar {})
    (layersControl {:key (hash props)}
      baseLayers
      mvtLayer

      (for [[layer-name layer] (:leaflet/layers props)]
           (layersControlOverlay {:key layer-name :name layer-name :checked (boolean (:prechecked layer))}
             (for [overlay (:overlays layer)
                   :let [dataset-features (get-in props [::gf/id (:dataset overlay) ::gf/geojson :features])
                         filtered-features (filter (overlay-filter-rule->filter (:filter overlay)) dataset-features)
                         component (overlay-class->component (:class overlay))]]
                  (if (and component filtered-features)
                      (component {:react-key (str layer-name (hash overlay) (hash filtered-features))
                                  :geojson {:type "FeatureCollection" :features filtered-features}})))))
      (layersControlOverlay
        {:key "test-x-layer" :name "test" :checked true}
        #_(marker {:position [51.055 13.74]
                   :icons (.icon. l (clj->js
                                      {
                                       :iconUrl     "https://cdn.rawgit.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png"
                                       :shadowUrl   "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png"
                                       :iconSize    [25, 41]
                                       :iconAnchor  [12, 41]
                                       :popupAnchor [1, -34]
                                       :shadowSize  [41, 41]
                                       }
                                      ))
                   })
        (if (nil? (:selected/points props))
          []
          (for [point (:selected/points props)]
            (startStopMarker (:selected/latlng point))
            )
          )
        ))))

(def leaflet (factory Leaflet))

(defsc LeafletWithSidebar [this props]
  {:query (fn [] (into (get-query Leaflet)
                       (get-query FulcroSidebar)))}
  (dom/div {:style {:width "100%" :height "100%"}}
    (if (get-in props [:leaflet/sidebar :visible])
        (fulcroSidebar props))
    (leaflet (select-keys props [:leaflet/datasets :leaflet/layers :selected/points]))))

(def leafletWithSidebar (factory LeafletWithSidebar))
