(ns app.application
  (:require [com.fulcrologic.fulcro.networking.http-remote :as net]
            [com.fulcrologic.fulcro.algorithms.tx-processing :as tx]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp]
            [edn-query-language.core :as eql]
            ["osmtogeojson" :as osmtogeojson]
            ["@mapbox/vt2geojson" :as vt2geojson]
            [app.model.geofeatures :as gf]))

(def secured-request-middleware
  ;; This ensures your client can talk to a CSRF-protected server.
  ;; See middleware.clj to see how the token is embedded into the HTML
  ;; The CSRF token is embedded via server_components/html.clj
  (-> (net/wrap-csrf-token (if (js/window.hasOwnProperty "fulcro_network_csrf_token")
                               js/fulcro_network_csrf_token
                               ""))
      (net/wrap-fulcro-request)))

(defn mvt-remote []
  {:active-requests (atom {})
   :transmit! (fn transmit! [remote {::tx/keys [result-handler update-handler ast] :as send-node}]
                  (let [params (get-in ast [:children 0 :params :args])
                        edn (eql/ast->query ast)]
                       (vt2geojson (clj->js params)
                                   (fn [error result]
                                       (result-handler {:body {[::gf/id :mvt-loschwitz] {::gf/geojson (if error error (js->clj result :keywordize-keys true))}}  ;; TODO
                                                        :transaction edn
                                                        :status-code (if error 500 200)})))))})

(def SPA_conf {:remotes {:pathom (net/fulcro-http-remote {:url #_ "/api" "http://localhost:3000/api" ;; TODO from configuration
                                                          :request-middleware secured-request-middleware})
                         :overpass (net/fulcro-http-remote
                                     {:url "http://overpass-api.de/api/interpreter"
                                      :request-middleware (fn [req] (let [params (->> req :body first second :args)]
                                                                         (assoc req :headers {"Content-Type" "text/plain"}
                                                                                    :body (str "[out:json];" (apply str params) "out;"))))
                                      :response-middleware (fn [resp] (let [data (some-> (:body resp)
                                                                                         js/JSON.parse
                                                                                         osmtogeojson
                                                                                         (js->clj :keywordize-keys true))]
                                                                           (assoc resp :body {[::gf/id :overpass-example] {::gf/geojson data}})))}) ;; TODO
                         :mvt (mvt-remote)}})

(defn conf-with-default-remote
  "add the default remote (called :remote)"
  [conf default-remote]
  (update-in conf [:remotes] (fn [remotes] (assoc remotes :remote (get remotes default-remote)))))

(defonce SPA (app/fulcro-app (conf-with-default-remote SPA_conf :pathom)))
