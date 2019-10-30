(ns app.cordova-preload
  (:require
    [com.fulcrologic.fulcro.algorithms.timbre-support :as ts]
    [taoensso.timbre :as log]
    [cljs.core.async :as async :refer [go]]
    [com.fulcrologic.fulcro.inspect.inspect-client :as inspect]
    [com.fulcrologic.fulcro.inspect.inspect-ws :as iws]))


(def SERVER_CONF {
                  :url "http://10.0.2.2"
                  :port "8237"})

(defn server-uri
  [conf]
  (str (:url SERVER_CONF) ":" (:port SERVER_CONF))
  )

(defn start-ws-messaging! []
  (try
    (let [socket (iws/websockets  (server-uri SERVER_CONF) (fn [msg]
                                                             (inspect/handle-devtool-message msg)))]
      (iws/start socket)
      (async/go-loop []
                     (when-let [[type data] (async/<! inspect/send-ch)]
                       (iws/push socket {:type type :data data :timestamp (js/Date.)})
                       (recur))))
    (catch :default e
      (log/error e "Unable to start inspect."))))

(defn install-ws []
  (when-not @inspect/started?*
    (log/info "Installing Fulcro 3.x Inspect over Websockets targeting server " (server-uri SERVER_CONF))
    (reset! inspect/started?* true)
    (start-ws-messaging!)))

(install-ws)

