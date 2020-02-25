(ns app.routing.isochrone
  (:require [app.routing.graphs :refer [graphs feature-ids->lngLatPaths]]
            [loom.graph]
            [loom.alg :refer [dijkstra-span]]))

(defn hex
  "return a 2 digit hex"
  [i]
  (str (if (< i 16) "0")
       (.toString i 16)))

(defn float->colortransition
  "0 ~ green; 0.5 ~ yellow; 1 ~ red"
  [value]
  (let [value-bound (max 0 (min 1 value))
        r (int (* 2 255 (max 0 (min 0.5 value))))
        g (int (* 2 255 (- 1 (max 0.5 (min 1 value)))))]
       (str "#" (hex r) (hex g) "00")))

(defn isochrone->geojson [id2feature &[{:keys [from max-costs] :or {max-costs 90}}]]
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
