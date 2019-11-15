(ns app.ui.leaflet.sidebar
  (:require
    [com.fulcrologic.fulcro.components :refer [defsc factory transact!]]
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    ["react-leaflet-sidetabs" :refer [Sidebar Tab]]
    ["react-leaflet-control" :default Control]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.semantic-ui.collections.table.ui-table :refer [ui-table]]
    [com.fulcrologic.semantic-ui.collections.table.ui-table-row :refer [ui-table-row]]
    [com.fulcrologic.semantic-ui.collections.table.ui-table-body :refer [ui-table-body]]
    [com.fulcrologic.semantic-ui.collections.table.ui-table-cell :refer [ui-table-cell]]
    [com.fulcrologic.semantic-ui.collections.table.ui-table-header :refer [ui-table-header]]
    [com.fulcrologic.semantic-ui.collections.table.ui-table-header-cell :refer [ui-table-header-cell]]
    [app.model.geofeatures :as gf]))

(defmutation mutate-sidebar [params]
  (action [{:keys [state]}]
    (swap! state update-in [:leaflet/sidebar] merge (select-keys params [:tab :visible]))))

(def sidebar (react-factory Sidebar))
(def tab (react-factory Tab))
(def control (react-factory Control))

(defsc FulcroSidebar [this {:as props}]
  {:query [:leaflet/sidebar
           ;:leaflet/datasets
           :leaflet/layers
           ::gf/id ::gf/source]}
  (let [selected (get-in props [:leaflet/sidebar :tab] "help")
        onOpen #(transact! this [(mutate-sidebar {:tab %})])
        onClose #(transact! this [(mutate-sidebar {:visible false})])]
       (sidebar {:id "sidebar" :closeIcon "fa fa-window-close"
                 :selected selected :collapsed (nil? selected) 
                 :onOpen onOpen :onClose onClose}
         (tab {:id "help" :header "About" :icon (dom/i {:classes ["fa" "fa-question"]})}
              (dom/p {} "…"))
         (tab {:id "datasets" :header "Datasets" :icon (dom/i {:classes ["fa" "fa-database"]})}
              (let [datasets (::gf/id props)]
                   (ui-table {}
                     (ui-table-header {}
                       (ui-table-row {}
                         (ui-table-header-cell {} "name")
                         (ui-table-header-cell {} "remote")
                         (ui-table-header-cell {} "args")
                         (ui-table-header-cell {} "entries")
                         (ui-table-header-cell {} "type")
                         (ui-table-header-cell {} "comment")))
                     (ui-table-body {}
                       (for [dataset datasets
                             :let [source (::gf/source (val dataset))]]
                            (ui-table-row {:key (key dataset)}
                              (ui-table-cell {:key (str (key dataset) :n)} (str (key dataset)))
                              (ui-table-cell {:key (str (key dataset) :r)} (str (:remote source)))
                              (ui-table-cell {:key (str (key dataset) :q)} (if (vector? (:args source))
                                                                              (map-indexed (fn [i line] [(str line) (dom/br {:key i})])
                                                                                           (:args source))
                                                                              (str (:args source))))
                              (ui-table-cell {:key (str (key dataset) :e)} (->> (get-in (val dataset) [::gf/geojson :features])
                                                                                (group-by #(get-in % [:geometry :type]))
                                                                                (map (fn [[k vs]] {:count (count vs) :type k}))
                                                                                (sort-by :count >)
                                                                                (map-indexed (fn [i line] [(str (:count line) " " (:type line))
                                                                                                           (dom/br {:key i})]))))
                              (ui-table-cell {:key (str (key dataset) :t)} (str (:type source)))
                              (ui-table-cell {:key (str (key dataset) :c)} (str (:comment source)))))))))
         (tab {:id "layers" :header "Layers" :icon (dom/i {:classes ["fa" "fa-layer-group"]})}
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

(def fulcroSidebar (factory FulcroSidebar))

(defsc ControlOpenSidebar [this props]
  (control {:position "bottomleft"}
    (dom/button {:onClick #(transact! this [(mutate-sidebar {:visible true})])
             :style {:height "26px" :width "26px"}}
      (dom/i {:classes ["fa" "fa-cog"]}))))

(def controlOpenSidebar (factory ControlOpenSidebar))
