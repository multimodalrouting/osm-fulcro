(ns app.client
  (:require
    [app.application :refer [SPA]]
    [com.fulcrologic.fulcro.application :as app]
    [app.ui.root :as root]
    [com.fulcrologic.fulcro.networking.http-remote :as net]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro-css.css-injection :as cssi]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.inspect.inspect-client :as inspect]
    [com.fulcrologic.fulcro.rendering.keyframe-render :refer [render!]]
    [app.ui.root]))

(defn load! []
  #_(df/load! SPA :geojson.vvo/geojson nil)

  ;; This is the transact! caused by the load! above 
  #_(let [il `df/internal-load!
        lp (df/load-params* app.application/SPA :geojson.vvo/geojson nil nil)]
       (comp/transact! app.application/SPA `[(~il ~lp)]))

  ;; This loads from the remote `overpass`
  (let [il `df/internal-load!
        lp (assoc (df/load-params* app.application/SPA :geojson.vvo/geojson nil nil)
                  :remote :overpass)]
       (comp/transact! app.application/SPA `[(~il ~lp)])))

(defn ^:export refresh []
  (log/info "Hot code Remount")
  (cssi/upsert-css "componentcss" {:component root/Root})
  (app/mount! SPA root/Root "app"))

(defn ^:export init []
  (log/info "Application starting.")
  (cssi/upsert-css "componentcss" {:component root/Root})
  ;(inspect/app-started! SPA)
  (app/set-root! SPA root/Root {:initialize-state? true})
  (dr/initialize! SPA)
  (app/mount! SPA root/Root "app" {:initialize-state? false})
  (load!))
