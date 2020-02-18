(ns app.client-config)

;; TODO currently not used, find a proper config system for client analog to server config
(def default-config {
                     :app.server.pathom-demo/config     {:url "/api"}
                     :app.server.graphhopper-web/config {:url "/route"}
                     })

(def config {
             :app.server.pathom-demo/config     {:url "http://localhost/api"}
             :app.server.pathom-gpx/config      {:url "http://localhost/api"}
             ;:app.server.graphhopper-web/config {:url "http://10.0.0.2:8989/route"}
             })


