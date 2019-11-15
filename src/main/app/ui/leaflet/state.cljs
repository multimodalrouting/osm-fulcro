(ns app.ui.leaflet.state
  (:require
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :refer [transact!]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [app.model.geofeatures :as gf :refer [GeoFeatures]]))

(defmutation mutate-datasets-load
  [{:keys [updated-state]}]
  (action [{:keys [app]}]
    (doseq [[ds-name ds] (:leaflet/datasets updated-state)
            :let [source (:source ds)]]
           (load! app [::gf/id ds-name] GeoFeatures {:remote (:remote source)
                                                     :params ;; :params must be a map, so we handover {:params {:args …}}
                                                             (select-keys source [:args])}))))

(defmutation mutate-datasets [{:keys [path data]}]
  (action [{:keys [app state]}]
    (swap! state update-in (concat [:leaflet/datasets] path)
                           (fn [d_orig d_new] (if (map? d_new) (merge d_orig d_new) d_new))
                           data)
    (transact! app [(mutate-datasets-load {:updated-state @state})])))

(defmutation mutate-layers [{:keys [path data]}]
  (action [{:keys [state]}]
    (swap! state update-in (concat [:leaflet/layers] path)
                           (fn [d_orig d_new] (if (map? d_new) (merge d_orig d_new) d_new))
                           data)))

