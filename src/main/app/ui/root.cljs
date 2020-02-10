(ns app.ui.root
  (:require
    [com.fulcrologic.fulcro.components :refer [defsc get-initial-state get-query transact!]]
    [com.fulcrologic.fulcro.dom :as dom]
    [app.model.geofeatures :as gf]
    [app.ui.leaflet.state :refer [State state mutate-layers]]
    [app.ui.leaflet :as leaflet :refer [Leaflet leaflet]]
    [app.ui.leaflet.layers :refer [example-layers]]))

(defsc Root [this props]
  {:initial-state (fn [_] (merge (get-initial-state State)
                                 {::leaflet/id {:main {::leaflet/layers (assoc-in example-layers [:aerial :base :checked] true)}}}))
   :query (fn [] (reduce into [(get-query State)
                               (get-query Leaflet)]))}

  (dom/div {:style {:width "100%" :height "100%"}}
   (state (merge props))
   (leaflet (merge (get-in props [::leaflet/id :main]) (select-keys props [::gf/id]) {:style {:height "80%" :width "100%"}}))))
