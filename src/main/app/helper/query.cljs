(ns app.helper.query
  (:require [clojure.string :as string]))

(defn get-query-params-str [query-params-map & [query-param-str]]
  "Converts a hash-map to a query param string containing the keys and values
   in the hash-map
   Note: this function assumes the values have already been url-encoded"
  (let [query-param-str (str query-param-str)
        query-param-str-blank? (string/blank? query-param-str)
        key (first (keys query-params-map))
        query-param-key (name (or key ""))
        query-param-val (and key (key query-params-map))]
    (case (count query-params-map)
      0 nil
      1 (string/join
          (flatten [(if query-param-str-blank?
                      ["?"]
                      [query-param-str "&"])
                    (if (vector? query-param-val)
                       (vec (butlast (flatten (for [query-param-val-val query-param-val]
                                        [query-param-key "=" query-param-val-val "&"]
                                        ))))
                      [query-param-key "=" query-param-val])]))
      (apply get-query-params-str
             (if query-param-str-blank?
               [query-params-map (str "?")]
               [(into {} (rest query-params-map))
                (str query-param-str
                     (when-not (= query-param-str "?") "&")
                     (string/join (flatten (if (vector? query-param-val)
                                             (vec (butlast (flatten (for [query-param-val-val query-param-val]
                                                                      [query-param-key "=" query-param-val-val "&"]
                                                                      ))))
                                             [query-param-key "=" query-param-val]
                                             ))))])))))
(comment

  (get-query-params-str {
                         :point          ["51.02932979285427,13.729509787911619" "51.02932979285427,13.729509787911619"]
                         :vehicle        "car"
                         :locale         "de"
                         :calc_points    "true"
                         :points_encoded "false"
                         :instructions   "false"
                         :key            "api_key"

                         }))
