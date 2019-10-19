(ns app.ui.leaflet
  (:require
    [app.ui.leaflet.sidebar :refer [mutate-sidebar FulcroSidebar fulcroSidebar controlOpenSidebar]]
    [app.ui.leaflet.layers :refer [overlay-class->component]]
    [app.ui.leaflet.layers.extern.base :refer [baseLayers]]
    [app.ui.leaflet.layers.extern.mvt :refer [mvtLayer]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :refer [defsc factory get-query]]
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    ["react-leaflet" :refer [withLeaflet Map LayersControl LayersControl.Overlay]]
    [com.fulcrologic.fulcro.dom :as dom]))

(def leafletMap (react-factory Map))
(def layersControl (react-factory LayersControl))
(def layersControlOverlay (react-factory LayersControl.Overlay))


(defmutation mutate-datasets [{:keys [path data]}]
  (action [{:keys [state]}]
    (swap! state update-in (concat [:leaflet/datasets] path)
                           (fn [d_orig d_new] (if (map? d_new) (merge d_orig d_new) d_new))
                           data)))

(defmutation mutate-layers [{:keys [path data]}]
  (action [{:keys [state]}]
    (swap! state update-in (concat [:leaflet/layers] path)
                           (fn [d_orig d_new] (if (map? d_new) (merge d_orig d_new) d_new))
                           data)))


(defn overlay-filter-rule->filter [filter-rule]
  (fn [feature]
      (->> (map (fn [[path set_of_accepted_vals]]
                    (set_of_accepted_vals (get-in feature path)))
                filter-rule)
      (reduce #(and %1 %2)))))

(defsc Leaflet
  [this props]
  {:query [:leaflet/datasets
           :leaflet/layers]}
  (leafletMap {:style {:height "100%" :width "100%"}
               :center [51.055 13.74] :zoom 12}
    (controlOpenSidebar {})
    (layersControl {:key (hash props)}
      baseLayers
      mvtLayer

      (for [[layer-name layer] (:leaflet/layers props)]
           (layersControlOverlay {:key layer-name :name layer-name :checked (boolean (:prechecked layer))}
             (for [overlay (:overlays layer)
                   :let [dataset-features (get-in props [:leaflet/datasets (:dataset overlay) :data :geojson :features])
                         filtered-features (filter (overlay-filter-rule->filter (:filter overlay)) dataset-features)
                         component (overlay-class->component (:class overlay))]]
                  (if (and component filtered-features)
                      (component {:react-key (str layer-name (hash overlay) (hash filtered-features))
                                  :geojson {:type "FeatureCollection" :features filtered-features}}))))) )))

(def leaflet (factory Leaflet))

(defsc LeafletWithSidebar [this props]
  {:query (fn [] (into (get-query Leaflet)
                       (get-query FulcroSidebar)))}
  (dom/div {:style {:width "100%" :height "100%"}}
    (if (get-in props [:leaflet/sidebar :visible])
        (fulcroSidebar props))
    (leaflet (select-keys props [:leaflet/datasets :leaflet/layers]))))

(def leafletWithSidebar (factory LeafletWithSidebar))
