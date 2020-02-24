(ns app.model.osm-dataset
  (:require [com.fulcrologic.fulcro.components :refer [defsc get-query]]
            [app.model.osm :refer [OsmData]]))

(defsc OsmDataset
  [this {::keys [id] :as props}]
  {:ident (fn [] [::id id])
   :query [::id ::source {'(::elements {:remove {:members-when-incomplete true}}) (get-query OsmData)}]})
