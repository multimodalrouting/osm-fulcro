(ns app.model.routing
  (:require [com.fulcrologic.fulcro.components :refer [defsc get-query]]
            [app.model.osm :as osm :refer [OsmData OsmDataNode]]))

(defsc Routing
  [this {::keys [id]}]
  {:ident (fn [] [::id id])
   :query (fn [] [::id ::from {::osm/id (get-query OsmDataNode)} ::to {::osm/id (get-query OsmDataNode)}
                  {::results (get-query OsmData)}])})
