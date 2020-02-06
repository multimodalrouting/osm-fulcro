(ns app.client-f7
  (:require
    [app.application :refer [SPA]]
    [app.ui.root-f7 :as root]
    [app.ui.leaflet.state :refer [mutate-datasets mutate-layers new-sensor-data]]
    [com.fulcrologic.fulcro.components :refer [transact!]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro-css.css-injection :as cssi]
    [com.fulcrologic.fulcro.networking.http-remote :as net]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.fulcro.components :as comp]
    ["framework7/framework7-lite.esm.bundle.js" :default Framework7]
    ["framework7-react/framework7-react.esm.js" :default Framework7React]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.inspect.inspect-client :as inspect]
    [app.background-geolocation :refer [bg-prepare!] ]
    [app.ui.leaflet :refer [LeafletWithSidebar leafletWithSidebar]]
    ))

(defn init-f7 []
  (.use Framework7 Framework7React))


(defn ^:export refresh []
  (log/info "Hot code Remount")
  (cssi/upsert-css "componentcss" {:component root/Root})
  (app/mount! SPA root/Root "app"))

(defn ^:export init []
  (init-f7)
  (cssi/upsert-css "componentcss" {:component root/Root})
  ;(inspect/app-started! SPA)
  (app/set-root! SPA root/Root {:initialize-state? true})
  (dr/initialize! SPA)
  (log/info "Starting session machine.")
  (app/mount! SPA root/Root "app" {:initialize-state? false}))
