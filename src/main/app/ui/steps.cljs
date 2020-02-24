(ns app.ui.steps
  (:require [com.fulcrologic.fulcro.components :refer [defsc factory transact!]]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.dom :as dom]
            [app.ui.steps-helper :refer [title->step-index]]))

(defmutation update-state-of-step [{:keys [steps step new-state info info-popup]}]
  (action [{:keys [state]}]
          (swap! state assoc-in [::id steps ::step-list step :state] new-state)
          (swap! state assoc-in [::id steps ::step-list step :info] info)
          (swap! state assoc-in [::id steps ::step-list step :info-popup] info-popup)))

(defn update-state-of-step-if-changed [app-or-comp props {:keys [steps step new-state info info-popup]}]
  (if-not (and (= new-state (get-in props [[::id steps] ::step-list step :state]))
               (= info (get-in props [[::id steps] ::step-list step :info])))
          (transact! app-or-comp [(update-state-of-step {:steps steps :step step :new-state new-state :info info :info-popup info-popup})])))

(defmutation post-mutation
  "To be used with `scom.fulcrologic.fulcro.data-fetch/load!` when it changes the state.
   The parameters `steps`, `step` and `ok-condition` should be given via `:post-mutation-params`"
  [{:keys [steps step ok-condition]}]
  (action [{:keys [app] :as env}] (transact! app [(update-state-of-step {:steps steps :step step :new-state (if (ok-condition @(:state env)) :done :failed)})])))


(defsc Steps
  [this {::keys [id step-list] :keys [style] :as props}]
  {:ident (fn [] [::steps id])
   :query (fn [] [::id ::step-list :com.fulcrologic.fulcro.application/active-remotes])}
  (dom/div {:classes ["ui" "steps" "unstackable"]
            :style style}
    (for [step step-list
          :let [state (:state step :queued)]]
      (dom/div {:key (:title step)
                :onClick #(transact! this [(update-state-of-step {:steps id :step (title->step-index (:title step) step-list) :new-state nil})])
                :classes ["step"
                          (case state
                                :active "active"
                                :done "completed"
                                nil)]}
        (dom/i {:classes (concat ["icon"]
                                 (case state
                                       :failed ["exclamation" "red"]
                                       :active ["spinner" "loading"]
                                       ["ellipsis" "horizontal"]))})
        (dom/div {:classes ["content"]}
          (dom/div {:classes ["title"]} (:title step))
          (dom/div {:classes ["description"]} (get (:contents step) state))
          (dom/div {:classes ["description"] :title (:info-popup step)} (:info step)))))

    (if-not (empty? (:com.fulcrologic.fulcro.application/active-remotes props))
                    (dom/div {:classes ["ui" "active" "dimmer"]}
                             (dom/div {:classes ["ui" "loader" "text"]} "Loading from Remote")))))

(def steps (factory Steps))
