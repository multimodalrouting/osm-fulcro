(ns app.ui.leaflet.layers.d3svg-styled-lines
  (:require
    [com.fulcrologic.fulcro.components :refer [defsc]]
    [app.ui.leaflet.d3 :refer [d3SvgOverlay lngLat->Point color-by-accessibility feature->confident?]]))

(defn d3DrawCallback [sel proj data]
  (let [radius 3
        upd (-> sel
                (.selectAll "a")
                (.data (clj->js data)))
        a-el (-> (.enter upd)
                 (.append "a")
                 (.on "click" (fn [d i ds] (js/console.log (js->clj d)))))
        id-fn (fn [d] (str "path-" (hash (js->clj d :keywordize-keys true))))
        path-d-fn  (fn [d] (->> (get-in (js->clj d :keywordize-keys true) [:geometry :coordinates])
                                (map #(lngLat->Point proj %))
                                (map #(str (.-x %) "," (.-y %)))
                                (clojure.string/join " ")
                                (str "M")))
        stroke-width-fn (fn [d] (get-in (js->clj d :keywordize-keys true) [:properties :style :stroke-width] 1))]
       (-> a-el
           (.append "path")
             (.attr "id" id-fn)
           (.attr "d" path-d-fn )
           (.attr "stroke" #(color-by-accessibility (js->clj % :keywordize-keys true)))
           (.attr "stroke-width" stroke-width-fn)
           (.attr "fill" "none"))
       (-> a-el
           (.append "path")
           (.attr "opacity" #(if (feature->confident? (js->clj % :keywordize-keys true))
                                 0 1))  ;; TODO instead don't draw this at all
           (.attr "d" path-d-fn)
           (.attr "stroke-dasharray" 2)
           (.attr "stroke" "grey")
           (.attr "stroke-width" stroke-width-fn)
           (.attr "fill" "none"))
       #_(-> a-el
             (.append "text")
             (.append "textPath")
             (.attr "href" (fn [d] (str "#" (id-fn d))))
             (.attr "side" "right")
             (.text "a text along the line"))))

(defsc D3SvgStyledLines [this {:keys [react-key geojson]}]
  (d3SvgOverlay {:key          react-key
                 :data         (:features geojson)
                 :drawCallback d3DrawCallback}))
