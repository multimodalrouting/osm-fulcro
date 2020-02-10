(ns app.ui.steps-helper)

(defn index-of
  "return the index of the first element matching pred"
  [pred coll]
  (->> coll
       (keep-indexed (fn [i x] {i x}))  (into {})
       (some #(if (pred (val %))
                  (key %)))))

(defn title->step-index [title steps]
  (index-of #(= (:title %) title) steps))

(defn title->step [title steps]
  (get steps (title->step-index title steps)))
