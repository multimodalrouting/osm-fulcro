(ns app.ui.steps-ws
  (:require [app.ui.steps :as steps]
            [app.ui.steps-helper :refer [title->step-index]]
            [app.ui.leaflet.state :refer [layers->dataset->graph->route]]
            [nubank.workspaces.core :refer [defcard]]
            [nubank.workspaces.card-types.fulcro3 :as ct.fulcro]
            [nubank.workspaces.model :as wsm]
            [com.fulcrologic.fulcro.components :refer [defsc transact!]]
            [com.fulcrologic.fulcro.dom :as dom]
            [app.application :refer [SPA_conf]]))

(defsc Root [this props]
  {;:ident ::root   ;; This would enable „ident-optimized-render“ and stop rerendering the effects of the mutation
                    ;; TODO What is the clean solution of setting an ident to this component?
   :initial-state (fn [_] (update-in layers->dataset->graph->route [::steps/id :layers->dataset->graph->route ::steps/step-list] 
                                     (fn [step-list] (-> step-list (assoc-in [(title->step-index "Layers" step-list) :state] :failed)
                                                                   (assoc-in [(title->step-index "Geofeatures" step-list) :state] :done)
                                                                   (assoc-in [(title->step-index "Geofeatures" step-list) :info] "1337 Features")
                                                                   (assoc-in [(title->step-index "Graph" step-list) :state] :active)))))
   :query (fn [] [[::steps/id :layers->dataset->graph->route]])}
  (dom/div {:style {:width "100%" :height "100%"}}
    (dom/div {:classes ["ui" "message"]} "This card shows how the „Steps“-component can be used to visualize progress stored at Fulcro-DB")

    (steps/steps (merge (get props [::steps/id :layers->dataset->graph->route])
                        {:style {:width "100%"}}))

    (dom/button {:onClick #(transact! this [(steps/update-state-of-step {:steps :layers->dataset->graph->route
                                                                         :step (rand-int 4)
                                                                         :new-state (rand-nth [:done :failed :active :queued])})])}
                "Random change of state")))

(defcard example
  {::wsm/align {:flex 1}  ;; fullscreen
   #_#_::wsm/node-props {:style {:background "red"}}}
  (ct.fulcro/fulcro-card
    {::ct.fulcro/root Root
     ::ct.fulcro/wrap-root? false
     ::ct.fulcro/app (update-in SPA_conf [:remotes] select-keys [:pathom])}))
