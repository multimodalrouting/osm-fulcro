(ns app.loader-ws
  (:require [nubank.workspaces.core :refer [defcard]]
            [nubank.workspaces.card-types.fulcro3 :as ct.fulcro]
            [com.fulcrologic.fulcro.components :refer [defsc]]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.data-fetch :refer [refresh!]]
            [com.fulcrologic.fulcro.dom :as dom]
            [app.model.geofeatures :as gf]
            [app.application :refer [SPA_conf]]))

(defsc LoadDataset
  [this {:as props}]
  {:ident         (fn [] [::gf/id :vvo])
   :query         [::gf/id ::gf/source ::gf/geojson ::app/active-remotes]}
  (if-not (get props ::gf/id)
          (refresh! this {:remote :pathom}))
  (if-not (empty? (::app/active-remotes props))
          "loading…"
          (dom/div 
            (dom/p (str "source: "
                        (get-in props [::gf/id :vvo ::gf/source])))
            (dom/p (str "features: "
                        (count (get-in props [::gf/id :vvo ::gf/geojson :features])))))))

(defcard load-dataset
  (ct.fulcro/fulcro-card
    {::ct.fulcro/root LoadDataset
     ::ct.fulcro/wrap-root? false
     ::ct.fulcro/app (update-in SPA_conf [:remotes] select-keys [:pathom])}))
