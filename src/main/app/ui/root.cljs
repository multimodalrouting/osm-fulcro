(ns app.ui.root
  (:require
    [clojure.string :as str]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li p h3 i button br]]
    [com.fulcrologic.semantic-ui.collections.table.ui-table :refer [ui-table]]
    [com.fulcrologic.semantic-ui.collections.table.ui-table-row :refer [ui-table-row]]
    [com.fulcrologic.semantic-ui.collections.table.ui-table-body :refer [ui-table-body]]
    [com.fulcrologic.semantic-ui.collections.table.ui-table-cell :refer [ui-table-cell]]
    [com.fulcrologic.semantic-ui.collections.table.ui-table-header :refer [ui-table-header]]
    [com.fulcrologic.semantic-ui.collections.table.ui-table-header-cell :refer [ui-table-header-cell]]
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
    ["react-leaflet-d3-svg-overlay" :as D3SvgOverlay]
    ["react-leaflet-sidetabs" :refer [Sidebar Tab]]
    ["react-leaflet-control" :default Control]))

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
(def sidebar (interop/react-factory Sidebar))
(def tab (interop/react-factory Tab))
(def control (interop/react-factory Control))

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
   (some->> bounds
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
                (.selectAll "a")
                (.data (clj->js arc-data)))]
       (-> (.enter upd)
           (.append "a")
           (.append "path")
           (.attr "transform" (fn [d] (let [[lng lat] (get-in (js->clj d :keywordize-keys true)
                                                              [:geometry :coordinates])
                                            latLng (clj->js {:lat lat :lng lng})
                                            point (.latLngToLayerPoint proj latLng)]
                                            (str "translate(" (.-x point) ","
                                                              (.-y point) ")"))))
           (.attr "d" #(:path (js->clj % :keywordize-keys true)))
           (.attr "fill" #(:color (js->clj % :keywordize-keys true)))
           (.attr "fill-opacity" "0.5")
           (.on "click" (fn [d i ds] (js/console.log (js->clj d)))))))

(defsc GeoJSON
  "a GeoJSON dataset"
  [this {:as props}]
  {:query [:type :features :timestamp :generator :copyright]})

(defmutation mutate-datasets [{:keys [path data]}]
  (action [{:keys [state]}]
    (swap! state update-in (concat [:leaflet/datasets] path)
                           (fn [d_orig d_new] (if (map? d_new) (merge d_orig d_new) d_new))
                           data)))

(defmutation mutate-layers [{:keys [path data]}]
  (action [{:keys [state]}]
    (swap! state update-in (concat [:leaflet/layers] path)
                           (fn [d_orig d_new] (if (map? d_new) (merge d_orig d_new) d_new))
                           data)))

(defmutation mutate-sidebar [params]
  (action [{:keys [state]}]
    (swap! state update-in [:leaflet/sidebar] merge (select-keys params [:tab :visible]))))

(defsc FulcroSidebar [this {:as props}]
  {:query [:leaflet/sidebar
           :leaflet/datasets
           :leaflet/layers]}
  (let [selected (get-in props [:leaflet/sidebar :tab] "help")
        onOpen #(comp/transact! this [(mutate-sidebar {:tab %})])
        onClose #(comp/transact! this [(mutate-sidebar {:visible false})])]
       (sidebar {:id "sidebar" :closeIcon "fa fa-window-close"
                 :selected selected :collapsed (nil? selected) 
                 :onOpen onOpen :onClose onClose}
         (tab {:id "help" :header "About" :icon (i {:classes ["fa" "fa-question"]})}
              (p {} "…"))
         (tab {:id "datasets" :header "Datasets" :icon (i {:classes ["fa" "fa-database"]})}
              (let [datasets (:leaflet/datasets props)]
                   (ui-table {}
                     (ui-table-header {}
                       (ui-table-row {}
                         (ui-table-header-cell {} "name")
                         (ui-table-header-cell {} "remote")
                         (ui-table-header-cell {} "query")
                         (ui-table-header-cell {} "entries")
                         (ui-table-header-cell {} "type")
                         (ui-table-header-cell {} "comment")))
                     (ui-table-body {}
                       (for [dataset datasets
                             :let [source (:source (val dataset))]]
                            (ui-table-row {:key (key dataset)}
                              (ui-table-cell {:key (str (key dataset) :n)} (str (key dataset)))
                              (ui-table-cell {:key (str (key dataset) :r)} (str (:remote source)))
                              (ui-table-cell {:key (str (key dataset) :q)} (if (vector? (:query source))
                                                                              (map-indexed (fn [i line] [(str line) (br {:key i})])
                                                                                           (:query source))
                                                                              (str (:query source))))
                              (ui-table-cell {:key (str (key dataset) :e)} (->> (get-in (val dataset) [:data :geojson :features])
                                                                                (group-by #(get-in % [:geometry :type]))
                                                                                (map (fn [[k vs]] {:count (count vs) :type k}))
                                                                                (sort-by :count >)
                                                                                (map-indexed (fn [i line] [(str (:count line) " " (:type line))
                                                                                                           (br {:key i})]))))
                              (ui-table-cell {:key (str (key dataset) :t)} (str (:type source)))
                              (ui-table-cell {:key (str (key dataset) :c)} (str (:comment source)))))))))
         (tab {:id "layers" :header "Layers" :icon (i {:classes ["fa" "fa-layer-group"]})}
              (let [layers (:leaflet/layers props)
                    overlays (->> (map (fn [layer] (->> (val layer)
                                                        :overlays
                                                        (map #(assoc % :layer (key layer)))))
                                       layers)
                                  (apply concat))]
                   (ui-table {}
                     (ui-table-header {}
                       (ui-table-row {}
                         (ui-table-header-cell {} "layer")
                         (ui-table-header-cell {} "class")
                         (ui-table-header-cell {} "dataset")
                         (ui-table-header-cell {} "filter")))
                     (ui-table-body {}
                       (for [overlay overlays
                             :let [k (hash overlay)]]
                            (ui-table-row {:key k}
                              (ui-table-cell {:key (str k :l)} (str (:layer overlay)))
                              (ui-table-cell {:key (str k :c)} (str (:class overlay)))
                              (ui-table-cell {:key (str k :d)} (str (:dataset overlay)))
                              (ui-table-cell {:key (str k :f)} (str (:filter overlay))))))))))))

(def fulcroSidebar (comp/factory FulcroSidebar))

(defn overlay-filter-rule->filter [filter-rule]
  (fn [feature]
      (->> (map (fn [[path set_of_accepted_vals]]
                    (set_of_accepted_vals (get-in feature path)))
                filter-rule)
      (reduce #(and %1 %2)))))

(defsc D3SvgPieChart [this {:keys [react-key features]}]
  (d3SvgOverlay {:key react-key
                 :data (let [centroid (js/d3.geoCentroid (clj->js {:type "FeatureCollection" :features features}))
                             bounds (js/d3.geoBounds (clj->js {:type "FeatureCollection" :features features}))]
                            (map (fn [[color ds]]
                                     (let [d (first ds)
                                           coords (get-in (js->clj d :keywordize-keys true)
                                                          [:geometry :coordinates])]
                                          {:geometry {:coordinates centroid}
                                           :color color
                                           :n (count ds)
                                           :bounds bounds}))
                                 (group-by color-by-accessibility features)))
                 :drawCallback d3DrawCallback}))

(def overlay-class->component {:d3SvgPieChart (comp/factory D3SvgPieChart)})

(defsc OSM
  [this props]
  {:query [:leaflet/datasets
           :leaflet/layers]}
  (leafletMap {:style {:height "100%" :width "100%"}
               :center [51.055 13.74] :zoom 12}
    ;(if-not (get-in props [:leaflet/sidebar :visible])
      (control {:position "bottomleft"}
        (button {:onClick #(comp/transact! this [(mutate-sidebar {:visible true})])
                 :style {:height "26px" :width "26px"}}
          (i {:classes ["fa" "fa-cog"]})));)
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
      #_(layersControlOverlay {:name "Graphhopper MVT example"}
        (vectorGrid {:type "protobuf" :url "http://localhost:8989/mvt/{z}/{x}/{y}.mvt" :subdomains ""
                     :vectorTileLayerStyles {"roads" (fn [properties zoom] {})}
                     :zIndex 1}))
      #_(layersControlOverlay {:name "GeoJSON example"}
        (if (:features geojson)
            (vectorGrid {:type "slicer" :zIndex 1 :data geojson})))
      #_(layersControlOverlay {:name "GeoJSON D3 Hexbin example"}
        (if (:features geojson)
            (hexbinLayer (merge {:data geojson} hexbinOptions))))
      (for [[layer-name layer] (:leaflet/layers props)]
           (layersControlOverlay {:key layer-name :name layer-name :checked true}
             (for [overlay (:overlays layer)
                   :let [dataset-features (get-in props [:leaflet/datasets (:dataset overlay) :data :geojson :features])
                         filtered-features (filter (overlay-filter-rule->filter (:filter overlay)) dataset-features)
                         component (overlay-class->component (:class overlay))]]
                  (if (and component filtered-features)
                      (component {:react-key (hash overlay)
                                  :features filtered-features}))))) )))

(def ui-osm (comp/factory OSM))

(defsc Root [this props]
  {:query (fn [] (into (comp/get-query OSM)
                       (comp/get-query FulcroSidebar)))}
  (div {:style {:width "100%" :height "100%"}}
    (if (get-in props [:leaflet/sidebar :visible])
        (fulcroSidebar props))
    (ui-osm (select-keys props [:leaflet/datasets :leaflet/layers]))))
