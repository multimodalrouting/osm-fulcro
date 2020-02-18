(ns app.client
  (:require
    [app.application :refer [SPA]]
    [app.ui.root :as root]
    [app.ui.leaflet.state :refer [mutate-datasets mutate-layers]]
    [com.fulcrologic.fulcro.components :refer [transact!]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro-css.css-injection :as cssi]
    [app.background-geolocation :refer [bg-prepare!] ]
    [taoensso.timbre :as log]))

(defn load-map! []
  (if (nil? js/navigator.graphhopper)
    nil
    (.loadMap js/navigator.graphhopper "sachsen-latest" (fn [success] (prn success))))
  )

(defn load-overpass! []
  (transact! SPA [(mutate-datasets {:data {
                                           :overpass-example {:source {:remote :overpass :type :geojson
                                                                       :query ["area[name=\"Dresden\"]->.city;"
                                                                               "nwr(area.city)[operator=\"DVB\"]->.connections;"
                                                                               "node.connections[public_transport=stop_position];"]}}}})]))


#_(defn initSensors
  [sensorTypes]
  (do
    (prn "init Sensors")
    (prn (str (type js/sensors.addSensorListener)))
    (if (some? js/sensors)
      (do
        (prn "yes")
        #_(map js/sensors.enableSensor sensors)
        (doseq [sensorT (seq sensorTypes)]
          (do
            (prn (str "Will enable sensor: " sensorT))
            (js/sensors.addSensorListener
              sensorT
              "NORMAL"
              (fn [event]
                #_(prn values)
                (transact! SPA [(new-sensor-data {:values (js->clj event.values) :sensor_type sensorT})])
                )
              (fn [error]
                (prn "sensor error")
                (prn error)
                )))))
      (prn "no"))))


(defn ^:export refresh []
  (js/console.clear)
  (log/info "Hot code Remount")
  (cssi/upsert-css "componentcss" {:component root/Root})
  (app/mount! SPA root/Root "app")
  )

(defn ^:export init []
  (log/info "Application starting.")
  (cssi/upsert-css "componentcss" {:component root/Root})
  ;(inspect/app-started! SPA)
  (app/set-root! SPA root/Root {:initialize-state? true})
  ;(dr/initialize! SPA)
  (app/mount! SPA root/Root "app" {:initialize-state? false}))
