(ns app.ui.leaflet.layers
  (:require
    [com.fulcrologic.fulcro.components :refer [factory]]
    [app.ui.leaflet.layers.vectorGrid :refer [VectorGridOverlay]]
    [app.ui.leaflet.layers.hexbin :refer [Hexbin]]
    [app.ui.leaflet.layers.d3svg-points :refer [D3SvgPoints]]
    [app.ui.leaflet.layers.d3svg-lines :refer [D3SvgLines]]
    [app.ui.leaflet.layers.d3svg-piechart :refer [D3SvgPieChart]]))

(def overlay-class->component {:vectorGrid (factory VectorGridOverlay)
                               :hexbin (factory Hexbin)
                               :d3SvgPoints (factory D3SvgPoints)
                               :d3SvgLines (factory D3SvgLines)
                               :d3SvgPieChart (factory D3SvgPieChart)})
