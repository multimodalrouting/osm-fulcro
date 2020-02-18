(ns app.client-f7
  (:require
    [app.application :refer [SPA]]
    [app.ui.root-f7 :as root]
    [com.fulcrologic.fulcro.components :refer [transact!]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro-css.css-injection :as cssi]
    ["framework7/framework7-lite.esm.bundle.js" :default Framework7]
    ["framework7-react/framework7-react.esm.js" :default Framework7React]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.inspect.inspect-client :as inspect]
    [app.background-geolocation :refer [bg-prepare!] ]
    ))

(defn init-f7 []
  (.use Framework7 Framework7React))


(defn ^:export refresh []
  (log/info "Hot code Remount")
  (cssi/upsert-css "componentcss" {:component root/AppMain})
  (app/mount! SPA root/AppMain "app"))

(defn ^:export init []
  (init-f7)
  (cssi/upsert-css "componentcss" {:component root/AppMain})
  ;(inspect/app-started! SPA)
  (app/set-root! SPA root/AppMain {:initialize-state? true})
  (dr/initialize! SPA)
  (log/info "Starting session machine.")
  (app/mount! SPA root/AppMain "app" {:initialize-state? false}))
