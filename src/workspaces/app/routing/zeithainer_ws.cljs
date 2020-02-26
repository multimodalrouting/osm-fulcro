(ns app.routing.zeithainer-ws
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
                                 {::leaflet/id {:main {::leaflet/center [51.0824 13.7300]
                                                       ::leaflet/zoom 19
                                                       ::leaflet/layers {:background {:osm {:styles style-background}}
                                                                         :streets {:osm {:styles style-streets}}
                                                                         :public-transport {:osm {:styles style-public-transport}}
                                                                         :route:main {:osm {:styles style-route}}}}}
                                  ::osm-dataset/id {:trachenberger {:required true}
                                                    :linie3 {:required true}}
                                  ::routing/id {:main {::routing/from {::osm/id 2120500033}
                                                       ::routing/to {::osm/id 2120500077}}}}))
   :query (fn [] (reduce into [(get-query State)
                               (get-query Leaflet)]))}

  (dom/div {:style {:width "100%" :height "100%"}}
   (state props)
   (leaflet (merge (get-in props [::leaflet/id :main])
                   (select-keys props (concat (filter keyword? (get-query Leaflet))
                                              (map #(first (keys %))
                                                   (filter map? (get-query Leaflet)))))
                   {:style {:height "80%" :width "100%"}}))))

(defcard zeithainer
  {::wsm/align {:flex 1}}  ;; fullscreen
  (ct.fulcro/fulcro-card
    {::ct.fulcro/root Root
     ::ct.fulcro/wrap-root? false
     ::ct.fulcro/app (conf-with-default-remote SPA_conf :pathom)}))
