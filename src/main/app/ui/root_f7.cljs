(ns app.ui.root-f7
  (:require
    [clojure.string :as s]
    [app.ui.leaflet :as leaflet :refer [leaflet Leaflet]]
    [app.ui.leaflet.state :refer [State state mutate-layers]]
    [com.fulcrologic.fulcro.components :refer [defsc get-query]]
    [app.model.osm-dataset :as osm-dataset]
    [app.model.osm :as osm]
    [app.model.osm-helper :refer [closest]]
    [app.model.routing :as routing :refer [Routing]]
    [app.ui.steps-helper :refer [title->step-index]]
    [app.ui.steps :as steps :refer [post-mutation]]
    [app.ui.framework7.components :refer
     [f7-app f7-panel f7-view f7-views f7-page f7-row f7-col f7-page-content f7-navbar f7-nav-left f7-nav-right f7-link f7-toolbar f7-tabs f7-tab f7-block f7-block-title f7-list f7-list-item f7-list-button f7-fab f7-fab-button f7-icon f7-input]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li p h3 button]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [app.utils.file-save :refer [store-file open-with]]
    [app.utils.gpx :refer [geo->gpx-xml]]
    [app.background-geolocation :refer [clear-locations clear-local-gpx-tracks]]
    [app.ui.leaflet.layers.d3svg-osm :refer [style-background style-streets style-public-transport style-route]]))

(defmutation map-clicked [{:keys [id latlng]}]
  (action [{:keys [app state]}]
          (swap! state assoc-in [::id id ::latlng] latlng)
          (if-let [geofeature (closest latlng (:app.model.osm/id @state ))]
            (let [osmid (::osm/id geofeature)]
              (swap! state assoc-in [::id id ::osm-id] osmid)
              (swap! state assoc-in [(::current-edit @state)] osmid))

            )))

(defmutation edit-start-point [_]
  (action [{:keys [app state]}]
          (swap! state assoc-in [::current-edit] ::start)
          ))
(defmutation edit-destination [_]
  (action [{:keys [app state]}]
          (swap! state assoc-in [::current-edit] ::destination)
          ))


(defsc StartDestinationInput [this props]
  {:initial-state {
                   ::start       {}
                   ::destination {}
                   ::current-edit ::destination
                   }
   :query         [::start ::destination ::current-edit]
   }
  (prn props)
  (f7-block
    {:style {:width "100%" }}
    (f7-row
      {}
      (f7-col
        {}
        (f7-link
          {:style {:backgroundColor
                   (if (= (::current-edit props) ::start)
                     "#eedfd9")}
           :onClick #(comp/transact! this [(edit-start-point nil)])
           }
          (::startLabel props)
          ))
      (f7-col
        {}
        (f7-icon
          {:icon "arrow-right"}
          )
        "nach"
        )
      (f7-col
        {}
        (f7-link
          {:style {:backgroundColor
                      (if (= (::current-edit props) ::destination)
                        "#eedfd9")}
           :onClick #(comp/transact! this [(edit-destination nil)])
           }
          (::destinationLabel props)
          )))
    ))

(def startDestinationInput (comp/factory StartDestinationInput))


(defn geofeature->label [feature]
  (if-not feature
    "not set"
    (if-let [tags (::osm/tags feature)]
      (->> [:name
            :amenity
            :addr:street
            :addr:housenumber
            :addr:city
            ]
           (map #(% tags))
           (remove nil?)
           (s/join ", ")
           )
      (str (::osm/lat feature) "," (::osm/lon feature))
      )))

(defsc MapView [this props]
  {
   :initial-state (fn [_] (merge (comp/get-initial-state State)
                                 (comp/get-initial-state StartDestinationInput)
                                 {::leaflet/id     {:main {::leaflet/center [51.0824 13.7300]
                                                           ::leaflet/zoom   19
                                                           ::leaflet/layers {
                                                                             :tiles            {:base {:name "OSM Tiles"
                                                                                                       :checked true
                                                                                                       :tile {:url         "https://{s}.tile.osm.org/{z}/{x}/{y}.png"
                                                                                                              :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}}}
                                                                             ;:background       {:osm {:styles style-background}}
                                                                             ;:streets          {:osm {:styles style-streets}}
                                                                             ;:public-transport {:osm {:styles style-public-transport}}
                                                                             :route:main       {:osm {:styles style-route}}
                                                                             }}}
                                  ::osm-dataset/id {        ;:trachenberger {:required true}
                                                    :linie3 {:required true}
                                                    }}
                                 #_{:app.ui.leaflet/id {:main {:app.ui.leaflet/layers (assoc-in example-layers [:aerial :base :checked] true)}}}
                                 ))
   :query         (fn [] (reduce into [
                                       [::steps/id :layers->dataset->graph->route ::steps/step-list]
                                       (comp/get-query State)
                                       (comp/get-query Leaflet)
                                       (comp/get-query StartDestinationInput)]))

   :ident         (fn [] [:component/id :map-view])
   :route-segment ["main"]
   }

  (merge/merge-component! this Routing {::routing/id :main
                                        ::routing/from {::osm/id (::start props)}
                                        ::routing/to {::osm/id (::destination props)}})

  (prn (get-in props [::steps/id :layers->dataset->graph->route ::steps/step-list]))

  (f7-view
    nil
    (f7-page
      nil
      #_(f7-fab
        {:position  "left-top"
         :slot      "fixed"
         :className "panel-open"
         :panel     "left"
         }
        (f7-icon
          {
           :icon "bars"
           }
          ))
      (f7-toolbar
        {:position "top" :inner true}
        (f7-link
          {:icon "bars"
           :panelOpen "left"
           }
          )
        (startDestinationInput
          (merge
            {
             ::startLabel (geofeature->label (get-in props [::osm/id (::start props)]))
             ::destinationLabel (geofeature->label (get-in props [::osm/id (::destination props)]))
             }
            (select-keys props (concat (filter keyword? (get-query StartDestinationInput))
                                       (map #(first (keys %))
                                            (filter map? (get-query StartDestinationInput)))))))
        (f7-link nil "")
        )
      (f7-toolbar {:position "bottom"}
                  (state props))
      (leaflet (merge (get-in props [::leaflet/id :main])
                      (select-keys props (concat (filter keyword? (get-query Leaflet))
                                                 (map #(first (keys %))
                                                      (filter map? (get-query Leaflet)))))
                      {
                       :style               {:height "100%" :width "100%"}
                       ::leaflet/onMapClick (fn [evt]
                                              (comp/transact! this [(map-clicked evt)] {:post-mutation        `post-mutation
                                                                                        :post-mutation-params {:steps :layers->dataset->graph->route
                                                                                                               :step  2#_(title->step-index "Graph" (get-in props [::steps/id :layers->dataset->graph->route ::steps/step-list]))}}))
                       }))
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
