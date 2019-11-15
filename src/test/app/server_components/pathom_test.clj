(ns app.server-components.pathom-test
  (:require
    [clojure.test :refer [use-fixtures deftest testing is]]
    [mount.core :as mount]
    [app.server-components.pathom :refer [parser]]
    [app.model.geofeatures :as gf]))

(use-fixtures :once
  (fn [f]
      (-> (mount/only #{#'parser})
          (mount/start))
      (f)))


(deftest parser-test

  (testing "A merge can return the `source` of a specific dataset"
    (is (-> (parser {} [{[::gf/id :vvo] [::gf/source]}])
            (get-in [[::gf/id :vvo] ::gf/source]))))

  (testing "Querying the root should return the `id` of each dataset"
    (is (= [::gf/id]
           (-> (parser {} [::gf/all])
               ::gf/all first keys))))

  (testing "Querying `source` for every dataset"
    (is (= [::gf/source]
           (-> (parser {} [{::gf/all [::gf/source]}])
               ::gf/all first keys))))

  (testing "Querying `name` and `source` for every dataset"
    (is (= [::gf/id ::gf/source]
           (-> (parser {} [{::gf/all [::gf/id ::gf/source]}])
               ::gf/all first keys)))))

(deftest vvo-geojson-test
  (testing "Count the features of the dataset :vvo"
    (is (= 8999
           (-> (parser {} [{[::gf/id :vvo] [::gf/geojson]}])
               (get-in [[::gf/id :vvo] ::gf/geojson :features]) count)))))
