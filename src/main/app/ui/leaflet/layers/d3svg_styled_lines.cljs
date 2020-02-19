(ns app.ui.leaflet.layers.d3svg-styled-lines
  (:require
    [com.fulcrologic.fulcro.components :refer [defsc]]
    [app.ui.leaflet.d3 :refer [d3SvgOverlay lngLat->Point color-by-accessibility]]))

(defn d3DrawCallback [sel proj data]
  (let [radius 3
        upd (-> sel
                (.selectAll "a")
                (.data (clj->js data)))
        a-el (-> (.enter upd)
                 (.append "a")
                 (.on "click" (fn [d i ds] (js/console.log (js->clj d))))
                 )
        id-fn (fn [d] (str "path-" (hash (js->clj d :keywordize-keys true))))
        path-d-fn  (fn [d] (->> (get-in (js->clj d :keywordize-keys true) [:geometry :coordinates])
                                (map #(lngLat->Point proj %))
                                (map #(str (.-x %) "," (.-y %)))
                                (clojure.string/join " ")
                                (str "M")
                                ))
        stroke-width-fn (fn [d] (get-in (js->clj d :keywordize-keys true) [:properties :style :stroke-width] 1))

        _ (-> a-el
            (.append "path")
              (.attr "id" id-fn)
            (.attr "d" path-d-fn )
            (.attr "stroke" (fn [d] "#C00000"
                              #_#(color-by-accessibility (js->clj % :keywordize-keys true))))
            (.attr "stroke-width" stroke-width-fn)
            (.attr "fill-opacity" 0.5)
            (.attr "fill" "none")

            )

        _ (-> a-el
            (.append "path")
            (.attr "d" path-d-fn)
            (.attr "stroke-dasharray" "2")
            (.attr "stroke" (fn [d] "#FF0000"
                              #_#(color-by-accessibility (js->clj % :keywordize-keys true))))
            (.attr "stroke-width" stroke-width-fn)
            (.attr "fill-opacity" 0.5)
            (.attr "fill" "none"))
        #_(-> a-el
              (.append "text")
              (.append "textPath")
              (.attr "href" (fn [d] (str "#" (id-fn d))))
              (.attr "side" "right")
              (.text "a text along the line"))
        ]
    a-el
    ))

(defsc D3SvgStyledLines [this {:keys [react-key geojson]}]
  (d3SvgOverlay {:key          react-key
                 :data         (:features geojson)
                 :drawCallback d3DrawCallback}))
