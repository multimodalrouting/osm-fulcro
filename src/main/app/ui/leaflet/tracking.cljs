(ns app.ui.leaflet.tracking
  (:require
    [app.background-geolocation :as bg-geo]
    [com.fulcrologic.fulcro.components :refer [defsc factory transact!]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    ["react-leaflet-control" :default Control]
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp]))

(def control (react-factory Control))

(defsc ControlToggleTracking [this {:keys [enabled triggerActivities] :as props}]
  {:query [:enabled :triggerActivities]}
       (control {:position "topleft"}
                (dom/button {:onClick (fn []
                                        (comp/transact! this [(bg-geo/send-message {:message "ping from frontend"})]))
                             :style   {:height "26px" :width "26px"}}
                            (dom/i {:classes ["fa" "fa-wheelchair"]}))
                (dom/button {:onClick (fn []
                                        (prn (if enabled "true" "not enabled!"))
                                        (prn props)
                                        (if enabled
                                          (comp/transact! this [(bg-geo/stop-tracking nil)])
                                          (comp/transact! this [(bg-geo/start-tracking nil)])
                                          )
                                        )
                             :style   {:height "26px" :width "26px"}}
                            (dom/i {:classes ["fa" (if enabled "fa-square" "fa-circle")]}))))

(def controlToggleTracking (factory ControlToggleTracking))

