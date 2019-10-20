(ns app.ui.leaflet.layers.d3svg-piechart
  (:require
    [com.fulcrologic.fulcro.components :refer [defsc]]
    [app.ui.leaflet.d3 :refer [d3SvgOverlay lngLat->Point bounds->circumcircleRadius color-by-accessibility]]
    ["d3-shape" :as d3-shape]))

(defn d3DrawCallback [sel proj data]
  (let [radius (->> (js->clj data :keywordize-keys true)
                     first
                     :bounds
                     (bounds->circumcircleRadius proj))
        numbers (map :n (js->clj data :keywordize-keys true))
        arcs ((js/d3.pie) (clj->js numbers))
        arc-data (map-indexed (fn [i d] (assoc d :path ((d3-shape/arc) (clj->js (merge {:innerRadius (* 0.5 radius) :outerRadius radius}
                                                                                       (js->clj (nth arcs i) :keywordize-keys true))))))
                              (js->clj data :keywordize-keys true))
        upd (-> sel
                (.selectAll "a")
                (.data (clj->js arc-data)))]
       (-> (.enter upd)
           (.append "a")
           (.append "path")
           (.attr "transform" (fn [d] (let [point (lngLat->Point proj (get-in (js->clj d :keywordize-keys true) [:geometry :coordinates]))]
                                           (str "translate(" (.-x point) "," (.-y point) ")"))))
           (.attr "d" #(:path (js->clj % :keywordize-keys true)))
           (.attr "fill" #(:color (js->clj % :keywordize-keys true)))
           (.attr "fill-opacity" "0.5")
           (.on "click" (fn [d i ds] (js/console.log (js->clj d)))))))

(defsc D3SvgPieChart [this {:keys [react-key geojson]}]
  (d3SvgOverlay {:key react-key
                 :data (let [centroid (js/d3.geoCentroid (clj->js geojson))
                             bounds (js/d3.geoBounds (clj->js geojson))]
                            (map (fn [[color ds]]
                                     (let [d (first ds)
                                           coords (get-in (js->clj d :keywordize-keys true)
                                                          [:geometry :coordinates])]
                                          {:geometry {:coordinates centroid}
                                           :color color
                                           :n (count ds)
                                           :bounds bounds}))
                                 (group-by color-by-accessibility (:features geojson))))
                 :drawCallback d3DrawCallback}))
