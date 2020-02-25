(ns app.ui.leaflet.layers.d3svg-osm
  (:require
    [com.fulcrologic.fulcro.components :refer [defsc get-query]]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [app.ui.leaflet.d3 :refer [d3SvgOverlay lngLat->Point proj->bounds filter-nodes-within-bounds filter-ways-within-bounds color-by-accessibility]]
    [app.model.osm :as osm :refer [OsmData]]
    [app.model.osm-dataset :as osm-dataset :refer [OsmDataset]]))

(defn osmNode->Point [proj d]
  (let [node (js->clj d :keywordize-keys true)]
       (lngLat->Point proj [(:lon node) (:lat node)])))

(defn d3DrawCallback-Nodes [upd proj data &[{:keys [svg]}]]
  (-> (.data upd (->> (filter #(= (:type %) "node") data)
                      (filter-nodes-within-bounds (proj->bounds proj))
                      clj->js))
      (.enter)
      (.append "a")
      (.append "circle")
      (.attr "cx" #(.-x (osmNode->Point proj %)))
      (.attr "cy" #(.-y (osmNode->Point proj %)))
      (.attr "r" (:r svg))
      (.attr "stroke" (:stroke svg))
      #_(.attr "fill" "red" #_#(color-by-accessibility (js->clj % :keywordize-keys true)))
      (.attr "fill-opacity" 0)
      (.on "click" (fn [d i ds] (js/console.log (js->clj d :keywordize-keys true))))))

(defn d3DrawCallback-Ways [upd proj data &[{:keys [svg]}]]
  (-> (.data upd (->> (filter #(= (:type %) "way") data)
                      (filter-ways-within-bounds (proj->bounds proj))
                      clj->js))
      (.enter)
      (.append "a")
      (.append "polyline")
      (.attr "points" (fn [d] (->> (:nodes (js->clj d :keywordize-keys true))
                                   (remove #(nil? (or (:lon %) (:lat %))))
                                   (map #(osmNode->Point proj %))
                                   (map #(str (.-x %) "," (.-y %)))
                                   (clojure.string/join " "))))
      (.attr "stroke" (:stroke svg))
      (.attr "stroke-width" (:stroke-width svg 1))
      (.attr "fill" "none")
      (.on "click" (fn [d i ds] (js/console.log (js->clj d))))))

(def style-topo {:relation-way {:svg {:stroke "blue" :stroke-width 3}}
                 :relation-node {:svg {:stroke "blue" :r 5}}
                 :way {:svg {:stroke "green" :stroke-width 1}}
                 :node {:svg {:stroke "red" :r 2}}
                 :way-node {:svg {:stroke "yellow" :r 2}}})

(def style-background {:way {:svg {:stroke "silver" :stroke-width 1}}})

(defn d3DrawCallback [sel proj data]
  (let [upd (.selectAll sel "a")
        {:keys [elements
                relation-way relation-node way node way-node node]}
          (js->clj data :keywordize-keys true)  ;; carefull, the keywords are not longer namespaced
;        _ (js/console.log elements)  
        ways (filter :nodes elements)
        relations (filter :members elements)]
       ;; By intention we have this drawing order:
       ;; * first relations (to be in background) -> they should have a bigger :r and :stroke-width
       ;; * next ways and nodes
       ;; * last way-nodes (when defined), so they can overwrite normal nodes
       (if relation-way (d3DrawCallback-Ways upd proj (map :ref (apply concat (map :members relations))) relation-way))
       (if relation-node (d3DrawCallback-Nodes upd proj (map :ref (apply concat (map :members relations))) relation-node))
       (if way (d3DrawCallback-Ways upd proj ways way))
       (if node (d3DrawCallback-Nodes upd proj elements node))
       (if way-node (d3DrawCallback-Nodes upd proj (apply concat (map :nodes ways)) way-node))))

(defsc D3SvgOSM [this {:keys [elements style]}]
  (js/console.log elements)  
  (d3SvgOverlay {:key key
                 :data (merge #_(select-keys style-topo [:way-attr :node-attr])
                              style
                              {:elements elements})
                 :drawCallback d3DrawCallback}))
