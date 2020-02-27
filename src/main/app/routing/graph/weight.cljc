(ns app.routing.graph.weight
  (:require [app.model.osm :as osm]))

(defn weight
  "The taken argument should have the structure of an osm-way and represents an edge in the routing graph"
  [osmEdge]
  (let [tags (::osm/tags osmEdge)]
       {:cost (* 0.1 (+ 1 (rand-int 10)))
        :confidence (rand)
        :wheelchair (case (:highway tags)
                          "footway"
                            "yes"
                          "cyleway"
                            "yes"
                          "residential"
                            "limited"
                          "primary"
                            "no"
                          "secondary"
                            "no"
                          "tertiary"
                            "no"
                          nil)}))
