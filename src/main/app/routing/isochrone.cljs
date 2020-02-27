(ns app.routing.isochrone
  (:require [app.routing.graphs :refer [graphs feature-ids->lngLatPaths]]
            [loom.graph]
            [loom.alg :refer [dijkstra-span]]))

#_(defn isochrone->geojson [id2feature &[{:keys [from max-costs] :or {max-costs 90}}]]
  (let [g (get-in @graphs [:highways :graph])
        span (dijkstra-span g (or from
                                  (first (loom.graph/nodes g))))]
    {:type "FeatureCollection"
     :features (->> (for [[fromId successors] span]
                         (for [[toId cost] successors
                               :let [lngLatPath (feature-ids->lngLatPaths [fromId toId] id2feature)]]
                              {:type "Feature"
                               :geometry {:type "LineString"
                                          :coordinates (into [] lngLatPath)}
                               :properties {:cost cost
                                            :style {:stroke (float->colortransition (/ cost max-costs))}
                                            :fromId fromId
                                            :toId toId}}))
                    (apply concat))}))
