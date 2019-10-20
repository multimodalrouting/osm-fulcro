(ns app.ui.leaflet.d3
  (:require
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    ["react-leaflet" :refer [withLeaflet]]
    ["d3" :as d3]
    ["react-leaflet-d3-svg-overlay" :as D3SvgOverlay]))

(def d3SvgOverlay (react-factory (withLeaflet (.-ReactLeaflet_D3SvgOverlay D3SvgOverlay))))

(defn lngLat->Point [proj [lng lat]]
  (.latLngToLayerPoint proj (clj->js {:lat lat :lng lng})))

(defn bounds->circumcircleRadius [proj bounds]
   (some->> bounds
            (map (partial lngLat->Point proj))
            (#(.subtract (first %) (second %)))
            (#(js/Math.sqrt (+ (* (.-x %) (.-x %))
                               (* (.-y %) (.-y %)))))
            (* 0.5)))


(defn color-by-accessibility [d]
  ({"yes" "green"
    "no" "red"
    "limited" "yellow"}
   (get-in (js->clj d :keywordize-keys true)
           [:properties :wheelchair])
   "blue"))
