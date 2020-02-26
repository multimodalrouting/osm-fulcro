(ns app.routing.route
  (:require [app.model.geofeatures :as gf]
            [app.routing.graphs :refer [graphs]]
            [loom.graph :as graph]
            [loom.alg :as alg]))


(defn calculate-routes
  "returns the path+dist of the route selected or expected to be best"
  [g from to]
  ;; TODO depending on fulcro-state
  (prn "start routing" (type g) from to)
  (let [result (if g (alg/dijkstra-path-dist g from to))]
       (prn "route:" result)
       result))
