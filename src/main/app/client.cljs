(ns app.client
  (:require
    [app.application :refer [SPA]]
    [app.ui.root :as root]
    [app.ui.leaflet.state :refer [mutate-datasets mutate-layers]]
    [com.fulcrologic.fulcro.components :refer [transact!]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro-css.css-injection :as cssi]
    [taoensso.timbre :as log]))

(defn ^:export refresh []
  (js/console.clear)
  (log/info "Hot code Remount")
  (cssi/upsert-css "componentcss" {:component root/Root})
  (app/mount! SPA root/Root "app"))

(defn ^:export init []
  (log/info "Application starting.")
  (cssi/upsert-css "componentcss" {:component root/Root})
  ;(inspect/app-started! SPA)
  (app/set-root! SPA root/Root {:initialize-state? true})
  ;(dr/initialize! SPA)
  (app/mount! SPA root/Root "app" {:initialize-state? false}))
