(ns app.background-geolocation
  (:require [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.common.async-clj :refer [let-chan]]
            [clojure.core.async :as async]
            [app.server-components.config :refer [config]]
            [clojure.spec.gen.alpha :as sgen]
            [clojure.spec.alpha :as s]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [clojure.java.io :refer [writer]]
            [app.utils.gpx :refer [geo->gpx-xml]]
            ))

(s/def ::latitude double?)
(s/def ::longitude double?)
(s/def ::accuracy pos-int?)
(s/def ::speed double?)
(s/def ::heading pos-int?)
(s/def ::altitude pos-int?)

(s/def ::coords (s/keys ::latitude ::longitude ::accuracy ::speed ::heading ::altitude))

(s/def ::is_moving boolean?)
(s/def ::odometer double?)
(s/def ::uuid string?)
(s/def ::timestamp inst?)
(s/valid? ::timestamp inst? (java.util.Date.))
(s/def ::event string?)

(s/def ::pos (s/keys :req [::coords ::is_moving ::odometer ::uid ::timestamp]))

(pc/defresolver latest-track [_ _]
  {::pc/output [{::latest-track [::uuid ::coords ::timestamp]}]}
  {::latest-track {::uiid      "102312321"
                   ::coords    {}
                   ::timestamp (tc/to-sql-time (time/now))}})

(def gpx-tracks (atom {:background-location/tracks []}))

(comment
  (prn @gpx-tracks)
  )

(defn save-track [track fileName]
  (with-open [wrtr (writer fileName)
              xml (geo->gpx-xml {:tracks track})
              ]
    (prn track)
    (.write wrtr xml)
    (.close wrtr)))

(pc/defmutation save-gpx-track [env {:background-location/keys [track]}]
  {
   ::pc/params  [:background-location/track]
   ::pc/output [::save-gpx-track ::count-tracks]}
  (do
    (prn track)
    (swap! gpx-tracks update-in [:background-location/tracks] #(concat % track))
    (save-track track (str "/tmp/gpx/geo-" (quot (System/currentTimeMillis) 1000) ".gpx"))
    {::save-gpx-track true
     ::count-tracks   (count (:background-location/tracks @gpx-tracks))
     }))


(def example-db (atom {:example-counter 42 :message ""}))

(pc/defmutation send-message [env {:keys [message/text]}]
  {
   ::pc/params [:message/text]
   ::pc/output [:message/id :message/text]}
  (let [oldstate (:message @example-db)]
    (swap! example-db update-in [:message] #(str % text))

    {:message/id   123
     :message/text oldstate}
    )
  )

(pc/defresolver mutation-by-resolver [env props]
  {::pc/input #{:example-input}
   ::pc/output [:example-output]}
  (let [oldstate (:example-counter @example-db)]
    (swap! example-db update-in [:example-counter] #(+ % (:example-input props)))
    {:example-output oldstate}))



(def parser
  (p/parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/reader2
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}}
     ::p/mutate  pc/mutate
     ::p/plugins [(pc/connect-plugin {::pc/register [latest-track save-gpx-track send-message]})
                  p/error-handler-plugin
                  p/trace-plugin]}))

(comment
  (parser {} [{[:background-location/track {::coords []}] [:background-location/success :background-location/count-tracks]}])

  (parser {} ['(send-message {:message/text "Hello Clojurist!"})])

  (parser {} [{[:example-input 23] [:example-output]}])
  )


(comment
  "please run this let-binding and you see that the output gets increased by 23 on every call"
  (->> (parser {} [{[:example-input 23] [:example-output]}])))
