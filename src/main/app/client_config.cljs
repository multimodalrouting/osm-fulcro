(ns app.client-config)

;; TODO currently not used, find a proper config system for client analog to server config
(def default-config {
                     :app.server.pathom-demo/config     {:url "/api"}
                     :app.server.graphhopper-web/config {:url "/route"}
                     })

(def config2 {
             :app.server.pathom-demo/config     {:url "http://192.168.122.1:3000/api"}
             :app.server.pathom-gpx/config      {:url "http://192.168.122.1:3000/api"}
             :app.server.graphhopper-web/config {:url "http://192.168.122.1:8989/route"}
             })


(def config {
             :app.server.pathom-demo/config     {:url "https://multimodal.gra.one/api"}
             :app.server.pathom-gpx/config      {:url "https://multimodal.gra.one/api"}
             :app.server.graphhopper-web/config {:url "http://10.0.2.2:8989/route"}
             })
