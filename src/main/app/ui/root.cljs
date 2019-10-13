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
    ["d3-shape" :as d3-shape]
    ["react-leaflet-d3" :refer [HexbinLayer]]
    ["react-leaflet-d3-svg-overlay" :as D3SvgOverlay]))

(defn arc [kwargs]
  ((d3-shape/arc) (clj->js kwargs)))

(def leafletMap (interop/react-factory Map))
(def layersControl (interop/react-factory LayersControl))
(def layersControlBaseLayer (interop/react-factory LayersControl.BaseLayer))
(def layersControlOverlay (interop/react-factory LayersControl.Overlay))
(def tileLayer (interop/react-factory TileLayer))
(def vectorGrid (interop/react-factory (withLeaflet VectorGrid)))
(def hexbinLayer (interop/react-factory (withLeaflet HexbinLayer)))
(def d3SvgOverlay (interop/react-factory (withLeaflet (.-ReactLeaflet_D3SvgOverlay D3SvgOverlay))))

(def hexbinOptions {:colorScaleExtent [1 nil]
                    :radiusScaleExtent [1 nil]
                    :colorRange ["#ffffff" "#00ff00"]
                    :radiusRange [5 12]})

(defn color-by-accessibility [d]
  ({"yes" "green"
    "no" "red"
    "limited" "yellow"}
   (get-in (js->clj d :keywordize-keys true)
           [:properties :wheelchair])
   "blue"))

(defn lngLat->Point [proj [lng lat]]
  (.latLngToLayerPoint proj (clj->js {:lat lat :lng lng})))

(defn bounds->circumcircleRadius [proj bounds]
   (->> bounds
        (map (partial lngLat->Point proj))
        (#(.subtract (first %) (second %)))
        (#(js/Math.sqrt (+ (* (.-x %) (.-x %))
                           (* (.-y %) (.-y %)))))
        (* 0.5)))

(defn d3DrawCallback [sel proj data]
  (let [radius (->> (js->clj data :keywordize-keys true)
                     first
                     :bounds
                     (bounds->circumcircleRadius proj))
        numbers (map :n (js->clj data :keywordize-keys true))
        arcs ((js/d3.pie) (clj->js numbers))
        arc-data (map-indexed (fn [i d] (assoc d :path (arc (merge {:innerRadius (* 0.5 radius) :outerRadius radius}
                                                                   (js->clj (nth arcs i) :keywordize-keys true)))))
                              (js->clj data :keywordize-keys true))
        upd (-> sel
                (.selectAll "path")
                (.data (clj->js arc-data)))]
       (-> (.enter upd)
           (.append "path")
           (.attr "transform" (fn [d] (let [[lng lat] (get-in (js->clj d :keywordize-keys true)
                                                              [:geometry :coordinates])
                                            latLng (clj->js {:lat lat :lng lng})
                                            point (.latLngToLayerPoint proj latLng)]
                                            (str "translate(" (.-x point) ","
                                                              (.-y point) ")"))))
           (.attr "d" #(:path (js->clj % :keywordize-keys true)))
           (.attr "fill" #(:color (js->clj % :keywordize-keys true)))
           (.attr "fill-opacity" "0.5"))))

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
      (layersControlBaseLayer {:name "Esri Aearial" :checked true}
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
      (layersControlBaseLayer {:name "NONE (only overlays)"
                               :checked true}
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
      (layersControlOverlay {:name "GeoJSON D3 SvgOverlay example" :checked true}
        (if (:features geojson)
            (d3SvgOverlay {:data (->> (:features geojson)
                                      (filter #(and (#{"Point"} (get-in % [:geometry :type]))
                                                    (#{"stop_position"} (get-in % [:properties :public_transport]))))
                                      ;; group features
                                      ((fn [features]
                                           (let [centroid (js/d3.geoCentroid (clj->js {:type "FeatureCollection" :features features}))
                                                 bounds (js/d3.geoBounds (clj->js {:type "FeatureCollection" :features features}))]
                                                (map (fn [[color ds]]
                                                         (let [d (first ds)
                                                               coords (get-in (js->clj d :keywordize-keys true)
                                                                              [:geometry :coordinates])]
                                                              {:geometry {:coordinates centroid}
                                                               :color color
                                                               :n (count ds)
                                                               :bounds bounds}))
                                                     (group-by color-by-accessibility features))))))
                           :drawCallback d3DrawCallback}))) )))

(def ui-osm (comp/factory OSM))

(defsc Root [this {:root/keys [top-osm]}]
  {:query [{:root/top-osm (comp/get-query OSM)}]
   :ident (fn [] [:component/id :ROOT])
   :initial-state {:root/top-osm {:osm {}}}}
  (ui-osm top-osm))
