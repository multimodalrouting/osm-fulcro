(ns app.ui.root
  (:require
    [clojure.string :as str]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li p h3 button]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro-css.css :as css]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
    ["react-leaflet" :as ReactLeaflet :refer [withLeaflet Map
                                              LayersControl LayersControl.BaseLayer LayersControl.Overlay
                                              TileLayer Marker Popup]]
    ["react-leaflet-vectorgrid" :as VectorGrid]
    ["d3" :as d3]
    ["react-leaflet-d3" :refer [HexbinLayer]]
    ["react-leaflet-d3-svg-overlay" :refer [D3SvgOverlay]]))

(def leafletMap (interop/react-factory Map))
(def layersControl (interop/react-factory LayersControl))
(def layersControlBaseLayer (interop/react-factory LayersControl.BaseLayer))
(def layersControlOverlay (interop/react-factory LayersControl.Overlay))
(def tileLayer (interop/react-factory TileLayer))
(def vectorGrid (interop/react-factory (withLeaflet VectorGrid)))
(def hexbinLayer (interop/react-factory (withLeaflet HexbinLayer)))
(def d3SvgLayer (interop/react-factory (withLeaflet D3SvgOverlay)))

(def hexbinOptions {:colorScaleExtent [1 nil]
                    :radiusScaleExtent [1 nil]
                    :colorRange ["#ffffff" "#00ff00"]
                    :radiusRange [5 12]})

(defn color-by-accessibility [d]
  ({"yes" "green"
    "no" "red"
    "limited" "yellow"}
   (get-in (js->clj d :keywordize-keys true)
           [:properties :wheelchair])))

(defn d3DrawCallback [sel proj data]
  (let [upd (-> sel
                (.selectAll "path")
                (.data data))]
       (-> (.enter upd)
           (.append "path")
           (.attr "d" (.-pathFromGeojson proj))
           (.attr "fill" color-by-accessibility)
           (.attr "fill-opacity" "0.3"))
       (-> upd
           (.attr "stroke" "black")
           (.attr "stroke-width" (/ 1 (.-scale proj))))))

(defsc GeoJSON
  "a GeoJSON dataset"
  [this {:as props}]
  {:query [:type :features :timestamp :generator :copyright]
   :ident (fn [] [:data :vvo])
   :initial-state {:type {}}})

(defsc OSM
  [this {:osm/keys [geojson] :as props}]
  {:query [{:osm/geojson (comp/get-query GeoJSON)}]
   :ident (fn [] [:component/id :osm])
   :initial-state (fn [{:as props}] {:osm/geojson (comp/get-initial-state GeoJSON)})}
  (js/console.log geojson)
  (leafletMap {:style {:height "100%" :width "100%"}
               :center [51.055 13.74] :zoom 12}
    (layersControl {}
      (layersControlBaseLayer {:name "Esri Aearial" 
                               :checked true}
        (tileLayer {:url "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}.png"
                    :attribution "&copy; <a href=\"http://esri.com\">Esri</a>, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community"}))
      (layersControlBaseLayer {:name "OSM Tiles"}
        (tileLayer {:url "https://{s}.tile.osm.org/{z}/{x}/{y}.png"
                    :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}))
      (layersControlBaseLayer {:name "PublicTransport (MeMOMaps)"}
        (tileLayer {:url "https://tileserver.memomaps.de/tilegen/{z}/{x}/{y}.png"
                    :attribution "<a href=\"https://memomaps.de\">MeMOMaps"}))
      (layersControlBaseLayer {:name "PublicTransport (openptmap)"}
        (tileLayer {:url "http://openptmap.org/tiles/{z}/{x}/{y}.png"
                    :attribution "<a href=\"https://wiki.openstreetmap.org/wiki/Openptmap\">Openptmap"}))
      (layersControlBaseLayer {:name "NONE (only overlays)"}
        (tileLayer {:url ""}))
      (layersControlOverlay {:name "Graphhopper MVT example"}
        (vectorGrid {:type "protobuf" :url "http://localhost:8989/mvt/{z}/{x}/{y}.mvt" :subdomains ""
                     :vectorTileLayerStyles {"roads" (fn [properties zoom] {})}
                     :zIndex 1}))
      (layersControlOverlay {:name "GeoJSON example"}
        (if (:features geojson)
            (vectorGrid {:type "slicer" :zIndex 1 :data geojson})))
      (layersControlOverlay {:name "GeoJSON D3 Hexbin example"}
        (if (:features geojson)
            (hexbinLayer (merge {:data geojson #_#_:zIndex 1} hexbinOptions))))
      (layersControlOverlay {:name "GeoJSON D3 SvgLayer example" :checked true}
        (if (:features geojson)
            (d3SvgLayer {:data (filter #(and (#{"Point"} (get-in % [:geometry :type])))
                                       (:features geojson))
                         :drawCallback d3DrawCallback})))
)))

(def ui-osm (comp/factory OSM))

(defsc Root [this {:root/keys [top-osm]}]
  {:query [{:root/top-osm (comp/get-query OSM)}]
   :ident (fn [] [:component/id :ROOT])
   :initial-state {:root/top-osm {:osm {}}}}
  (ui-osm top-osm))
