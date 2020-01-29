(ns app.server-components.middleware
  (:require
    [app.server-components.config :refer [config]]
    [app.server-components.pathom :refer [parser]]
    [mount.core :refer [defstate]]
    [com.fulcrologic.fulcro.server.api-middleware :refer [handle-api-request
                                                          wrap-transit-params
                                                          wrap-transit-response]]
    [ring.middleware.defaults :refer [wrap-defaults]]
    [ring.middleware.gzip :refer [wrap-gzip]]
    [ring.middleware.file :refer [wrap-file]]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.middleware.webjars :refer [wrap-webjars]]
    [ring.util.response :refer [response file-response resource-response]]
    [ring.util.response :as resp]
    [hiccup.page :refer [html5]]
    [taoensso.timbre :as log]))

(def ^:private not-found-handler
  (fn [req]
    {:status  404
     :headers {"Content-Type" "text/plain"}
     :body    "NOPE"}))


(defn wrap-api [handler uri]
  (fn [request]
    (if (= uri (:uri request))
      (handle-api-request
        (:transit-params request)
        (fn [tx] (parser {:ring/request request} tx)))
      (handler request))))


;; ================================================================================
;; Dynamically generated HTML. We do this so we can safely embed the CSRF token
;; in a js var for use by the client.
;; ================================================================================
(defn index [csrf-token &[{:keys [main title]
                           :or {main "js/main/main.js"
                                tile "Application"}}]]
  (html5
    [:html {:lang "en"}
     [:head {:lang "en"}
      [:title title]
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"}]
      [:link {:rel "stylesheet" :href "webjars/Semantic-UI/semantic.min.css"}]
      [:link {:rel "stylesheet" :href "@fortawesome/fontawesome-free/css/all.css"}]
      ;[:link {:rel "stylesheet" :href "leaflet/dist/leaflet.css"}]
      [:link {:rel "stylesheet" :href "https://api.tiles.mapbox.com/mapbox.js/v2.1.9/mapbox.css"}]
      [:style {:type "text/css"} (str
        ".sidebar-pane {padding: 0 !important;}"
        "@media (min-width: 768px) {"
        " .sidebar {right: 10px !important; width: unset !important;}}"
        "@media (max-width: 991px) and (min-width: 768px) {"
        " .sidebar {right: 10px !important; width: unset !important;}"
        " .sidebar-left .sidebar-content {right: 0 !important;}}")]
      [:link {:rel "shortcut icon" :href "data:image/x-icon;," :type "image/x-icon"}]
      [:script (str "var fulcro_network_csrf_token = '" csrf-token "';")]]
     [:body
      [:div#app {:style "width: 100%; height: 100%"}]
      [:script {:src "d3/dist/d3.min.js"}]
      [:script {:src main}]]]))

(defn -main [token & args]
  (prn (index token)))

(defn wrap-html-routes [ring-handler]
  (fn [{:keys [uri anti-forgery-token] :as req}]
    (cond
      (#{"/" "/index.html"} uri)
      (-> (resp/response (index anti-forgery-token))
          (resp/content-type "text/html"))

      ;; Workspaces can be accessed via shadow's http server on http://localhost:8023/workspaces.html
      (#{"/wslive.html"} uri)
      (-> (resp/response (index anti-forgery-token {:main "workspaces/js/main.js" :title "devcards"}))
          (resp/content-type "text/html"))

      :else
      (ring-handler req))))

(defstate middleware
  :start
  (let [defaults-config (:ring.middleware/defaults-config config)
        legal-origins (get config :legal-origins [#".*"])]
    (-> not-found-handler
      (wrap-api "/api")
      (wrap-webjars "/webjars")
      (wrap-file "./node_modules")
      wrap-transit-params
      wrap-transit-response
      (wrap-html-routes)
      ;; If you want to set something like session store, you'd do it against
      ;; the defaults-config here (which comes from an EDN file, so it can't have
      ;; code initialized).
      ;; E.g. (wrap-defaults (assoc-in defaults-config [:session :store] (my-store)))
      (wrap-defaults defaults-config)
      (wrap-cors :access-control-allow-origin legal-origins 
                 :access-control-allow-methods [:get :post])
      wrap-gzip)))
