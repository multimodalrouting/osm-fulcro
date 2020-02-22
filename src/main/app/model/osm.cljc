(ns app.model.osm
  (:require [com.fulcrologic.fulcro.components :refer [defsc get-query]]))

(defsc OsmDataNode
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   :query [::id ::type ::tags
           ::lon ::lat]})

(defsc OsmDataWay
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   :query (fn [] [::id ::type ::tags
                  {::nodes (get-query OsmDataNode)}])})

(defsc OsmDataRelation
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   :query (fn [] [::id ::type ::tags
                  {::members [::type ::role {::ref (into (get-query OsmDataNode) (get-query OsmDataWay))}]}])})
                                                   ;; TODO don't normalize nodes for which only the id is known

(defsc OsmData
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   :query (fn [] (reduce into [(get-query OsmDataNode) (get-query OsmDataWay) (get-query OsmDataRelation)]))})
