(ns app.application
  (:require [com.fulcrologic.fulcro.networking.http-remote :as net]
            [com.fulcrologic.fulcro.algorithms.tx-processing :as tx]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp]
            [edn-query-language.core :as eql]
            ["osmtogeojson" :as osmtogeojson]
            ["@mapbox/vt2geojson" :as vt2geojson]))

(def secured-request-middleware
  ;; The CSRF token is embedded via server_components/html.clj
  (->
    (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
    (net/wrap-fulcro-request)))

(defn mvt-remote []
  {:active-requests (atom {})
   :transmit! (fn transmit! [remote {::tx/keys [result-handler update-handler ast] :as send-node}]
                  (let [query (get-in ast [:children 0 :params :query])
                        edn (eql/ast->query ast)]
                       (vt2geojson (clj->js query)
                                   (fn [error result]
                                       (result-handler {:body {:_ (if error error (js->clj result :keywordize-keys true))}
                                                        :transaction edn
                                                        :status-code (if error 500 200)})))))})

(defonce SPA (app/fulcro-app
               {;; This ensures your client can talk to a CSRF-protected server.
                ;; See middleware.clj to see how the token is embedded into the HTML
                :remotes {:pathom (net/fulcro-http-remote {:url "/api"
                                                           :request-middleware secured-request-middleware})
                          :overpass (net/fulcro-http-remote
                                      {:url "http://overpass-api.de/api/interpreter"
                                       :request-middleware (fn [req] (let [query (->> req :body first (apply hash-map) :_ :query)]
                                                                          (assoc req :headers {"Content-Type" "text/plain"}
                                                                                     :body (str "[out:json];" (apply str query) "out;"))))
                                       :response-middleware (fn [resp] (let [data (some-> (:body resp)
                                                                                          js/JSON.parse
                                                                                          osmtogeojson
                                                                                          (js->clj :keywordize-keys true))]
                                                                            (assoc resp :body {:_ data}))) })
                          :mvt (mvt-remote)}}))
