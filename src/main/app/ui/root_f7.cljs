(ns app.ui.root-f7
  (:require
    [app.ui.leaflet :refer [Leaflet leaflet]]
    [app.ui.leaflet.state :refer [State state mutate-layers]]
    [com.fulcrologic.fulcro.components :refer [defsc get-query]]
    [app.ui.framework7.components :refer
     [f7-app f7-panel f7-view f7-views f7-page f7-page-content f7-navbar f7-nav-left f7-nav-right f7-link f7-toolbar f7-tabs f7-tab f7-block f7-block-title f7-list f7-list-item f7-list-button f7-fab f7-fab-button f7-icon]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li p h3 button]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [app.utils.file-save :refer [store-file open-with]]
    [app.utils.gpx :refer [geo->gpx-xml]]
    [app.background-geolocation :refer [clear-locations clear-local-gpx-tracks]]
    [app.ui.leaflet.layers :refer [example-layers]]
    ))

(defsc MapView [this props]
  {
   :initial-state (fn [_] (merge (comp/get-initial-state State)
                                 {:app.ui.leaflet/id {:main {:app.ui.leaflet/layers (assoc-in example-layers [:aerial :base :checked] true)}}}))
   :query (fn [] (reduce into [(comp/get-query State)
                               (comp/get-query Leaflet)]))

   :ident         (fn [] [:component/id :map-view])
   :route-segment ["main"]
   }
  (f7-view
    nil
    (f7-page
      nil
      (f7-fab
        {:position  "left-top"
         :slot      "fixed"
         :className "panel-open"
         :panel "left"
         }
        (f7-icon
          {
           :icon    "bars"
           }
          )
        )
      (f7-toolbar {:position "bottom"}
                  (state props))
      (leaflet (merge (get-in props [:app.ui.leaflet/id :main]) (select-keys props [:app.model.geofeatures/id]) {:style {:height "100%" :width "100%"}}))
      )))

(def mapView (comp/factory MapView))

(defsc Settings [this {:keys [:account/time-zone :account/real-name] :as props}]
  {:query         [:account/time-zone :account/real-name]
   :ident         (fn [] [:component/id :settings])
   :route-segment ["settings"]
   :initial-state {}}
  (f7-view
    nil
    (f7-page
      nil
      (f7-navbar
        nil
        (f7-nav-left
          nil
          (f7-link {:icon "bars" :panelOpen "left"})
          ))
      (h3 "Settings")
      )))

(defsc MyTracks [this {:as props}]
  {:query         [:background-location/tracks]
   :ident         (fn [] [:component/id :tracks])
   :route-segment ["tracks"]
   :initial-state {:background-location/tracks []}}
  (f7-view
    nil
    (let [tracks (:background-location/tracks props)]
      (f7-page
        nil
        (f7-navbar
          nil
          (f7-nav-left
            nil
            (f7-link {:icon "bars" :panelOpen "left"})))
        (f7-list
          {:inset true}
          (f7-list-button {:onClick (fn [] (comp/transact! this [(clear-locations nil) (clear-local-gpx-tracks nil)]))
                           :title   "clear DB"})
          (f7-list-button {:onClick (fn []
                                      (let [mimeType "application/gpx+xml"]
                                        (store-file (str "test-" (.getTime (js/Date.)) ".gpx")
                                                    (geo->gpx-xml {:tracks tracks})
                                                    mimeType
                                                    (fn [file] (open-with file.nativeURL mimeType))
                                                    (fn [err] (prn "error!!! " err))))
                                      )
                           :title   "save and open"}))
        (f7-block
          nil
          (f7-block-title nil "current tracks")
          (f7-list
            nil
            (for [track tracks
                  :let [uuid (:uuid track)]]
              (f7-list-item {:key uuid :title (str (:uuid track)) :text (str (:timestamp track))})))
          )))))

(dr/defrouter TopRouter [this props]
              {:router-targets [MapView MyTracks Settings]})

(def ui-top-router (comp/factory TopRouter))

(defsc AppMain [this {:root/keys [router main] :as props}]
  {:query         (fn []
                    (let [query (reduce into [(comp/get-query MapView)
                                              [{:root/router (comp/get-query TopRouter)}
                                               [::uism/asm-id ::TopRouter]]])]
                      query
                      ))
   :initial-state (fn [_] (merge
                            (comp/get-initial-state MapView)
                            {:root/router {}}))}
  (let [current-tab (some-> (dr/current-route this this) first keyword)]
    (f7-app
      (clj->js {:params {
                         :id         "io.github.multimodalrouting.osm-fulcro"
                         :name       "multimodalrouting"
                         :theme      "auto"
                         :router     false
                         :pushState  true
                         :ajaxLinks  "a.ajax"
                         :fastClicks false
                         }})
      (f7-panel
        {:side "left"}
        (f7-view
          nil
          (f7-page
            nil
            (f7-navbar {:title "Routing"})
            (f7-block
              nil
              (f7-list
                nil
                (f7-list-button
                  {
                   :title         "Map"
                   :tabLinkActive (= :main current-tab)
                   :onClick       (fn [] (dr/change-route this ["main"]))}
                  )
                (f7-list-button
                  {
                   :title         "My Tracks"
                   :tabLinkActive (= :tracks current-tab)
                   :onClick       (fn [] (dr/change-route this ["tracks"]))}
                  )
                (f7-list-button
                  {
                   :title         "Settings"
                   :tabLinkActive (= :settings current-tab)
                   :onClick       (fn [] (dr/change-route this ["settings"]))}
                  )
                )))))
      (f7-views {:tabs true}
                (mapView props)
                #_(ui-top-router router)))))


(def ui-app-main (comp/factory AppMain))

#_(defsc Root [this {:root/keys [main-app]}]
  {:query         [{:root/main-app (comp/get-query AppMain)}]
   :ident         (fn [] [:component/id :ROOT])
   :initial-state (fn [_] {:root/main-app (comp/get-initial-state AppMain)})
   }
  (ui-app-main main-app))
