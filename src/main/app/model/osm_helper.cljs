(ns app.model.osm-helper
  (:require
    [app.model.osm :as osm]
    [clojure.string :as string]
    [clojure.core.reducers :as r]
    ["d3" :as d3]
    ))

(defn closest [{:keys [lat lon]} nodes]
  "find the closest node from the given latitude and longitude that has a tag"
  (->> (vec nodes)
       (map val)
       (filter
         (fn [node]
           (and
             (exists? (::osm/tags node))
             (= (::osm/type node) "node")
             (number? (::osm/lat node))
             (number? (::osm/lon node)))))
       (map
         (fn [node]
           (assoc
             node
             ::distance (.geoDistance d3 (clj->js [(::osm/lon node) (::osm/lat node)]) (clj->js [lon lat])))
           ))
       (r/reduce (fn [n1 n2]
                   (if (nil? n1)
                     n2
                     (if (< (::distance n1) (::distance n2))
                       n1
                       n2
                       ))))))

(defn allTags [nodes]
  (->> (vec nodes)
       (map #(->> % val ::osm/tags keys))
       flatten
       set
       sort
       ))

#_(defn search [{:keys [tags searchText]} nodes]
  (->> (vec nodes)
       (map val)
       (filter (fn [node]
                 (if-let [ts (::osm/tags node)]
                   #_(if-let [tagval (->> ts :addr:street)]
                       (do
                         (prn tagval)
                         (string/includes?
                           (string/capitalize tagval)
                           (string/capitalize searchText)))
                       false
                       )
                   (do
                     (prn tags)
                     (contains?
                       (for [t tags]
                         (do
                           (prn t)
                           (if-let [tagval (->> ts t)]
                             (do
                               (prn tagval)
                               (string/includes?
                                 (string/capitalize tagval)
                                 (string/capitalize searchText)))
                             false
                             )))
                       true)
                     )
                   false)))
       ))

(defn nodes [_]
  (->> @(->> app.application/SPA :com.fulcrologic.fulcro.application/state-atom) :app.model.osm/id)
  )

(comment


  (closest {:lat 51.06539547578647 :lon 13.713907808452442} (nodes))

  (allTags (nodes))

  #_(search {:tags [:addr:street :name] :searchText "Rehefelder"} (nodes))

  )

