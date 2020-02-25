(ns app.model.osm-helper
  (:require
    [app.model.osm :as osm]
    [clojure.string :as string]
    ["d3" :as d3]
            ))

(defn closest [{:keys [lat lon]} nodes]
  (->> (vec nodes)
       (map val)
       (filter
         (fn [node]
           (and (number? (::osm/lat node)) (number? (::osm/lon node)))))
       (map
         (fn [node]
           (assoc
             node
             ::distance (.geoDistance d3 (clj->js [(::osm/lon node) (::osm/lat node)]) (clj->js [lon lat])))
           ))
       (sort ::distance)
       (last)
       ))

(defn allTags [nodes]
   (->> (vec nodes)
        (map #(->> % val ::osm/tags keys))
        flatten
        set
        sort
          ))

(defn search [{:keys [tags searchText]} nodes]
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
                     (->> (vec tags)
                          (map
                            (fn [t]
                              (prn t)
                              (if-let [tagval (->> ts t)]
                                (do
                                  (prn tagval)
                                  (string/includes?
                                    (string/capitalize tagval)
                                    (string/capitalize searchText)))
                                false
                                )))
                          (contains? true)
                          )
                     false
                     )))
    ))

(defn nodes [_]
  (->> @(->> app.application/SPA :com.fulcrologic.fulcro.application/state-atom) :app.model.osm/id)
  )

(comment


    (closest {:lat 51.088 :lon 13.72033} (nodes))

    (allTags (nodes))

    (search {:tags [:addr:street :name] :searchText "Rehefelder"} (nodes))

  )

