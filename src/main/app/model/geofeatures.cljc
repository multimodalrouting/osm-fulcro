(ns app.model.geofeatures
  (:require [com.fulcrologic.fulcro.components :refer [defsc get-query]]))

(defsc GeoFeature
  [this {::keys [id] :as props}]
  {:ident         (fn [] [::id id])
   :query         [::id ::source ::geojson]})

(defsc GeoFeaturesAll
  [this props]
  {:ident         ::all
   :query         [{::all (get-query GeoFeature)}]})
