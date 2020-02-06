(ns app.ui.leaflet.example-ws
  (:require [nubank.workspaces.core :refer [defcard]]
            [nubank.workspaces.card-types.fulcro3 :as ct.fulcro]
            [nubank.workspaces.model :as wsm]
            [com.fulcrologic.fulcro.components :refer [defsc factory get-query]]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.data-fetch :refer [refresh!]]
            [com.fulcrologic.fulcro.dom :as dom]
            [app.model.geofeatures :as gf]
            [app.application :refer [SPA_conf]]
            [app.client :refer [load-all!]]
            [app.ui.root :as root]
            [app.ui.leaflet :refer [leaflet Leaflet]]))

(def r (factory root/Root))

(defsc LoadDataset
  [this {:as props}]
  {:ident         (fn [] [::gf/id :vvo])
   :query         [::gf/id ::gf/source ::gf/geojson ::app/active-remotes]}
  #_(when-not (get props ::gf/id)
            (refresh! this {:remote :pathom})
            #_(load-all! this))
  (dom/div {:classes ["ui" "steps" "unstackable"]}
    (dom/div {:classes ["step" "completed"]}
      (dom/i {:classes ["icon" "ellipsis" "horizontal"]})
      (dom/div {:classes ["content"]}
        (dom/div {:classes ["title"]} "Layers")
        (dom/div {:classes ["description"]} "loaded")))
    (dom/div {:classes ["step" "completed"]}
      (dom/i {:classes ["icon" "ellipsis" "horizontal"]})
      (dom/div {:classes ["content"]}
        (dom/div {:classes ["title"]} "Datasets")
        (dom/div {:classes ["description"]} "loaded")))
    (dom/div {:classes ["step" "active"]}
      (dom/i {:classes ["icon" "ellipsis" "horizontal"]})
      (dom/div {:classes ["content"]}
        (dom/div {:classes ["title"]} "Graph")
        (dom/div {:classes ["description"]} "being calculated")))
    (dom/div {:classes ["step"]}
      (dom/i {:classes ["icon" "ellipsis" "horizontal"]})
      (dom/div {:classes ["content"]}
        (dom/div {:classes ["title"]} "Route")
        (dom/div {:classes ["description"]} "waiting"))))

  #_(if-not (empty? (::app/active-remotes props))
          (dom/div {:classes ["ui" "active" "dimmer"]}
            (dom/div {:classes ["ui" "loader" "text"]} "Loading"))))

(defsc Root [this props]
  {:query (fn [] (into (get-query LoadDataset)
                       (get-query Leaflet)))}
  (dom/div {:style {:width "100%" :height "100%"}}
   (leaflet props)
   ((factory LoadDataset) props))) ;; infinite loop

(defcard example
  {::wsm/align {:flex 1}  ;; fullscreen
   #_#_::wsm/node-props {:style {:background "red"}}}
  (ct.fulcro/fulcro-card
    {::ct.fulcro/root Root
     ::ct.fulcro/wrap-root? false
     ::ct.fulcro/app SPA_conf #_(update-in SPA_conf [:remotes] select-keys [:pathom])}))
