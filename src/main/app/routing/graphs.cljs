(ns app.routing.graphs
  (:require [app.model.geofeatures :as gf]
            [app.ui.leaflet :as leaflet :refer [overlay-filter-rule->filter]]
            [loom.graph :as graph]))


(defonce graphs (atom {}))


(defn edge-between-features [feature1 feature2 weight]
  (if-not (= (:id feature1) (:id feature2))
          [(:id feature1) (:id feature2) weight]))


(defn example-trachenberger [all-features]
  (let [features (get-in all-features [:vvo ::gf/geojson :features])
        filter-rule {[:geometry :type] #{"Point"}
                     [:properties :public_transport] #{"stop_position"}}
        filtered-features (filter (overlay-filter-rule->filter filter-rule) features)

        trachenberger-features (-> (group-by #(get-in % [:properties :name]) filtered-features)
                                   (get "Trachenberger Platz"))
        edges (->> (for [f1 trachenberger-features]
                        (->> (for [f2 trachenberger-features]
                                  (edge-between-features f1 f2 1))
                             (remove nil?)))
                   (apply concat))]
       (swap! graphs assoc :main (apply graph/weighted-graph edges))))


(defn calculate-graphs [all-features]
  ;; TODO depending on fulcro-state
  (example-trachenberger all-features))
