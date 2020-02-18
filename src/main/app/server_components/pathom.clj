(ns app.server-components.pathom
  (:require
    [mount.core :refer [defstate]]
    [taoensso.timbre :as log]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.core :as p]
    [com.wsscode.common.async-clj :refer [let-chan]]
    [clojure.core.async :as async]
    [app.server-components.config :refer [config]]
    [jsonista.core :as json]
    [app.model.geofeatures :as gf]))

(def geofeatures {;:vvo {::gf/source {:comment "VVO stops+lines"
                  ;                   :remote :pathom :type :geojson}
                  :vvo-small {::gf/source {:comment "VVO stops+lines (north-west)"
                                           :remote :pathom :type :geojson}}
                  :trachenberger {::gf/source {:comment "Trachenberger Platz: Streets+Buildings"
                                               :remote :pathom :type :geojson}}
                  :overpass-example {::gf/source {:remote :overpass :type :geojson
                                                  :query ["area[name=\"Dresden\"]->.city;"
                                                          "nwr(area.city)[operator=\"DVB\"]->.connections;"
                                                          "node.connections[public_transport=stop_position];"]}}
                  :mvt-loschwitz {::gf/source {:remote :mvt :type :geojson
                                               :query {:uri "http://localhost:8989/mvt/13/4410/2740.mvt"
                                                       :layer "roads"}}}})

(pc/defresolver gf-all [_ _]
  {::pc/output [::gf/all ::gf/id]}
  {::gf/all (into [] (map #(hash-map ::gf/id (key %))
                          geofeatures))})

(pc/defresolver gf-source [_ {::gf/keys [id]}]
  {::pc/input #{::gf/id}
   ::pc/output [::gf/source]}
  (-> (get geofeatures id)
      (select-keys [::gf/source])))

(pc/defresolver gf-geojson-file [_ {::gf/keys [id]}]
  {::pc/input #{::gf/id}
   ::pc/output [::gf/geojson]}
  (let [known_files {:vvo "resources/test/vvo.geojson"
                     :vvo-small "resources/test/vvo-small.geojson"
                     :trachenberger "resources/test/trachenberger.geojson"}]
       (if-let [filename (get known_files id)]
               {::gf/geojson (json/read-value (slurp filename)
                                              (json/object-mapper {:decode-key-fn true}))})))

(pc/defresolver index-explorer [env _]
  {::pc/input #{:com.wsscode.pathom.viz.index-explorer/id}
   ::pc/output [:com.wsscode.pathom.viz.index-explorer/index]}
  {:com.wsscode.pathom.viz.index-explorer/index
   (-> (get env ::pc/indexes)
       (update ::pc/index-resolvers #(into [] (map (fn [[k v]] [k (dissoc v ::pc/resolve)])) %))
       (update ::pc/index-mutations #(into [] (map (fn [[k v]] [k (dissoc v ::pc/mutate)])) %)))})

(defn all-resolvers [] [index-explorer gf-all gf-source gf-geojson-file])

(defn preprocess-parser-plugin
  "Helper to create a plugin that can view/modify the env/tx of a top-level request.

  f - (fn [{:keys [env tx]}] {:env new-env :tx new-tx})

  If the function returns no env or tx, then the parser will not be called (aborts the parse)"
  [f]
  {::p/wrap-parser
   (fn transform-parser-out-plugin-external [parser]
     (fn transform-parser-out-plugin-internal [env tx]
       (let [{:keys [env tx] :as req} (f {:env env :tx tx})]
         (if (and (map? env) (seq tx))
           (parser env tx)
           {}))))})

(defn log-requests [{:keys [env tx] :as req}]
  (log/debug "Pathom transaction:" (pr-str tx))
  req)

(defn build-parser [db-connection]
  (let [real-parser (p/parallel-parser
                      {::p/mutate  pc/mutate-async
                       ::p/env     {::p/reader [p/map-reader pc/parallel-reader
                                                pc/open-ident-reader p/env-placeholder-reader]
                                    ::p/placeholder-prefixes #{">"}}
                       ::p/plugins [(pc/connect-plugin {::pc/register (all-resolvers)})
                                    (p/env-wrap-plugin (fn [env]
                                                         ;; Here is where you can dynamically add things to the resolver/mutation
                                                         ;; environment, like the server config, database connections, etc.
                                                         (assoc env
                                                           ;:db @db-connection ; real datomic would use (d/db db-connection)
                                                           :connection db-connection
                                                           :config config)))
                                    (preprocess-parser-plugin log-requests)
                                    p/error-handler-plugin
                                    p/request-cache-plugin
                                    (p/post-process-parser-plugin p/elide-not-found)
                                    p/trace-plugin]})
        ;; NOTE: Add -Dtrace to the server JVM to enable Fulcro Inspect query performance traces to the network tab.
        ;; Understand that this makes the network responses much larger and should not be used in production.
        trace?      (not (nil? (System/getProperty "trace")))]
    (fn wrapped-parser [env tx]
      (async/<!! (real-parser env (if trace?
                                    (conj tx :com.wsscode.pathom/trace)
                                    tx))))))

(defstate parser
  :start (build-parser nil))

(comment
  (parser {} [{::gf/all [::gf/source]}]))
