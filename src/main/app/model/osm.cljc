(ns app.model.osm
  (:require [com.fulcrologic.fulcro.components :refer [defsc get-query]]))

 (defn merge-if-not-only-id [{:keys [current-normalized data-tree]}]
   (if-not (empty? (dissoc data-tree ::id))
           (merge current-normalized
                  data-tree)))

(defsc OsmDataNode
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   :query [::id ::type ::tags
           ::lon ::lat]
   #_#_:pre-merge merge-if-not-only-id})

(defsc OsmDataWay
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   :query (fn [] [::id ::type ::tags
                  {::nodes (get-query OsmDataNode)}])
   #_#_:pre-merge merge-if-not-only-id})

(defsc OsmDataRelation
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   :query (fn [] [::id ::type ::tags
                  {::members [::type ::role {::ref (into (get-query OsmDataNode) (get-query OsmDataWay))}]}])})

(defsc OsmData
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   :query (fn [] (reduce into [(get-query OsmDataNode) (get-query OsmDataWay) (get-query OsmDataRelation)]))})
