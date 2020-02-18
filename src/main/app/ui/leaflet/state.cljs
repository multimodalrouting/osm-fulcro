(ns app.ui.leaflet.state
  (:require
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [com.fulcrologic.fulcro.components :refer [defsc factory get-query transact!]]
    [app.model.geofeatures :as gf :refer [GeoFeature GeoFeaturesAll]]
    [app.ui.steps :as steps :refer [Steps update-state-of-step update-state-of-step-if-changed post-mutation]]
    [app.ui.steps-helper :refer [title->step title->step-index]]
    [app.ui.leaflet :as leaflet]
    [app.routing.graphs :refer [graphs calculate-graphs paths->geojson features->id2feature feature-ids->lngLatPaths]]
    [app.routing.route :refer [calculate-routes]]
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

(defsc State
  "State machine keeping track of loading all required data.
   If you want enforce reloading, just use `steps/update-state-of-step` to set the state back to nil"
  [this {:as props}]
  {:initial-state (fn [_] layers->dataset->graph->route)
   :query (fn [] (reduce into [[[::steps/id :layers->dataset->graph->route] (get-query Steps)
                                 ::leaflet/id ::gf/id]
                               (get-query XY2NodeId)]))}

  (let [state (get props [::steps/id :layers->dataset->graph->route])
        step-list (::steps/step-list state)
        leaflets (::leaflet/id props)
        geofeatures (::gf/id props)
        xy2nodeid (get-in props [::gf/xy2nodeid "singleton" ::gf/xy2nodeid])]

       (if (not (:state (title->step "Layers" step-list)))
           (let [list-of-layers-per-leaflet (map #(count (::leaflet/layers (val %))) leaflets)
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
                                                   :new-state :active
                                                   #_#_:info "get index"})  ;; TODO maybe we just want get the sources without geojson first?

                 (load! this ::gf/xy2nodeid XY2NodeId {})

                 (load! this ::gf/all GeoFeaturesAll {:target [:tmp ::gf/all]
                                                      :post-mutation `post-mutation
                                                      :post-mutation-params {:steps :layers->dataset->graph->route
                                                                             :step (title->step-index "Geofeatures" step-list)
                                                                             :ok-condition (fn [db] (::gf/id db))}}))
                 ;; TODO use app.ui.leaflet.state/mutate-datasets-load to load features from non-default remotes

       (if (and (= :done (:state (title->step "Geofeatures" step-list)))
                (not (:info (title->step "Geofeatures" step-list))))
           (update-state-of-step-if-changed this props
                                            {:steps :layers->dataset->graph->route
                                             :step (title->step-index "Geofeatures" step-list)
                                             :new-state :done
                                             :info (str (count geofeatures) " Sources; "
                                                        (reduce + (map #(count (get-in % [::gf/geojson :features])) (vals geofeatures))) " Features")}))

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
                                                     (vals @graphs)))
                        paths (map #(feature-ids->lngLatPaths % id2feature)
                                   edge-pairs)]
                       (transact! this [(mutate-datasets {:path [:routinggraph]
                                                          :data {::gf/geojson (paths->geojson paths
                                                                                              {:style {:stroke "darkgreen"
                                                                                                       :stroke-width 1}})}})]))
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
             (let [[path dist] (calculate-routes)]
                  (transact! this [(mutate-datasets {:path [:routes]
                                                     :data {::gf/geojson (paths->geojson [(let [id2feature (features->id2feature (::gf/id props) xy2nodeid)]
                                                                                               (feature-ids->lngLatPaths path id2feature))]
                                                                                         {:style {:stroke "blue"
                                                                                                  :stroke-width 4}})}})])
                  (update-state-of-step-if-changed this props
                                                  {:steps :layers->dataset->graph->route
                                                   :step (title->step-index "Route" step-list)
                                                   :new-state (if path :done :failed)
                                                   :info (str "ETA " dist "min")})))

      (steps/steps (merge state {:style {:width "100%"}}))))

(def state (factory State))
