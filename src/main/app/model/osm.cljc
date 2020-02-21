(ns app.model.osm
  (:require [com.fulcrologic.fulcro.components :refer [defsc get-query]]))

(defsc OsmData
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   :query [::id ::lon ::lat
           ::type ::nodes ::members
           ::tags]})
