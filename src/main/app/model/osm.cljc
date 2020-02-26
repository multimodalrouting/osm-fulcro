(ns app.model.osm
  (:require [com.fulcrologic.fulcro.components :refer [defsc get-query]]))

 (defn merge-if-not-only-id [{:keys [current-normalized data-tree]}]
   #_(js/console.log "cur" current-normalized)
   #_(js/console.log "dt" data-tree)
   (merge current-normalized
          (if-not (empty? (dissoc data-tree ::id))
                  data-tree)))

(defsc OsmDataNode
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   ;:pre-merge merge-if-not-only-id
   :query [::id ::type ::tags
           ::lon ::lat]})

(defsc OsmDataWay
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   ;:pre-merge merge-if-not-only-id
   :query (fn [] [::id ::type ::tags
                  {::nodes (get-query OsmDataNode)}])})

(defsc OsmDataRelation
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   ;:pre-merge merge-if-not-only-id
   :query (fn [] [::id ::type ::tags
                  {::members [::type ::role {::ref (into (get-query OsmDataNode) (get-query OsmDataWay))}]}])})

(defsc OsmData
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   ;:pre-merge merge-if-not-only-id
   :query (fn [] (reduce into [(get-query OsmDataNode) (get-query OsmDataWay) (get-query OsmDataRelation)]))})
