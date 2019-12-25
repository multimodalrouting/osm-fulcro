(ns app.background-geolocation
  (:require [com.fulcrologic.fulcro.components :as comp]
            [com.fulcrologic.fulcro.mutations :refer [defmutation]]
            [com.fulcrologic.fulcro.data-fetch :refer [load!]]
            [app.application :refer [SPA]]
            [com.fulcrologic.fulcro.ui-state-machines :as uism]
            [app.ui.leaflet.state :refer [new-sensor-data new-location-data]]
            ))


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


(defmutation save-gpx-track [{:background-location/keys [track]}]
  (action [{:keys [ast]}]
          (do (prn ast)
              (prn "Will finally4 save gpx track (mutation)")
        (let [_track (seq track)]
                (update ast :params select-keys [:background-location/track])
                #_(load! SPA [:background-location/track _track] nil {:remote gpx})
                )))
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

(defmutation update-background-location-tracking-state [{:keys [bgstate]}]
  (action [{:keys [state]}]
          (do
            (swap! state into {:background-location/state bgstate})
            (bg.getLocations
              (fn [locations]
                (let [track (js->clj locations :keywordize-keys true)]
                  (prn "Will save gpx track (mutation)")
                  (comp/transact!
                    SPA
                    [ (save-gpx-track {:background-location/track  track})]
                    )))))))

(defn bg-state-update! []
  (.getState
    bg
    (fn [state]
      (comp/transact!
        SPA
        [(update-background-location-tracking-state {:bgstate (js->clj state :keywordize-keys true)})] {:refresh [:background-location/state]}))))

(defmutation start-tracking [_]
  (action [{:keys [state]}]
          (prn "Will enable")
          (bg.start
            bg-state-update!)))

(defmutation stop-tracking [_]
  (action [{:keys [state]}]
          (prn "Will stop")
          (bg.stop
            bg-state-update!)))

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

(defn listenForEvents!
  []
  (.onLocation
    bg
    (fn [location]
      (comp/transact! SPA [(new-location-data {:values (js->clj location :keywordize-keys true) :sensor_type "LOCATION"})]))
    )
  (.onEnabledChange
    bg
    (fn [isEnabled]
      bg-state-update!
      )
    )
  (.onActivityChange
    bg
    (fn [event]
      (comp/transact! SPA [(new-location-data {:value (js->clj event :keywordize-keys true) :sensor_type "ACTIVITY"})])
      )
    )
  (.onMotionChange
    bg
    (fn [motion] (comp/transact! SPA [(new-location-data {:values (js->clj motion :keywordize-keys true) :sensor_type "MOTION"})]))
    )
  (.onProviderChange
    bg
    (fn [provider] (comp/transact! SPA [(new-location-data {:values (js->clj provider :keywordize-keys true) :sensor_type "PROVIDER"})]))
    )

  )


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
