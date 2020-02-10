(ns app.ui.leaflet.state
  (:require
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :refer [transact!]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [com.fulcrologic.fulcro.components :refer [defsc factory get-query transact!]]
    [app.model.geofeatures :as gf :refer [GeoFeature GeoFeaturesAll]]
    [app.ui.steps :as steps :refer [Steps update-state-of-step update-state-of-step-if-changed post-mutation]]
    [app.ui.steps-helper :refer [title->step title->step-index]]
    [app.routing.graphs :refer [graphs calculate-graphs]]
    [app.routing.route :refer [calculate-routes]]
    [loom.graph :as graph]
    [app.model.geofeatures :as gf :refer [GeoFeature GeoFeaturesAll]]
    ))

(defmutation mutate-datasets-load
  [{:keys [updated-state]}]
  (action [{:keys [app]}]
    (doseq [[ds-name ds] (:leaflet/datasets updated-state)
            :let [source (:source ds)]]
           (load! app [::gf/id ds-name] GeoFeature {:remote (:remote source)
                                                    :params ;; :params must be a map, so we handover {:params {:args …}}
                                                            (select-keys source [:args])}))))

(defmutation mutate-datasets [{:keys [path data]}]
  (action [{:keys [app state]}]
    (swap! state update-in (concat [:leaflet/datasets] path)
                           (fn [d_orig d_new] (if (map? d_new) (merge d_orig d_new) d_new))
                           data)
    (transact! app [(mutate-datasets-load {:updated-state @state})])))

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

(defsc State
  "State machine keeping track of loading all required data.
   If you want enforce reloading, just use `steps/update-state-of-step` to set the state back to nil"
  [this {:as props}]
  {:initial-state (fn [_] layers->dataset->graph->route)
   :query [[::steps/id :layers->dataset->graph->route] (get-query Steps)
           :app.ui.leaflet/id ::gf/id]}

  (let [state (get props [::steps/id :layers->dataset->graph->route])
        step-list (::steps/step-list state)
        leaflets (:app.ui.leaflet/id props)
        geofeatures (::gf/id props)]

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
                                                   :new-state :active
                                                   #_#_:info "get index"})  ;; TODO maybe we just want get the sources without geojson first?
                 ;; load all ::gf/Geofeature (sources, but not geojson) normalized into ::gf/id
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
             (calculate-graphs (::gf/id props))
             (let [nodes (reduce + (map #(count (graph/nodes %)) (vals @graphs)))
                   edges (reduce + (map #(count (graph/edges %)) (vals @graphs)))]
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
             (let [result (calculate-routes)]
                  (update-state-of-step-if-changed this props
                                                  {:steps :layers->dataset->graph->route
                                                   :step (title->step-index "Route" step-list)
                                                   :new-state (if-not (empty? result) :done :failed)
                                                   :info (str "ETA " (second result) "min")})))

      (steps/steps (merge state {:style {:width "100%"}}))))

(def state (factory State))
