(ns app.ui.leaflet.layers.vectorGrid
  (:require
    [com.fulcrologic.fulcro.components :refer [defsc]]
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    ["react-leaflet" :refer [withLeaflet]]
    ["react-leaflet-vectorgrid" :as VectorGrid]))

(def vectorGrid (react-factory (withLeaflet VectorGrid)))

(defsc VectorGridOverlay [this {:keys [react-key geojson]}]
  (vectorGrid {:key react-key :type "slicer" :zIndex 1 :data geojson}))
