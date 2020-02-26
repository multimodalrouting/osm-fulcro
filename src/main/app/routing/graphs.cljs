(ns app.routing.graphs
  (:require [app.model.geofeatures :as gf]
            [app.model.osm :as osm]
            [app.ui.leaflet :as leaflet :refer [overlay-filter-rule->filter]]
            [loom.graph :as graph]
            [loom.attr]))

(defonce graphs (atom {}))

(defn edges+attrs->loom-attrs [list-of-edges-with-attrs &[{:keys [directed] :or {directed false}}]]
  (let [list-of-edges-with-attrs+symmetric (concat list-of-edges-with-attrs
                                                   (if-not directed
                                                           (for [[from to weight attrs] list-of-edges-with-attrs]
                                                                [to from weight attrs])))
        by-from (group-by first list-of-edges-with-attrs+symmetric)]
       (zipmap (keys by-from)
               (for [by-from-val (vals by-from)
                     :let [by-to (group-by second by-from-val)]]
                    {:loom.attr/edge-attrs
                     (zipmap (keys by-to)
                             (for [by-to-val (vals by-to)]
                                  (apply merge (map last by-to-val))))}))))

#_(= (edges+attrs->loom-attrs [[:a :b 1 {:i :x}] [:a :c 2 {:i :y}]
                               [:b :c 3 {:j :k}] [:b :c 4 {:i :z}]]
                              {:directed true})
   {:a {:loom.attr/edge-attrs {:b {:i :x} :c {:i :y}}}
    :b {:loom.attr/edge-attrs {:c {:j :k :i :z}}}})

(defn edges+attrs->loom-graph [list-of-edges-with-attrs]
  (let [edges (for [[n1 n2 weigth attr] list-of-edges-with-attrs]
                   [n1 n2 weigth])]
       (assoc (apply graph/weighted-graph edges)
              :attrs (edges+attrs->loom-attrs list-of-edges-with-attrs))))


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

(defn highways [elements]
  (let [graph-id :highways
        edges (->> (for [way (filter ::osm/nodes elements)
                         :let [nodes (map #(apply hash-map %) (::osm/nodes way))]]
                        (for [segment (partition 2 1 (map ::osm/id nodes))
                              :let [[id1 id2] segment]]
                             [id1 id2 0.5 {:id (::osm/id way)}])) ;; TODO weight
                   (apply concat))]
       (swap! graphs assoc graph-id {:graph (edges+attrs->loom-graph edges)})))


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


(defn calculate-graphs [elements]
  ;; TODO depending on fulcro-state
  (highways elements)
  #_(connect-stop-positions all-features))


;; helpers for creating a geojson 

(defn features->id2feature
  "Return a lookup table to calculate features from their ids. This can be calculated form geofeatures, as well as from a xy2nodeid-mapping."
  [geofeatures xy2nodeid]
  (merge (zipmap (vals xy2nodeid)
                 (map (fn [xy] {:geometry {:coordinates xy}})
                      (keys xy2nodeid)))
         (->> (for [dataset [:bahnhof-neustadt :trachenberger :vvo-small]
                    :let [id-fn :id  ;; TODO
                          features (get-in geofeatures [dataset ::gf/geojson :features])
                          id2feature (zipmap (map id-fn features) features)]]
                   id2feature)
              (apply merge))))

(defn feature-ids->lngLatPaths [feature-ids id2feature]
  (->> feature-ids
       (map #(get id2feature %))
       (map #(get-in % [:geometry :coordinates]))))

(defn weight [wayFeature fromFeature toFeature] ;; deprecated
  {:cost (if-not wayFeature
                 (+ 5 (rand-int 5))
                 (+ 1 (rand-int 3)))
   :confidence (if wayFeature (rand))
   :wheelchair (case (get-in wayFeature [:properties :highway])
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
                     nil #_(rand-nth ["yes" "limited" "no"]))})

(defn paths->geojson [id2feature edge-pairs {:keys [style]}]  ;; deprecated
  (let [[fromId toId] (last edge-pairs)
        wayId (loom.attr/attr (get-in @graphs [:highways :graph])
                              [fromId toId] :id)
        [wayFeature fromFeature toFeature] (map #(get id2feature %) [wayId fromId toId])]
       (js/console.log (merge {:wayId wayId
                               :wayFeature wayFeature
                               :fromId fromId
                               :fromFeature fromFeature
                               :toId toId
                               :toFeature toFeature}
                              (weight wayId fromFeature toFeature))))
  {:type "FeatureCollection"
   :features (for [[fromId toId] edge-pairs
                   :let [wayId (loom.attr/attr (get-in @graphs [:highways :graph])
                                               [fromId toId] :id)
                         [wayFeature fromFeature toFeature] (map #(get id2feature %) [wayId fromId toId])
                         lngLatPath (map #(get-in % [:geometry :coordinates]) [fromFeature toFeature])]]
                  (if (and fromFeature toFeature)
                      {:type "Feature"
                       :geometry {:type "LineString"
                                  :coordinates (into [] lngLatPath)}
                       :properties (merge (weight wayFeature fromFeature toFeature)  ;; deprecated
                                          {:style style})}))})
