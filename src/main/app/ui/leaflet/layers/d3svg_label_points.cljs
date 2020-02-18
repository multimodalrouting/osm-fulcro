(ns app.ui.leaflet.layers.d3svg-label-points
  (:require
    [com.fulcrologic.fulcro.components :refer [defsc]]
    [app.ui.leaflet.d3 :refer [d3SvgOverlay lngLat->Point color-by-accessibility accessibility-patterns]]))


(defn feature->confident? [feature]
  (if-let [confidence (get-in feature [:properties :confidence])]
     (>= confidence 0.5)))

(defn d3DrawCallback [sel proj data]
  (let [radius 3
        x-fn (fn [d] (.-x (lngLat->Point proj (get-in (js->clj d :keywordize-keys true) [:geometry :coordinates]))))
        y-fn (fn [d] (.-y (lngLat->Point proj (get-in (js->clj d :keywordize-keys true) [:geometry :coordinates]))))
        svg (-> sel
              (.select (fn [] (this-as self
                                #_(js/console.log self)
                                       self.parentNode)))

                    )
        pattern (dorun (accessibility-patterns svg 2))
        upd (-> sel
                (.selectAll "a")
                (.data (clj->js data)))
        a-el (->
               (.enter upd)
               (.append "a")
               (.on "click" (fn [d i ds] (js/console.log (js->clj d))))
               )
        circle-el (-> a-el
                      (.append "circle")
                      (.attr "cx" x-fn)
                      (.attr "cy" y-fn)
                      (.attr "r" 4)
                      (.attr "fill" #(let [feature (js->clj % :keywordize-keys true)]
                                          (if-not (feature->confident? feature)
                                            (str "url(#" (get-in feature [:properties :wheelchair]) ")")
                                            (color-by-accessibility feature))
                                          ) )
                      (.attr "fill-opacity" 0.9)
                    )
        text-el (-> a-el
                    (.append "text")
                    (.text #(get-in (js->clj % :keywordize-keys true) [:properties :symbol-text] ))
                    (.style "font", "bold 8px sans-serif")
                    (.attr "x" (fn [d] (- (x-fn d) radius)))
                    (.attr "y" (fn [d] (+ (y-fn d) radius)))
                    )

        ]
     a-el
    ))

(defsc D3SvgLabelPoints [this {:keys [react-key geojson]}]
  (d3SvgOverlay {:key react-key
                 :data (:features geojson)
                 :drawCallback d3DrawCallback}))
