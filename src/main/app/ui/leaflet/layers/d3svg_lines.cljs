(ns app.ui.leaflet.layers.d3svg-lines
  (:require
    [com.fulcrologic.fulcro.components :refer [defsc]]
    [app.ui.leaflet.d3 :refer [d3SvgOverlay lngLat->Point color-by-accessibility]]))

(defn d3DrawCallback [sel proj data]
  (let [radius 3
        upd (-> sel
                (.selectAll "a")
                (.data (clj->js data)))]
       (-> (.enter upd)
           (.append "a")
           (.append "polyline")
           (.attr "points" (fn [d] (->> (get-in (js->clj d :keywordize-keys true) [:geometry :coordinates])
                                        (map #(lngLat->Point proj %))
                                        (map #(str (.-x %) "," (.-y %)))
                                        (clojure.string/join " "))))
           (.attr "stroke" (fn [d] (get-in (js->clj d :keywordize-keys true) [:properties :style :stroke] "white")
                           #_#(color-by-accessibility (js->clj % :keywordize-keys true))))
           (.attr "stroke-width" (fn [d] (get-in (js->clj d :keywordize-keys true) [:properties :style :stroke-width] 1)))
           (.attr "fill-opacity" 0.5)
           (.attr "fill" "none")
           (.on "click" (fn [d i ds] (js/console.log (js->clj d)))))))

(defsc D3SvgLines [this {:keys [react-key geojson]}]
  (d3SvgOverlay {:key react-key
                 :data (:features geojson)
                 :drawCallback d3DrawCallback}))
