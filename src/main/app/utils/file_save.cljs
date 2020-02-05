(ns app.utils.file-save
  (:require
    ["file-saver" :refer [saveAs]]
    ))

(defn store-file [filename data mimeType successFn errorFn]
  (let [blob (js/Blob. (clj->js [data]) (clj->js {:type mimeType}))]
    (if (and (.hasOwnProperty js/window "cordova")
             (not (= js/window.cordova.platformId "browser")))
      (let [
            filePaths (js->clj js/window.cordova.file :keywordize-keys true)
            storageLocation (if (= js/window.cordova.platformId "android")
                              (:externalDataDirectory filePaths)
                              (:documentsDirectory filePaths))
            ]
        (js/window.resolveLocalFileSystemURL
          storageLocation
          (fn [dir]
            (.getFile
              dir
              filename
              (clj->js {:create true})
              (fn [file]
                (.createWriter
                  file
                  (fn [fileWriter]
                    (set! fileWriter.onwriteend
                          #(successFn  file))
                    (set! fileWriter.onerror
                          (fn [err] (errorFn err)))
                    (.write fileWriter blob)
                    )))))))
      (saveAs blob filename))))


(defn open-with [fileUrl mimeType]
  (when (.hasOwnProperty js/window.cordova.plugins "fileOpener2")
    (do
      (.open
        js/window.cordova.plugins.fileOpener2
        fileUrl
        mimeType
        (clj->js {
                  :error   (fn [err] (prn "error opening!!! " err))
                  :success #(prn "successfully opened")
                  })))))

(comment
  (store-file "test4.gpx"
              "<xml><trk></trk></xml>"
              "application/gpx+xml"
              (fn [file]
                (open-with file.nativeURL "application/gpx+xml"))
              (fn [err] (prn "error writing!!! " err))
              )
  )
