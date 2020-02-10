(ns app.routing.graphs
  (:require [app.model.geofeatures :as gf]
            [app.ui.leaflet :as leaflet :refer [overlay-filter-rule->filter]]
            [loom.graph :as graph]))


(defonce graphs (atom {}))


(defn edge-between-features [feature1 feature2 weight]
  (if-not (= (:id feature1) (:id feature2))
          [(:id feature1) (:id feature2) weight]))


(defn connect-grouped-nodes [all-features {:keys [filter-rule group-by-rule weight-fn]}]
  (let [features (get-in all-features [:vvo ::gf/geojson :features])
        filtered-features (filter (overlay-filter-rule->filter filter-rule) features)
        grouped-features (vals (group-by #(get-in % group-by-rule) filtered-features))

        edges (->> (for [group-of-features grouped-features]
                        (->> (for [f1 group-of-features]
                                  (->> (for [f2 group-of-features]
                                            (edge-between-features f1 f2 (weight-fn f1 f2)))
                                       (remove nil?)))
                             (apply concat)))
                  (apply concat))]
       (swap! graphs assoc :main (apply graph/weighted-graph edges))))


(defn connect-stop-positions [all-features]
  (connect-grouped-nodes all-features
                         {:filter-rule {[:geometry :type] #{"Point"}
                                        [:properties :public_transport] #{"stop_position"}
                                        #_#_[:properties :name] #{"Trachenberger Platz"}}
                          :group-by-rule [:properties :name]
                          :weight-fn (constantly 10)}))


(defn calculate-graphs [all-features]
  ;; TODO depending on fulcro-state
  (connect-stop-positions all-features))
