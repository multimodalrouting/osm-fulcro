(ns app.ui.leaflet.d3
  (:require
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    ["react-leaflet" :refer [withLeaflet]]
    ["d3" :as d3]
    ["react-leaflet-d3-svg-overlay" :as D3SvgOverlay]))

(def d3SvgOverlay (react-factory (withLeaflet (.-ReactLeaflet_D3SvgOverlay D3SvgOverlay))))

(defn lngLat->Point [proj [lng lat]]
  (.latLngToLayerPoint proj (clj->js {:lat lat :lng lng})))

(defn proj->bounds [proj]
  [[(-> proj .getBounds .-_southWest .-lng)
    (-> proj .getBounds .-_southWest .-lat)]
   [(-> proj .getBounds .-_northEast .-lng)
    (-> proj .getBounds .-_northEast .-lat)]])

(defn bounds->circumcircleRadius [proj bounds]
  (some->> bounds
           (map (partial lngLat->Point proj))
           (#(.subtract (first %) (second %)))
           (#(js/Math.sqrt (+ (* (.-x %) (.-x %))
                              (* (.-y %) (.-y %)))))
           (* 0.5)))

(defn filter-nodes-within-bounds [bounds elements]
  (filter #(and (< (-> bounds first first) (:lon %) (-> bounds second first))
                (< (-> bounds first second) (:lat %) (-> bounds second second)))
          elements))

(defn filter-ways-within-bounds [bounds elements]
  ;; Maybe we want keep the first nodes outside or only filter when all nodes are outside
  (->> elements
       (map (fn [element] (let [nodes (filter-nodes-within-bounds bounds (:nodes element))]
                               (if-not (empty? nodes)
                                       element))))
       (remove nil?)))

(defn stripe-pattern [defs id size color]
  "generates a stripe pattern as in the given defs
  it can be referenced using url(#id) in a shape, where
  id is the concrete id given here
  "
  (-> defs
      (.append "pattern")
      (.attr "id" id)
      (.attr "width" size)
      (.attr "height" size)
      (.attr "patternUnits" "userSpaceOnUse")
      (.attr "patternTransform" "rotate(60)")
      (.append "rect")
      (.attr "width" (/ size 2))
      (.attr "height" size)
      (.attr "transform" "translate(0,0)")
      (.attr "fill" color)))

(def colormap {"yes"     "#7ED321"                                     ;green
               "no"      "#D0021B"                                     ;red
               "limited" "#F5A623"                                     ;orange
               nil       "grey"})

(defn accessibility-patterns [svg size]
  (let [defs (.append svg "defs")]
    (for [[name color]
          [["yes" "#7ED321"]
           ["no" "#D0021B"]
           ["limited" "#F5A623"]
           ["other" "grey"]]]
      (stripe-pattern defs name size color)
      )))

(defn color-by-accessibility [d]
  ({"yes"     "#7ED321"                                     ;green
    "no"      "#D0021B"                                     ;red
    "limited" "#F5A623"                                     ;orange
    }
   (get-in (js->clj d :keywordize-keys true)
           [:properties :wheelchair])
   "grey"))

(defn feature->confident? [feature]
  (if-let [confidence (get-in feature [:properties :confidence])]
          (>= confidence 0.5)))
