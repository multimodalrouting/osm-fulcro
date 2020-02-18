(ns app.background-geolocation
  (:require [com.fulcrologic.fulcro.components :as comp]
            [com.fulcrologic.fulcro.mutations :refer [defmutation]]
            [com.fulcrologic.fulcro.data-fetch :refer [load!]]
            [app.application :refer [SPA]]
            [com.fulcrologic.fulcro.ui-state-machines :as uism]
            [app.utils.gpx :refer [geo->gpx]]
            [app.utils.ring-buffer :as rb]))



#_(def bg-properties [
                      "CY_MEDIUM"
                      "DESIRED_ACCURACY_LOW"
                      "DESIRED_ACCURACY_VERY_LOW"
                      "DESIRED_ACCURACY_THREE_KILOMETER"
                      "AUTHORIZATION_STATUS_NOT_DETERMINED"
                      "AUTHORIZATION_STATUS_RESTRICTED"
                      "AUTHORIZATION_STATUS_DENIED"
                      "AUTHORIZATION_STATUS_ALWAYS"
                      "AUTHORIZATION_STATUS_WHEN_IN_USE"
                      "NOTIFICATION_PRIORITY_DEFAULT"
                      "NOTIFICATION_PRIORITY_HIGH"
                      "NOTIFICATION_PRIORITY_LOW"
                      "NOTIFICATION_PRIORITY_MAX"
                      "NOTIFICATION_PRIORITY_MIN"
                      "ACTIVITY_TYPE_OTHER"
                      "ACTIVITY_TYPE_AUTOMOTIVE_NAVIGATION"
                      "ACTIVITY_TYPE_FITNESS"
                      "ACTIVITY_TYPE_OTHER_NAVIGATION"
                      "PERSIST_MODE_ALL"
                      "PERSIST_MODE_LOCATION"
                      "PERSIST_MODE_GEOFENCE"
                      "PERSIST_MODE_NONE"
                      "deviceSettings"
                      "logger"
                      "ready"
                      "configure"
                      "reset"
                      "requestPermission"
                      "getProviderState"
                      "onLocation"
                      "onMotionChange"
                      "onHttp"
                      "onHeartbeat"
                      "onProviderChange"
                      "onActivityChange"
                      "onGeofence"
                      "onGeofencesChange"
                      "onSchedule"
                      "onEnabledChange"
                      "onConnectivityChange"
                      "onPowerSaveChange"
                      "onNotificationAction"
                      "on"
                      "un"
                      "removeListener"
                      "removeListeners"
                      "getState"
                      "start"
                      "stop"
                      "startSchedule"
                      "stopSchedule"
                      "startGeofences"
                      "startBackgroundTask"
                      "stopBackgroundTask"
                      "finish"
                      "changePace"
                      "setConfig"
                      "getLocations"
                      "getCount"
                      "destroyLocations"
                      "clearDatabase"
                      "insertLocation"
                      "sync"
                      "getOdometer"
                      "resetOdometer"
                      "setOdometer"
                      "addGeofence"
                      "removeGeofence"
                      "addGeofences"
                      "removeGeofences"
                      "getGeofences"
                      "getGeofence"
                      "geofenceExists"
                      "getCurrentPosition"
                      "watchPosition"
                      "stopWatchPosition"
                      "registerHeadlessTask"
                      "setLogLevel"
                      "getLog"
                      "destroyLog"
                      "emailLog"
                      "isPowerSaveMode"
                      "getSensors"
                      "playSound"
                      "transistorTrackerParams"
                      "test"])

(def bg js/window.BackgroundGeolocation)

(defmutation add-local-gpx-track [{:background-location/keys [track]}]
  (action [{:keys [ast state]}]
          (let [_track (seq track)]
            (prn "Will add gpx track (mutation)")
            (swap! state into {:background-location/tracks _track}))))

(defmutation clear-local-gpx-tracks [_]
  (action [{:keys [state]}]
          (swap! state into {:background-location/tracks []})))

(defmutation save-gpx-track [{:background-location/keys [track]}]
  (action [{:keys [ast]}]
          (let [_track (seq track)]
            (prn "Will finally4 save gpx track (mutation)")
            (update ast :params select-keys [:background-location/track])
            #_(load! SPA [:background-location/track _track] nil {:remote gpx})
            ))
  (gpx [env] true)
  )

(defmutation send-message [env]
  (action [props]
          (do
            (prn "send-message to backend" env)
            )
          )
  (gpx [env] true)
  )

(defmutation clear-locations [_]
  (action [_]
          (bg.destroyLocations #(prn "successfully cleared internal SQLite DB")
                               (fn [error] (prn "an error occured while cleaning SQLite DB" error)))))

(defmutation update-background-location-tracking-state [{:keys [bgstate isPaused]}]
  (action [{:keys [state]}]
          (swap! state into {:background-location/state (assoc bgstate :isPaused isPaused)})))

(defmutation store-bg-location [_]
  (action [{:keys [state]}]
          (bg.getLocations
            (fn [locations]
              (let [track (js->clj locations :keywordize-keys true)]
                (prn "Will save gpx track (mutation)")
                (comp/transact!
                  SPA
                  [
                   (save-gpx-track {:background-location/track track})
                   (add-local-gpx-track {:background-location/track track})
                   (clear-locations nil)
                   ]))))))

(defmutation pause-tracking [_]
  (action [{:keys [state]}]
          (prn "Will pause")
          (bg.stop
            #(bg.getState
               (fn [state]
                 (comp/transact!
                   SPA
                   [(update-background-location-tracking-state {:bgstate (js->clj state :keywordize-keys true) :isPaused true})] {:refresh [:background-location/state]}))))))

(defmutation start-tracking [_]
  (action [{:keys [state]}]
          (prn "Will enable")
          (bg.start
            #(bg.getState
               (fn [state]
                 (comp/transact!
                   SPA
                   [(update-background-location-tracking-state {:bgstate (js->clj state :keywordize-keys true) :isPaused false})] {:refresh [:background-location/state]}))))))

(defmutation stop-tracking [_]
  (action [{:keys [state]}]
          (prn "Will stop")
          (bg.stop
            #(bg.getState
              (fn [state]
                (comp/transact!
                  SPA
                  [
                   (update-background-location-tracking-state {:bgstate (js->clj state :keywordize-keys true) :isPaused false})
                   (store-bg-location nil )
                   ] {:refresh [:background-location/state]}))))))



(defn bg-ready!
  []
  (.ready
    bg
    (clj->js {
              :debug             true
              :logLevel          bg.LOG_LEVEL_VERBOSE
              :stopTimeout       1
              :distanceFilter    10
              :desiredAccuracy   bg.DESIRED_ACCURACY_HIGH
              :url               "http://10.0.2.2:9001/locations"
              :autoSync          true
              ;:params (.transistorTrackerParams bg js/window.device)
              :stopOnTerminate   false
              :startOnBoot       true
              :foregroundService true}
             ),
    (fn [state]
      (comp/transact! SPA [(update-background-location-tracking-state {:bgstate (js->clj state :keywordize-keys true)})])
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



(defn listenForEvents!
  []
  (.onLocation
    bg
    (fn [location]
      (comp/transact! SPA [(new-location-data {:values (js->clj location :keywordize-keys true) :sensor_type "LOCATION"})])))
  (.onEnabledChange
    bg
    (fn [isEnabled]
      #(bg.getState
            (fn [state]
              (comp/transact!
                SPA
                [(update-background-location-tracking-state {:bgstate (js->clj state :keywordize-keys true)})] {:refresh [:background-location/state]})))))
  (.onActivityChange
    bg
    (fn [event]
      (comp/transact! SPA [(new-location-data {:value (js->clj event :keywordize-keys true) :sensor_type "ACTIVITY"})])))
  (.onMotionChange
    bg
    (fn [motion] (comp/transact! SPA [(new-location-data {:values (js->clj motion :keywordize-keys true) :sensor_type "MOTION"})])))
  (.onProviderChange
    bg
    (fn [provider] (comp/transact! SPA [(new-location-data {:values (js->clj provider :keywordize-keys true) :sensor_type "PROVIDER"})]))))


(defn intervalLocation [interval]
  (.setTimeout js/window
               #(.then (.getCurrentPosition bg)
                       (fn [position]
                         (intervalLocation interval)
                         ))
               interval
               ))


(defn bg-prepare!
  "initializes the background geo location plugin, if the plugin has been loaded in a cordova environment
  and js/window.BackgroundLocation is set globally"
  []
  (when (some? bg)
    (listenForEvents!)
    (bg-ready!)
    #_(intervalLocation 8000)
    ))
