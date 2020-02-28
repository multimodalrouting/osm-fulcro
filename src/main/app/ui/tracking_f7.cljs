(ns app.ui.tracking-f7
  (:require
    [app.background-geolocation :as bg-geo]
    [com.fulcrologic.fulcro.components :refer [defsc factory transact!]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [app.ui.framework7.components :refer [f7-fab]]
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp]))


(defsc ToggleTrackingFab [this {:keys [enabled triggerActivities position] :as props}]
  {:query [:enabled :triggerActivities :position]}
       (f7-fab {:onClick (fn []
                                 (if enabled
                                   (comp/transact! this [(bg-geo/stop-tracking nil)])
                                   (comp/transact! this [(bg-geo/start-tracking nil)])
                                   )
                                 )
                :style   {:height "26px" :width "26px"}
                :position position
                }
                   (dom/i {:classes ["fa" (if enabled "fa-square" "fa-circle")]})))

(def toggleTrackingFab (factory ToggleTrackingFab))

