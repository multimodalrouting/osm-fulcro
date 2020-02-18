(ns app.routing.route
  (:require [app.model.geofeatures :as gf]
            [app.routing.graphs :refer [graphs]]
            [loom.graph :as graph]
            [loom.alg :as alg]))


(defn calculate-routes
  "returns the path+dist of the route selected or expected to be best"
  []
  ;; TODO depending on fulcro-state
  (let [g+meta (last (vals @graphs))
        g (:graph g+meta)
        from (first (graph/nodes g))
        to (last (graph/nodes g))
        result (alg/dijkstra-path-dist g from to)]
       (prn "route:" result)
       result))
