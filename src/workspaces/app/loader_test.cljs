(ns app.loader-test
  (:require [cljs.test :as t :refer [use-fixtures async deftest is]]
            [clojure.core.async :refer [go chan <! put!]]
            [com.fulcrologic.fulcro.application :refer [fulcro-app set-root! current-state]]
            [com.fulcrologic.fulcro.mutations :refer [defmutation]]
            [com.fulcrologic.fulcro.data-fetch :refer [load!]]
            [app.model.geofeatures :as gf]
            [app.loader-ws :refer [LoadDataset]]
            [app.application :refer [SPA_conf]]))

;; We use the channel `loaded?` to wait in the fixture till the dataset was loaded

(def loaded? (chan))

(defmutation loaded [params]
  (action [{:keys [a s]}]
    (put! loaded? true)))


;; The APP we use for testing

(defonce APP (fulcro-app SPA_conf))
(set-root! APP LoadDataset {:initialize-state? true})


;; The fixtures are async

(use-fixtures :once
  {:before (fn [] (load! APP [::gf/id :vvo] LoadDataset {:remote :pathom :post-mutation `loaded})
                  (async done
                         (go (<! loaded?)
                             (done))))})


;; The tests itself can be sync

(deftest load-dataset
  (let [state (current-state APP)]
       (is (= 8999 (count (get-in state [::gf/id :vvo ::gf/geojson :features]))))))
