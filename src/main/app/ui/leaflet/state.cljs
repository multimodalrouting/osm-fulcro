(ns app.ui.leaflet.state
  (:require
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [com.fulcrologic.fulcro.components :refer [defsc factory get-query transact!]]
    [app.model.geofeatures :as gf :refer [GeoFeature GeoFeaturesAll]]
    [app.ui.steps :as steps :refer [Steps update-state-of-step update-state-of-step-if-changed post-mutation]]
    [app.ui.steps-helper :refer [title->step title->step-index]]
    [app.ui.leaflet :as leaflet]))


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
  [this {:as props :keys [force-recalc]}]
  {:initial-state (fn [_] layers->dataset->graph->route)
   :query [:force-recalc
           [::steps/id :layers->dataset->graph->route] (get-query Steps)
           ::leaflet/id ::gf/id]}

  (let [state (get props [::steps/id :layers->dataset->graph->route])
        step-list (::steps/step-list state)
        leaflets (::leaflet/id props)
        geofeatures (::gf/id props)]

       (if (or force-recalc (not (:state (title->step "Layers" step-list))))
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
                 (transact! this [(update-state-of-step {:steps :layers->dataset->graph->route
                                                         :step (title->step-index "Geofeatures" step-list)
                                                         :new-state :active
                                                         #_#_:info "get index"})])  ;; TODO maybe we just want get the sources without geojson first?
                 ;; load all ::gf/Geofeature (sources, but not geojson) normalized into ::gf/id
                 (load! this ::gf/all GeoFeaturesAll {:target [:tmp ::gf/all]
                                                      :post-mutation `post-mutation
                                                      :post-mutation-params {:steps :layers->dataset->graph->route
                                                                             :step (title->step-index "Geofeatures" step-list)
                                                                             :ok-condition (fn [db] (::gf/id db))}}))
                 ;; TODO use app.ui.leaflet.state/mutate-datasets-load to load features from non-default remotes

       (if (and (= :done (:state (title->step "Geofeatures" step-list)))
                (or force-recalc (not (:info (title->step "Geofeatures" step-list)))))
           (update-state-of-step-if-changed this props
                                            {:steps :layers->dataset->graph->route
                                             :step (title->step-index "Geofeatures" step-list)
                                             :new-state :done
                                             :info (str (count geofeatures) " Sources; "
                                                        (reduce + (map #(count (get-in % [::gf/geojson :features])) (vals geofeatures))) " Features")}))

      (steps/steps (merge state {:style {:width "100%"}}))))

(def state (factory State))
