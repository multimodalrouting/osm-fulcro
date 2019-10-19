(ns app.ui.leaflet.layers.extern.base
  (:require
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    ["react-leaflet" :as ReactLeaflet :refer [withLeaflet LayersControl.BaseLayer TileLayer]]))

(def layersControlBaseLayer (react-factory LayersControl.BaseLayer))
(def tileLayer (react-factory TileLayer))

(def baseLayers 
  [(layersControlBaseLayer {:key "Esri Aearial" :name "Esri Aearial" :checked true}
     (tileLayer {:url "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}.png"
                 :attribution "&copy; <a href=\"http://esri.com\">Esri</a>, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community"}))
   (layersControlBaseLayer {:key "OSM Tiles" :name "OSM Tiles"}
     (tileLayer {:url "https://{s}.tile.osm.org/{z}/{x}/{y}.png"
                 :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}))
   (layersControlBaseLayer {:key "PublicTransport (MeMOMaps)" :name "PublicTransport (MeMOMaps)"}
     (tileLayer {:url "https://tileserver.memomaps.de/tilegen/{z}/{x}/{y}.png"
                 :attribution "<a href=\"https://memomaps.de\">MeMOMaps"}))
   (layersControlBaseLayer {:key "PublicTransport (openptmap)" :name "PublicTransport (openptmap)"}
     (tileLayer {:url "http://openptmap.org/tiles/{z}/{x}/{y}.png"
                 :attribution "<a href=\"https://wiki.openstreetmap.org/wiki/Openptmap\">Openptmap"}))
   (layersControlBaseLayer {:key "NONE (only overlays)" :name "NONE (only overlays)"}
     (tileLayer {:url ""}))])
