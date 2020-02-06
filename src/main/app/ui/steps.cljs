(ns app.ui.steps
  (:require [com.fulcrologic.fulcro.components :refer [defsc factory]]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.dom :as dom]))

(defsc Steps
  [this {::keys [id step-list] :as props}]
  {:ident (fn [] [::steps id])
   :query (fn [] [::id ::step-list])}
  (dom/div {:classes ["ui" "steps" "unstackable"]
            :style {:width "100%"}}
    (for [step step-list]
      (dom/div {:key (:title step)
                :classes ["step"
                          (case (:state step)
                                :active "active"
                                :done "completed"
                                nil)]}
        (dom/i {:classes (concat ["icon"]
                                 (case (:state step)
                                       :failed ["exclamation" "red"]
                                       :active ["spinner" "loading"]
                                       ["ellipsis" "horizontal"]))})
        (dom/div {:classes ["content"]}
          (dom/div {:classes ["title"]} (:title step))
          (dom/div {:classes ["description"]} (get (:contents step) (:state step)))
          (dom/div {:classes ["description"]} (:info step)))))))

(def steps (factory Steps))

(defmutation update-state-of-step [{:keys [steps step new-state info] :as params}]
  (action [{:keys [state]}]
          (swap! state assoc-in [::id steps ::step-list step :state] new-state)
          (swap! state assoc-in [::id steps ::step-list step :info] info)))
