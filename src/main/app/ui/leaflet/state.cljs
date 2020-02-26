(ns app.ui.leaflet.state
  (:require
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :refer [transact!]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [com.fulcrologic.fulcro.components :refer [defsc factory get-query transact! ]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.dom :as dom]
    [app.model.geofeatures :as gf :refer [GeoFeature GeoFeaturesAll]]
    [app.model.osm :as osm :refer [OsmData]]
    [app.model.osm-dataset :as osm-dataset :refer [OsmDataset OsmDatasetMeta]]
    [app.model.routing :as routing :refer [Routing]]
    [app.ui.steps :as steps :refer [Steps update-state-of-step update-state-of-step-if-changed post-mutation]]
    [app.ui.steps-helper :refer [title->step title->step-index]]
    [app.ui.leaflet :as leaflet]
    [app.routing.graphs :refer [graphs calculate-graphs]]
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

#_(defsc XY2NodeId [this props]
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
                               [::routing/id]
                               #_(get-query XY2NodeId)
                               (get-query Comparison)
                               [:tweak]]))}

  (let [state (get props [::steps/id :layers->dataset->graph->route])
        step-list (::steps/step-list state)
        leaflets (::leaflet/id props)
        geofeatures (::gf/id props)
        osm-dataset (::osm-dataset/id props)
        osm (::osm/id props)
        routings (::routing/id props)
        ;xy2nodeid (get-in props [::gf/xy2nodeid "singleton" ::gf/xy2nodeid])
        comparison (apply merge (get-in props [::gf/comparison "singleton" ::gf/comparison]))
        tweak (get-in leaflets [:main :tweak])]

       ;;TODO this way the to and from is set
       #_(merge/merge-component! this Routing {::routing/id :main
                                             ::routing/to {::osm/id 4532859077}})

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
                           
             (calculate-graphs (vals osm))

             (let [nodes (reduce + (map #(count (graph/nodes (:graph %))) (vals @graphs)))
                   edges (reduce + (map #(count (graph/edges (:graph %))) (vals @graphs)))]
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

             #_(doseq [gf (filter #(= (get-in (val %) [::gf/calculate :type]) :isochrones)
                                geofeatures)
                     :let [id (key gf)
                           args (get-in (val gf) [::gf/calculate :args])]]
                    (transact! this [(mutate-datasets {:path [id]
                                                       :data {::gf/geojson (isochrone->geojson (features->id2feature (::gf/id props) xy2nodeid)
                                                                                               args)}})]))

             (let [[id routing_] (first routings)
                   routing (-> (component+query->tree this [{[::routing/id id] (get-query Routing)}])
                               (get [::routing/id id]))
                   g (:graph (:highways @graphs))
                   from (get-in routing [::routing/from ::osm/id])
                   to (get-in routing [::routing/to ::osm/id])
                   [path dist] (calculate-routes g from to)]

                  (when (and tweak
                             path (> dist 0))
                        (update-state-of-step-if-changed this props
                                                         {:steps :layers->dataset->graph->route
                                                          :step (title->step-index "Geofeatures" step-list)
                                                          :new-state :done
                                                          :info "Pruned"})
                      (let [state (-> this (aget "props") (aget "fulcro$app")
                                     :com.fulcrologic.fulcro.application/state-atom)]
                           (swap! state assoc ::osm/id {})
                           (swap! state assoc ::osm-dataset/id {}))
                      (reset! graphs {}))

                  (merge/merge-component! this OsmDataset {::osm-dataset/id (keyword (str "route" id))
                                                           ::osm-dataset/elements [(assoc (get osm from)
                                                                                          ::osm/id (keyword (str "route" id :from))
                                                                                          ::osm/tags {:routing {::routing/id id}})
                                                                                   (assoc (get osm to)
                                                                                          ::osm/id (keyword (str "route" id :to))
                                                                                          ::osm/tags {:routing {::routing/id id}})
                                                                                   {::osm/id (keyword (str "route" id))
                                                                                    ::osm/type "way"
                                                                                    ::osm/tags {:routing {::routing/id id}}
                                                                                    ::osm/nodes (->> path
                                                                                                     (map (fn [i] (get osm i) #_{::osm/id i}))
                                                                                                     (into []))
                                                                                                #_[{::osm/id (keyword (str "route" id :from))}
                                                                                                 {::osm/id (keyword (str "route" id :to))}]}]}
                                          :append [::osm-dataset/root])

                  (update-state-of-step-if-changed this props
                                                  {:steps :layers->dataset->graph->route
                                                   :step (title->step-index "Route" step-list)
                                                   :new-state (if path :done :failed)
                                                   :info (str "ETA " dist "min")}))

             (doseq [[id routing_] routings]
                    (let [routing (-> (component+query->tree this [{[::routing/id id] (get-query Routing)}])
                                      (get [::routing/id id]))
                          from (get osm (get-in routing [::routing/from ::osm/id]))
                          to (get osm (get-in routing [::routing/to ::osm/id]))]
                                                 :append [::osm-dataset/root])))

      (steps/steps (merge state {:style {:width "100%"}}))))

(def state (factory State))
