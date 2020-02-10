(ns app.ui.leaflet.example-ws
  (:require [nubank.workspaces.core :refer [defcard]]
            [nubank.workspaces.card-types.fulcro3 :as ct.fulcro]
            [nubank.workspaces.model :as wsm]
            [com.fulcrologic.fulcro.components :refer [defsc get-query get-initial-state transact!]]
            [com.fulcrologic.fulcro.dom :as dom]
            [app.model.geofeatures :as gf]
            [app.application :refer [SPA_conf conf-with-default-remote]]
            [app.ui.leaflet :as leaflet :refer [leaflet Leaflet]]
            [app.ui.leaflet.layers :refer [example-layers]]
            [app.ui.leaflet.state :refer [State state mutate-layers]]))

(defsc Root [this props]
  {:initial-state (fn [_] (merge (get-initial-state State)
                                 {::leaflet/id {:top {::leaflet/id :top
                                                      ::leaflet/center [51.055 13.74]}
                                                :bottom {::leaflet/id :bottom
                                                         ::leaflet/zoom 2
                                                         ::leaflet/layers (assoc-in example-layers [:aerial :base :checked] true)}}}))
   :query (fn [] (reduce into [(get-query State)
                               (get-query Leaflet)]))}

  (dom/div {:style {:width "100%" :height "100%"}}
   (state (merge props {:force-recalc true}))
   (leaflet (merge (get-in props [::leaflet/id :top]) (select-keys props [::gf/id]) {:style {:height "40%" :width "100%"}}))
   (dom/button {:onClick (fn [_] (let [new-layer (rand-nth [:aerial :osm :memo :openpt])]
                                      (transact! this [(mutate-layers {::leaflet/id :top
                                                                       :data (assoc-in example-layers [new-layer :base :checked] true)})])))}
               "Set new BaseLayer")  ;; TODO till now fulcro-db is not aware of state changes within the original layer selection
   (leaflet (merge (get-in props [::leaflet/id :bottom]) (select-keys props [::gf/id]) {:style {:height "40%" :width "100%"}}))))

(defcard example
  {::wsm/align {:flex 1}}  ;; fullscreen
  (ct.fulcro/fulcro-card
    {::ct.fulcro/root Root
     ::ct.fulcro/wrap-root? false
     ::ct.fulcro/app (conf-with-default-remote SPA_conf :pathom)}))
