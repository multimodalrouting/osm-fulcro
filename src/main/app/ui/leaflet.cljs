(ns app.ui.leaflet
  (:require
    ;[app.routing.wip :refer [routing-example]]
    ;[app.ui.leaflet.sidebar :refer [FulcroSidebar fulcroSidebar controlOpenSidebar]]
    [app.ui.leaflet.layers :refer [overlay-class->component]]
    [app.ui.leaflet.layers.extern.base :refer [baseLayers]]
    [app.ui.leaflet.layers.extern.mvt :refer [mvtLayer]]
    [com.fulcrologic.fulcro.components :refer [defsc factory get-query]]
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    ["react-leaflet" :refer [withLeaflet Map LayersControl LayersControl.Overlay LayersControl.BaseLayer TileLayer]]
    [com.fulcrologic.fulcro.dom :as dom]
    [app.model.geofeatures :as gf]))

(def leafletMap (react-factory Map))
(def layersControl (react-factory LayersControl))
(def layersControlOverlay (react-factory LayersControl.Overlay))
(def layersControlBaseLayer (react-factory LayersControl.BaseLayer))
(def tileLayer (react-factory TileLayer))

(defn overlay-filter-rule->filter [filter-rule]
  (if (empty? filter-rule)
      (constantly true)
      (fn [feature]
          (->> (map (fn [[path set_of_accepted_vals]]
                        (set_of_accepted_vals (get-in feature path)))
                    filter-rule)
          (reduce #(and %1 %2))))))

(defsc Leaflet
  [this {:as props ::keys [id center zoom layers]
                   :keys [style]
                   :or {center [51.055 13.74]
                        zoom 12
                        style {:height "100%" :width "100%"}}}]
  {:ident (fn [] [:leaflet/id id])
   :query [::id ::layers ::center ::zoom
           ::gf/id :style]}
  ;(routing-example (get-in props [:leaflet/datasets :vvo :data :geojson]))

  (leafletMap {:style style
               :center center :zoom zoom}
    ;(controlOpenSidebar {})
    (layersControl {}
      ;mvtLayer

      (for [[layer-name layer] layers] [

           (if-let [base (:base layer)]
             (layersControlBaseLayer base
               (tileLayer (:tile base))))

           (let [overlays (->> (for [overlay (:overlays layer)
                                     :let [dataset-features (get-in props [::gf/id (:dataset overlay) ::gf/geojson :features])
                                           filtered-features (filter (overlay-filter-rule->filter (:filter overlay)) dataset-features)
                                           component (overlay-class->component (:class overlay))]]
                                    (if (and component (not (empty? filtered-features)))
                                        (component {:react-key (str layer-name (hash overlay) (hash filtered-features))
                                                    :geojson {:type "FeatureCollection" :features filtered-features}})))
                               (remove nil?))]
                (if-not (empty? overlays)
                        (layersControlOverlay {:key layer-name :name layer-name :checked (boolean (:prechecked layer))}
                                              ;; TODO why are not prechecked layers displayed?
                                              overlays)))]))))

(def leaflet (factory Leaflet))
