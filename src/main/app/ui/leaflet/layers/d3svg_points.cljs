(ns app.ui.leaflet.layers.d3svg-points
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
           (.append "circle")
           (.attr "cx" (fn [d] (.-x (lngLat->Point proj (get-in (js->clj d :keywordize-keys true) [:geometry :coordinates])))))
           (.attr "cy" (fn [d] (.-y (lngLat->Point proj (get-in (js->clj d :keywordize-keys true) [:geometry :coordinates])))))
           (.attr "r" 4)
           (.attr "fill" #(color-by-accessibility (js->clj % :keywordize-keys true)))
           (.attr "fill-opacity" 0.5)
           (.on "click" (fn [d i ds] (js/console.log (js->clj d)))))))

(defsc D3SvgPoints [this {:keys [react-key geojson]}]
  (d3SvgOverlay {:key react-key
                 :data (:features geojson)
                 :drawCallback d3DrawCallback}))
