(ns app.ui.leaflet.state
  (:require
    [app.application :refer [SPA]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :refer [transact!]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [com.fulcrologic.fulcro.application :refer [current-state]]))

(defmutation mutate-datasets-load
  "For now we don't require arguments, but always reload all datasets"
  [_]
  (action [{:keys [app]}]
    (doseq [[ds-name ds] (:leaflet/datasets (current-state SPA))]
           (let [source (:source ds)
                 [ident params] (if (keyword? (:query source))
                                    [(:query source) nil]
                                    [:_ {:query (:query source)}])]
                (load! app ident nil {:remote (:remote source)
                                      :params params
                                      :target [:leaflet/datasets ds-name :data :geojson]})))))

(defmutation mutate-datasets [{:keys [path data]}]
  (action [{:keys [app state]}]
    (swap! state update-in (concat [:leaflet/datasets] path)
                           (fn [d_orig d_new] (if (map? d_new) (merge d_orig d_new) d_new))
                           data)
    (transact! app [(mutate-datasets-load)])))

(defmutation mutate-layers [{:keys [path data]}]
  (action [{:keys [state]}]
    (swap! state update-in (concat [:leaflet/layers] path)
                           (fn [d_orig d_new] (if (map? d_new) (merge d_orig d_new) d_new))
                           data)))

