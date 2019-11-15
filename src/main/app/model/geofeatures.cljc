(ns app.model.geofeatures
  (:require [com.fulcrologic.fulcro.components :refer [defsc]]))

(defsc GeoFeatures
  [this {::keys [id] :as props}]
  {:ident         (fn [] [::id id])
   :query         [::id ::source ::geojson]})
