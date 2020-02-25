(ns app.ui.leaflet.layers.d3svg-piechart-comparison
  (:require
    [com.fulcrologic.fulcro.components :refer [defsc]]
    [app.ui.leaflet.d3 :refer [d3SvgOverlay lngLat->Point bounds->circumcircleRadius colormap]]
    ["d3-shape" :as d3-shape]))

(defn d3DrawCallback [sel proj arc-data]
  (let [upd (-> sel
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

(defsc D3SvgPieChartComparison [this {:keys [react-key geojson]}]
  (d3SvgOverlay {:key react-key
                 :data (->> (for [[_ dataset] app.ui.leaflet.state.comparison
                                  :let [center (:center dataset)
                                        listing (:listing dataset)
                                        colors (map colormap (keys listing))
                                        radius (/ (reduce + (vals listing)) 50)
                                        arcs (js->clj ((js/d3.pie) (clj->js (vals listing)))
                                                      :keywordize-keys true)]]
                                 (map-indexed (fn [i color] {:geometry {:coordinates center}
                                                             :color color
                                                             :path ((d3-shape/arc) (clj->js (merge {:innerRadius (* 0.5 radius) :outerRadius radius}
                                                                                                   (nth arcs i))))})
                                              colors))
                            (apply concat))
                 :drawCallback d3DrawCallback}))
