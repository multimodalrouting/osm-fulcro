(ns app.routing.graphs
  (:require [app.model.geofeatures :as gf]
            [app.ui.leaflet :as leaflet :refer [overlay-filter-rule->filter]]
            [loom.graph :as graph]))


(defonce graphs (atom {}))


(defn edge-between-features [feature1id feature2id weight]
  (if-not (= feature1id feature2id)
          [feature1id feature2id weight]))


(defn connect-grouped-nodes [all-features {:keys [graph-id dataset id-fn filter-rule group-by-rule weight-fn]}]
  (let [features (get-in all-features [dataset ::gf/geojson :features])
        filtered-features (filter (overlay-filter-rule->filter filter-rule) features)
        grouped-features (vals (group-by #(get-in % group-by-rule) filtered-features))

        edges (->> (for [group-of-features grouped-features]
                        (->> (for [f1 group-of-features]
                                  (->> (for [f2 group-of-features]
                                            (edge-between-features (id-fn f1) (id-fn f2) (weight-fn f1 f2)))
                                       (remove nil?)))
                             (apply concat)))
                   (apply concat))]
       (swap! graphs assoc graph-id {:graph (apply graph/weighted-graph edges)
                                     :dataset dataset
                                     :id-fn id-fn})))

(defn highways [all-features xy2nodeid]
  (let [args {:graph-id :highways
              :dataset :trachenberger
              :filter-rule {[:geometry :type] #{"LineString"}
                            [:properties :highway] some?}}
        {:keys [graph-id dataset filter-rule]} args
        features (get-in all-features [dataset ::gf/geojson :features])
        filtered-features (filter (overlay-filter-rule->filter filter-rule) features)
        edges (->> (for [lineString filtered-features
                         :let [coordinates (get-in lineString [:geometry :coordinates])
                               nodeids (map #(get xy2nodeid %) coordinates)]]
                        (for [segment (partition 2 1 nodeids)
                              :let [[id1 id2] segment]]
                             [id1 id2 3])) ;; TODO weight
                   (apply concat))]
       (js/console.log edges)
       (swap! graphs assoc graph-id {:graph (apply graph/weighted-graph edges)
                                     :dataset dataset
                                     #_#_:id-fn id-fn})))


(defn connect-stop-positions [all-features]
  (connect-grouped-nodes all-features
                         {:graph-id :connected-stations
                          :dataset :vvo-small
                          :filter-rule {[:geometry :type] #{"Point"}
                                        [:properties :public_transport] #{"stop_position"}
                                        #_#_[:properties :name] #{"Trachenberger Platz"}}
                          :group-by-rule [:properties :name] ;; TODO use the relation stop_area
                          :weight-fn (constantly 10)
                          :id-fn :id}))


(defn calculate-graphs [all-features xy2nodeid]
  ;; TODO depending on fulcro-state
  (highways all-features xy2nodeid)
  (connect-stop-positions all-features))


;; helpers for creating a geojson 

(defn features->id2feature
  "Return a lookup table to calculate features from their ids. This can be canculated form geofeatures, as well as from a xy2nodeid-mapping."
  [geofeatures xy2nodeid]
  (merge
    (zipmap (vals xy2nodeid)
            (map (fn [xy] {:geometry {:coordinates xy}})
                 (keys xy2nodeid)))
    (let [id-fn :id  ;; TODO
          dataset :vvo-small
          features (get-in geofeatures [dataset ::gf/geojson :features])
          id2feature (zipmap (map id-fn features) features)]
         id2feature)))

(defn feature-ids->lngLatPaths [feature-ids id2feature]
  (->> feature-ids
       (map #(get id2feature %))
       (map #(get-in % [:geometry :coordinates]))))

(defn paths->geojson [lngLatPaths {:keys [style]}]
  {:type "FeatureCollection"
   :features (for [lngLatPath lngLatPaths]
                  {:type "Feature"
                   :geometry {:type "LineString"
                              :coordinates (into [] lngLatPath)}
                   :properties {:style style}})})
