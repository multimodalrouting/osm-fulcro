(ns app.ui.root
  (:require
    [app.ui.leaflet :refer [LeafletWithSidebar leafletWithSidebar]]
    [com.fulcrologic.fulcro.components :refer [defsc get-query]]))

(defsc Root [this props]
  {:query (fn [] (get-query LeafletWithSidebar))}
  (leafletWithSidebar props))
