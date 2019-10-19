(ns app.ui.leaflet.layers.extern.mvt
  (:require
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    ["react-leaflet" :refer [withLeaflet LayersControl.Overlay]]
    ["react-leaflet-vectorgrid" :as VectorGrid]))

(def layersControlOverlay (react-factory LayersControl.Overlay))
(def vectorGrid (react-factory (withLeaflet VectorGrid)))

(def mvtLayer
  (layersControlOverlay {:name "Graphhopper MVT example"}
    (vectorGrid {:type "protobuf" :url "http://localhost:8989/mvt/{z}/{x}/{y}.mvt" :subdomains ""
                 :vectorTileLayerStyles {"roads" (fn [properties zoom] {})}
                 :zIndex 1})))
