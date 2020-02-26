(ns app.routing.trachenberger-ws
  (:require [nubank.workspaces.core :refer [defcard]]
            [nubank.workspaces.card-types.fulcro3 :as ct.fulcro]
            [nubank.workspaces.model :as wsm]
            [com.fulcrologic.fulcro.components :refer [defsc get-query get-initial-state transact!]]
            [com.fulcrologic.fulcro.dom :as dom]
            [app.model.geofeatures :as gf]
            [app.model.osm-dataset :as osm-dataset]
            [app.model.osm :as osm]
            [app.model.routing :as routing]
            [app.application :refer [SPA_conf conf-with-default-remote]]
            [app.ui.leaflet :as leaflet :refer [leaflet Leaflet]]
            [app.ui.leaflet.layers :refer [example-layers]]
            [app.ui.leaflet.state :refer [State state mutate-layers]]
            [app.ui.leaflet.layers.d3svg-osm :refer [style-background style-streets style-public-transport style-route]]))

(defsc Root [this props]
  {:initial-state (fn [_] (merge (get-initial-state State)
                                 {::leaflet/id {:main {::leaflet/center [51.0845 13.728]
                                                       ::leaflet/zoom 17
                                                       ::leaflet/layers {:osm {:base {:name "OSM Tiles"
                                                                                      :checked true
                                                                                      :tile {:url "https://{s}.tile.osm.org/{z}/{x}/{y}.png"}}}
                                                                         #_#_:background {:osm {:styles style-background}}  ;; only without the loading-filter
                                                                         :streets {:osm {:styles style-streets}}
                                                                         :public-transport {:osm {:styles style-public-transport}}
                                                                         :route:main {:osm {:styles style-route}}}}}
                                  ::osm-dataset/id {:linie3 {:required true}
                                                    :trachenberger {:required true}}
                                  ::routing/id {:main {::routing/from {::osm/id 1010804810}
                                                       ::routing/to {::osm/id 3331425510}}}}))
   :query (fn [] (reduce into [(get-query State)
                               (get-query Leaflet)]))}

  (dom/div {:style {:width "100%" :height "100%"}}
   (state props)
   (leaflet (merge (get-in props [::leaflet/id :main])
                   (select-keys props (concat (filter keyword? (get-query Leaflet))
                                              (map #(first (keys %))
                                                   (filter map? (get-query Leaflet)))))
                   {:style {:height "80%" :width "100%"}}))))

(defcard trachenberger
  {::wsm/align {:flex 1}}  ;; fullscreen
  (ct.fulcro/fulcro-card
    {::ct.fulcro/root Root
     ::ct.fulcro/wrap-root? false
     ::ct.fulcro/app (conf-with-default-remote SPA_conf :pathom)}))
