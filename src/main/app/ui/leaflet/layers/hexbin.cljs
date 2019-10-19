(ns app.ui.leaflet.layers.hexbin
  (:require
    [com.fulcrologic.fulcro.components :refer [defsc]]
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    ["react-leaflet" :refer [withLeaflet]]
    ["react-leaflet-d3" :refer [HexbinLayer]]))

(def hexbinLayer (react-factory (withLeaflet HexbinLayer)))

(def hexbinOptions {:colorScaleExtent [1 nil]
                    :radiusScaleExtent [1 nil]
                    :colorRange ["#ffffff" "#00ff00"]
                    :radiusRange [5 12]})

(defsc Hexbin [this {:keys [react-key geojson]}]
  (hexbinLayer (merge {:key react-key :data geojson} hexbinOptions)))
