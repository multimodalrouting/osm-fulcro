(ns app.ui.leaflet.layers
  (:require
    [com.fulcrologic.fulcro.components :refer [factory]]
    [app.ui.leaflet.layers.vectorGrid :refer [VectorGridOverlay]]
    [app.ui.leaflet.layers.hexbin :refer [Hexbin]]
    [app.ui.leaflet.layers.d3svg-piechart :refer [D3SvgPieChart]]))

(def overlay-class->component {:d3SvgPieChart (factory D3SvgPieChart)
                               :hexbin (factory Hexbin)
                               :vectorGrid (factory VectorGridOverlay)})
