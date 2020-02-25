(ns app.ui.leaflet.layers
  (:require
    [com.fulcrologic.fulcro.components :refer [factory]]
    [app.ui.leaflet.layers.vectorGrid :refer [VectorGridOverlay]]
    [app.ui.leaflet.layers.hexbin :refer [Hexbin]]
    [app.ui.leaflet.layers.d3svg-osm :refer [D3SvgOSM style-topo]]
    [app.ui.leaflet.layers.d3svg-points :refer [D3SvgPoints]]
    [app.ui.leaflet.layers.d3svg-label-points :refer [D3SvgLabelPoints]]
    [app.ui.leaflet.layers.d3svg-lines :refer [D3SvgLines]]
    [app.ui.leaflet.layers.d3svg-styled-lines :refer [D3SvgStyledLines]]
    [app.ui.leaflet.layers.d3svg-piechart :refer [D3SvgPieChart]]
    [app.ui.leaflet.layers.d3svg-piechart-comparison :refer [D3SvgPieChartComparison]]))

(def overlay-class->component {:vectorGrid (factory VectorGridOverlay)
                               :hexbin (factory Hexbin)
                               :d3SvgOSM (factory D3SvgOSM)
                               :d3SvgPoints (factory D3SvgPoints)
                               :d3SvgLabelPoints (factory D3SvgLabelPoints)
                               :d3SvgLines (factory D3SvgLines)
                               :d3SvgStyledLines (factory D3SvgStyledLines)
                               :d3SvgPieChart (factory D3SvgPieChart)
                               :d3SvgPieChartComparison (factory D3SvgPieChartComparison)})

(def example-layers {nil {:base {:name "NONE (only overlays)"
                                 :tile {:url ""}}}
                     :aerial {:base {:name "Esri Aearial"
                                     :tile {:url "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}.png"
                                     :attribution "&copy; <a href=\"http://esri.com\">Esri</a>, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community"}}}
                     :osm {:base {:name "OSM Tiles"
                                  :tile {:url "https://{s}.tile.osm.org/{z}/{x}/{y}.png"
                                         :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}}}
                     :memo {:base {:name "PublicTransport (MeMOMaps)"
                                   :tile {:url "https://tileserver.memomaps.de/tilegen/{z}/{x}/{y}.png"
                                          :attribution "<a href=\"https://memomaps.de\">MeMOMaps"}}}
                     :openpt {:base {:name "PublicTransport (openptmap)"
                                     :tile {:url "http://openptmap.org/tiles/{z}/{x}/{y}.png"
                                            :attribution "<a href=\"https://wiki.openstreetmap.org/wiki/Openptmap\">Openptmap"}}}


                     :topo {:osm {:name "Topography of OsmJson-Dataset"
                                  :datasets nil ;;all in osm-dataset/root
                                  :style style-topo}}


                     :hexbin-example {:overlays [{:class :hexbin
                                                  :dataset :vvo-small
                                                  :filter {[:geometry :type] #{"Point"}
                                                           [:properties :public_transport] #{"stop_position"}}}]}
                     :vectorGrid-loschwitz {:prechecked true
                                            :overlays [{:class :vectorGrid
                                                        :dataset :mvt-loschwitz}]}
                     :vectorGrid-trachenberger {:overlays [{:class :vectorGrid
                                                            :dataset :trachenberger
                                                            :filter {[:geometry :type] #{"LineString"}}}]}
                     :lines-vvo-connections {:prechecked true
                                             :overlays [{:class :d3SvgLines
                                                         :dataset :vvo
                                                         :filter {[:geometry :type] #{"LineString"}}}]}
                     :points-vvo-stops {:prechecked true
                                        :overlays [{:class :d3SvgPoints #_:d3SvgLabelPoints
                                                    :dataset :vvo-small
                                                    :filter {[:geometry :type] #{"Point"}
                                                             [:properties :public_transport] #{"stop_position"}}}]}
                     :pieChart-stop-positions {:prechecked true
                                               :overlays [{:class :d3SvgPieChartComparison
                                                           :dataset :vvo-small  ;; TODO
                                                           #_#_:dataset :stop-positions}]}
                     #_#_:routinggraph {:prechecked true
                                    :overlays [{:class :d3SvgStyledLines
                                                :dataset :routinggraph}]}
                     #_#_:routes {:prechecked true
                              :overlays [{:class :d3SvgStyledLines
                                          :dataset :routes}]}
                     :isochrones {:overlays [{:class :d3SvgLines
                                              :dataset :isochrones}]}})
