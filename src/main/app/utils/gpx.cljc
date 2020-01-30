(ns app.utils.gpx
  (:require [clojure.data.xml :as xml]
            ))

(defn makeDate [d]
  #?(:clj  (.getTime (javax.xml.bind.DatatypeConverter/parseDateTime d))
     :cljs (js/Date. d)
     ))

(defn time->iso8601 [d]
  #?(:cljs (.toISOString d)
     :clj  (let [tz (java.util.TimeZone/getTimeZone "UTC")
                 sdf (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")]
             (.setTimeZone sdf tz)
             (.format sdf d)
             )))

(defn geo-track->gpx [track]
  (xml/element
    :trk nil
    (xml/element*
      :trkseg nil
      (->> (:tracks track)
           (map (fn [t]
                  (let [c (:coords t)]
                    (xml/element
                      :trkpt {
                             :lat (str (:latitude c))
                             :lon (str (:longitude c))}
                      (xml/element :ele nil (str (:altitude c)))
                      (xml/element :time nil (str (time->iso8601 (:timestamp t))))
                      (xml/element :speed nil (str (:speed c)))
                      (xml/element :magvar nil (str (:heading c)))
                      ))))))))

(defn geo->gpx [tracks]
  (xml/element :gpx {:version "1.1" :creator "osm-fulcro"}
           (geo-track->gpx tracks)))

(defn geo->gpx-xml [tracks]
  #?(:clj  (xml/emit-str (geo->gpx tracks))
     :cljs (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                (xml/emit-str (xml/element-node (geo->gpx tracks))))
     ))

