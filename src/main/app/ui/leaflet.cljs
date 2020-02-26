(ns app.ui.leaflet
  (:require
    [app.ui.leaflet.layers :refer [overlay-class->component]]
    [app.ui.leaflet.tracking :refer [controlToggleTracking ControlToggleTracking]]
    [app.ui.leaflet.layers.extern.base :refer [baseLayers]]
    [app.ui.leaflet.layers.extern.mvt :refer [mvtLayer]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :refer [defsc factory get-query transact!]]
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    ["react-leaflet" :refer [withLeaflet Map LayersControl LayersControl.Overlay Marker Popup GeoJSON Polyline LayersControl.BaseLayer TileLayer]]
    ["leaflet" :as l]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp]
    [app.model.geofeatures :as gf]
    [app.model.osm-dataset :as osm-dataset :refer [OsmDataset]]))

(def leafletMap (react-factory Map))
(def layersControl (react-factory LayersControl))
(def layersControlOverlay (react-factory LayersControl.Overlay))
(def layersControlBaseLayer (react-factory LayersControl.BaseLayer))
(def tileLayer (react-factory TileLayer))
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

(defmutation refresh [{::keys [id] :keys [target]}]
  (action [{:keys [app state]}]
    (swap! state assoc-in [::id id ::center] [(-> target .getCenter .-lat) (-> target .getCenter .-lng)])
    (swap! state assoc-in [::id id ::zoom] (.getZoom target))))

(defsc Leaflet
  [this {:as props ::keys [id center zoom layers onMapClick]
                   :keys [style]
                   :or {center [51.055 13.74]
                        zoom 12
                        style {:height "100%" :width "100%"}}}]
  {:ident (fn [] [::id id])
   :query (fn [] [::id :tweak
                  ::layers ::center ::zoom
                  {::osm-dataset/root (get-query OsmDataset)}
                  ::gf/id :style])}

  (leafletMap {:style     style
               :center    center :zoom zoom
               :onClick   (fn [evt]
                            (if onMapClick
                              (onMapClick {:id :main :latlng {:lat (-> (.-latlng evt) .-lat) :lon (-> (.-latlng evt) .-lng)}})))
               :onMoveEnd #(transact! this [(refresh {::id :main :target (.-target %)})])
               :onZoomEnd #(transact! this [(refresh {::id :main :target (.-target %)})])}
    (layersControl {}

      (for [[layer-name layer] layers] [

           (if-let [base (:base layer)]
             (layersControlBaseLayer base
               (tileLayer (:tile base))))

           (if-let [layer-conf (:osm layer)]
                   ((overlay-class->component :d3SvgOSM) (merge
                                                           (if-not (get-in props [:tweak :redraw])
                                                                   {:center center ::zoom zoom})  ;; TODO be more intelligent when to update
                                                           {:elements (->> #_(component+query->tree this [{::osm-dataset/root (get-query OsmDataset)}])
                                                                           props
                                                                           ::osm-dataset/root
                                                                           ;; TODO here we want filter the datasets
                                                                           (map ::osm-dataset/elements)
                                                                           (apply concat))
                                                            :styles (:styles layer-conf)})))


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
