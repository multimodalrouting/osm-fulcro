(ns app.ui.leaflet.state
  (:require
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :refer [transact!]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [com.fulcrologic.fulcro.components :refer [defsc factory get-query transact!]]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.dom :as dom]
    [app.model.geofeatures :as gf :refer [GeoFeature GeoFeaturesAll]]
    [app.model.osm :as osm :refer [OsmData]]
    [app.model.osm-dataset :as osm-dataset :refer [OsmDataset OsmDatasetMeta]]
    [app.ui.steps :as steps :refer [Steps update-state-of-step update-state-of-step-if-changed post-mutation]]
    [app.ui.steps-helper :refer [title->step title->step-index]]
    [app.ui.leaflet :as leaflet]
    [app.routing.graphs :refer [graphs calculate-graphs paths->geojson features->id2feature]]
    [app.routing.route :refer [calculate-routes]]
    [app.routing.isochrone :refer [isochrone->geojson]]
    [loom.graph :as graph]))

(defmutation mutate-datasets-load
  [{:keys [updated-state]}]
  (action [{:keys [app]}]
    (doseq [[ds-name ds] (::gf/id updated-state)
            :let [source (:source ds)]]
           (load! app [::gf/id ds-name] GeoFeature {:remote (:remote source)
                                                    :params ;; :params must be a map, so we handover {:params {:args …}}
                                                            (select-keys source [:args])}))))

(defmutation mutate-datasets [{:keys [path data]}]
  (action [{:keys [app state]}]
    (swap! state update-in (concat [::gf/id] path)
                           (fn [d_orig d_new] (if (map? d_new) (merge d_orig d_new) d_new))
                           data)
    ;; not longer needed
    #_(transact! app [(mutate-datasets-load {:updated-state @state})])))

(defmutation mutate-layers [{:app.ui.leaflet/keys [id] :keys [path data]}]
  (action [{:keys [state]}]
    (swap! state update-in (concat [:app.ui.leaflet/id id :app.ui.leaflet/layers] path)
                           (fn [d_orig d_new] (if (map? d_new) (merge d_orig d_new) d_new))
                           data)))



(def graphhopper-remote #_:graphhopper :graphhopper-web)

(defn load-route
  [fromPoint toPoint app]
  (let [request {:remote graphhopper-remote
                 :params {
                          :start fromPoint
                          :end   toPoint
                          }
                 :target [:leaflet/datasets :routing :data]
                 }]
    (prn "Will request")
    (prn request)
    (load! app :graphhopper/route nil request))

  )

(defmutation current-point-select [props]
  (action [{:keys [app state]}]
          (let [points (:selected/points @state)]
            (if (> 1 (count points))
              nil
              (load-route (:selected/latlng (last points)) props app)
              )
            (swap! state into {:selected/points (vec (conj (:selected/points @state) {:selected/latlng props}))})

            )))


(def layers->dataset->graph->route
  {::steps/id {:layers->dataset->graph->route
    {::steps/id :layers->dataset->graph->route
     ::steps/step-list [{:title "Layers" :contents {:done "defined" :failed "missing"}}
                        {:title "Geofeatures" :contents {:done "loaded" :failed "failed to load" :active "loading" :queued "to be loaded"}}
                        {:title "Graph" :contents {:done "calculated" :failed "failed to calculate" :active "calculating" :queued "to be calculated"}}
                        {:title "Route" :contents {:done "calculated" :failed "failed to calculate" :active "calculating" :queued "to be calculated"}}]}}})

(defsc XY2NodeId [this props]
  {:ident (fn [_] [::gf/xy2nodeid "singleton"])
   :query [::gf/xy2nodeid]})

(defsc Comparison [this props]
  {:ident (fn [_] [::gf/comparison "singleton"])
   :query [::gf/comparison]})

(defn component+query->tree [component query]
  (let [state (-> component (aget "props") (aget "fulcro$app")
                  :com.fulcrologic.fulcro.application/state-atom deref)]
       (fdn/db->tree query state state)))

(defmutation load-required-datasets [_]
  (action [{:keys [app state]}]
    (let [osm-dataset (::osm-dataset/id @state)
          step-list (get-in @state [::steps/id :layers->dataset->graph->route ::steps/step-list])]
         (doseq [required-dataset (->> (filter :required (vals osm-dataset))
                                       (map ::osm-dataset/id)
                                       (remove nil?))]
                (load! app [::osm-dataset/id required-dataset] OsmDataset {:post-mutation `post-mutation
                                                                           :post-mutation-params {:steps :layers->dataset->graph->route
                                                                                                  :step (title->step-index "Geofeatures" step-list)
                                                                                                  :ok-condition (fn [db] (::osm/id db))}})))))  ;; TODO

(defsc State
  "State machine keeping track of loading all required data.
   If you want enforce reloading, just use `steps/update-state-of-step` to set the state back to nil"
  [this {:as props}]
  {:initial-state (fn [_] layers->dataset->graph->route)
   :query (fn [] (reduce into [[[::steps/id :layers->dataset->graph->route]
                                 ::leaflet/id]
                                (get-query Steps)
                               [{::osm-dataset/root (get-query OsmDataset)} ::osm-dataset/id ::osm/id]
                               (get-query XY2NodeId)
                               (get-query Comparison)]))}

  (let [state (get props [::steps/id :layers->dataset->graph->route])
        step-list (::steps/step-list state)
        leaflets (::leaflet/id props)
        geofeatures (::gf/id props)
        osm-dataset (::osm-dataset/id props)
        osm (::osm/id props)
        xy2nodeid (get-in props [::gf/xy2nodeid "singleton" ::gf/xy2nodeid])
        comparison (apply merge (get-in props [::gf/comparison "singleton" ::gf/comparison]))]
    

#_       (js/console.log "Bytes"
                       (->> (component+query->tree this [{::osm-dataset/root (get-query OsmDataset)}])
                            #_#_str count time))
#_       (js/console.log (->> (component+query->tree this [{::osm-dataset/root (get-query OsmDataset)}])
                            ::osm-dataset/root first
                            ::osm-dataset/elements (filter #(= "relation" (::osm/type %)))
                                                   (filter #(= (get-in % [::osm/tags :type]) "route"))
                                                   (filter #(= (get-in % [::osm/tags :ref]) "3"))))
#_       (js/console.log (->> (component+query->tree this [{::osm-dataset/root (get-query OsmDataset)}])
                            ::osm-dataset/root first
                            ::osm-dataset/elements (filter #(= "relation" (::osm/type %)))
                                                   (filter #(= (get-in % [::osm/tags :name]) "Zeithainer Straße"))))

#_       (js/console.log (->> (component+query->tree this [{::osm-dataset/root (get-query OsmDataset)}])
                            ::osm-dataset/root first
                            ::osm-dataset/elements
                            (group-by ::osm/type)
                            (#(zipmap (keys %) (map count (vals %))))))
    

       (def comparison comparison)  ;; TODO cleanup

       (if (not (:state (title->step "Layers" step-list)))
           (let [list-of-layers-per-leaflet (map #(count (:app.ui.leaflet/layers (val %))) leaflets)
                 leaflet-count (count list-of-layers-per-leaflet)
                 layers-sum (reduce + list-of-layers-per-leaflet)
                 errors (remove nil? [(if-not (pos? leaflet-count) "No Leaflets found")
                                      (if (some #{0} list-of-layers-per-leaflet) "Every Leaflet should have at least one Layer")])]
                (if-not (empty? errors)
                    (update-state-of-step-if-changed this props
                                                     {:steps :layers->dataset->graph->route
                                                      :step (title->step-index "Layers" step-list)
                                                      :new-state :failed
                                                      :info (clojure.string/join "; " errors)})
                    (update-state-of-step-if-changed this props
                                                     {:steps :layers->dataset->graph->route
                                                      :step (title->step-index "Layers" step-list)
                                                      :new-state :done
                                                      :info (str (if (> leaflet-count 1)
                                                                     (str leaflet-count " Leaflets; "))
                                                                     layers-sum " Layers")}))))

       (when-not (:state (title->step "Geofeatures" step-list))
                 (update-state-of-step-if-changed this props
                                                  {:steps :layers->dataset->graph->route
                                                   :step (title->step-index "Geofeatures" step-list)
                                                   :new-state :active})

                 (load! this ::osm-dataset/root OsmDatasetMeta {:post-mutation `load-required-datasets}))

       (if (and (= :done (:state (title->step "Geofeatures" step-list)))
                (not (:info (title->step "Geofeatures" step-list))))
           (update-state-of-step-if-changed this props
                                            {:steps :layers->dataset->graph->route
                                             :step (title->step-index "Geofeatures" step-list)
                                             :new-state :done
                                             :info (str ;(count osm-dataset) " Sources; "
                                                        (count (filter :required (vals osm-dataset))) " Required; "
                                                        (count (vals osm)) " Features")
                                             :info-popup (str (->> (vals osm)
                                                                   (group-by ::osm/type)
                                                                   (#(zipmap (keys %) (map count (vals %))))))}))

       (when (and (#{:done} (:state (title->step "Geofeatures" step-list)))
                  (not (:state (title->step "Graph" step-list))))
             (update-state-of-step-if-changed this props
                                              {:steps :layers->dataset->graph->route
                                               :step (title->step-index "Graph" step-list)
                                               :new-state :active})
             (calculate-graphs (::gf/id props) xy2nodeid)
             (let [nodes (reduce + (map #(count (graph/nodes (:graph %))) (vals @graphs)))
                   edges (reduce + (map #(count (graph/edges (:graph %))) (vals @graphs)))]
                  (let [id2feature (features->id2feature (::gf/id props) xy2nodeid)
                        edge-pairs (reduce into (map #(graph/edges (:graph %))
                                                     (vals @graphs)))]
                       (transact! this [(mutate-datasets {:path [:routinggraph]
                                                          :data {::gf/geojson (paths->geojson id2feature
                                                                                              edge-pairs
                                                                                              {:style {:stroke-width 2}})}})]))
                  (update-state-of-step-if-changed this props
                                                   {:steps :layers->dataset->graph->route
                                                    :step (title->step-index "Graph" step-list)
                                                    :new-state (if (pos? edges) :done :failed)
                                                    :info (str nodes " Nodes; " edges " Edges")})))

       (when (and (#{:done} (:state (title->step "Graph" step-list)))
                  (not (:state (title->step "Route" step-list))))
             (update-state-of-step-if-changed this props
                                              {:steps :layers->dataset->graph->route
                                               :step (title->step-index "Route" step-list)
                                               :new-state :active})

             (doseq [gf (filter #(= (get-in (val %) [::gf/calculate :type]) :isochrones)
                                geofeatures)
                     :let [id (key gf)
                           args (get-in (val gf) [::gf/calculate :args])]]
                    (transact! this [(mutate-datasets {:path [id]
                                                       :data {::gf/geojson (isochrone->geojson (features->id2feature (::gf/id props) xy2nodeid)
                                                                                               args)}})]))

             (let [[path dist] (calculate-routes)]
                  (transact! this [(mutate-datasets {:path [:routes]
                                                     :data {::gf/geojson (paths->geojson (features->id2feature (::gf/id props) xy2nodeid)
                                                                                         (partition 2 1 path)
                                                                                         {:style {:stroke-width 6}})}})])
                  (update-state-of-step-if-changed this props
                                                  {:steps :layers->dataset->graph->route
                                                   :step (title->step-index "Route" step-list)
                                                   :new-state (if path :done :failed)
                                                   :info (str "ETA " dist "min")})))

      (steps/steps (merge state {:style {:width "100%"}}))))

(def state (factory State))
