(ns app.application
  (:require [com.fulcrologic.fulcro.networking.http-remote :as net]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp]
            ["osmtogeojson" :as osmtogeojson]))

(def secured-request-middleware
  ;; The CSRF token is embedded via server_components/html.clj
  (->
    (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
    (net/wrap-fulcro-request)))

(defonce SPA (app/fulcro-app
               {;; This ensures your client can talk to a CSRF-protected server.
                ;; See middleware.clj to see how the token is embedded into the HTML
                :remotes {:remote (net/fulcro-http-remote {:url "/api"
                                                           :request-middleware secured-request-middleware})
                          :overpass (net/fulcro-http-remote
                                      {:url "http://overpass-api.de/api/interpreter?data=%5Bout%3Ajson%5D%3B%0Aarea%5Bname%3D%22Dresden%22%5D-%3E.city%3B%0Anwr%28area.city%29%5Boperator%3D%22DVB%22%5D-%3E.connections%3B%0A%0Arelation.connections%5Broute%3Dbus%5D%3B%20%28._%3B%3E%3B%29-%3E.bus%3B%0Away.connections%5Brailway%3Dtram%5D%3B%20%28._%3B%3E%3B%29-%3E.tram%3B%0A%0A%28.tram%3B%20.bus%3B%29%3B%0A%0Aout%3B%0A%0A%0A%0A%0A%0A%0A%0A%0A%0A%0A%0A%0A%0A"
                                       :method :get
                                       :request-middleware (fn [req] (assoc req :method :get))
                                       :response-middleware (fn [resp] (let [data (some-> (:body resp)
                                                                                          js/JSON.parse
                                                                                          osmtogeojson
                                                                                          (js->clj :keywordize-keys true))]
                                                                            (assoc resp :body {:geojson.vvo/geojson data}))) })}}))
