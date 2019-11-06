(ns app.ui.leaflet.state
  (:require
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :refer [transact!]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [app.model.geofeatures :as gf :refer [GeoFeatures]]))
    [app.utils.ring-buffer :as rb]
    ))

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


(defmutation new-sensor-data [{:keys [values sensor_type]}]
  (action [{:keys [state]}]
          (let [values (vec values)
                keywd (keyword "sensors" sensor_type)
                ]
            (do
              (let [rbuf
                    (if (nil? (keywd @state))
                      (rb/ring-buffer 10)
                      (keywd @state)
                      )]
                (swap! state into {keywd (conj rbuf values)}))
                ))))

(defmutation new-location-data [{:keys [values sensor_type]}]
  (action [{:keys [state]}]
          (let [values (vec values)
                keywd (keyword "sensors" sensor_type)
                ]
            (do
              (let [rbuf
                    (if (nil? (keywd @state))
                      (vec [])
                      (keywd @state)
                      )]
                (swap! state into {keywd (conj rbuf values)}))
              ))))
