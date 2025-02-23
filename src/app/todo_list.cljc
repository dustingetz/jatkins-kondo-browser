(ns app.todo-list
  (:require contrib.str
            #?(:clj [datascript.core :as d]) ; database on server
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]
            [contrib.electric-codemirror :as ecm]
            [app.source-parser :as parser]))

(def !client-db #?(:cljs (atom {})
                   :clj nil))
(e/def client-db)

(e/defn SearchBox
  []
  (ui/input (:search-box client-db) 
    (e/fn [v] (swap! !client-db assoc :search-box (contrib.str/empty->nil v)))
    (dom/props {:placeholder "Search Files"})))

(e/defn VarList
  []
  (let [{:keys [search-box]} client-db
        vars (e/server (parser/search-vars search-box))]
    (dom/div (dom/props {:class "master-view"})
      (dom/h3
        (dom/text "Kondo located vars"))
      (e/for-by identity [var vars]
        (dom/div
          (dom/on "click"
            (e/fn [e]
              (swap! !client-db
                #(-> %
                   (update :open-vars (fnil conj #{}) var)
                   #_(assoc :search-box "")))))
          (dom/text var))))))

(e/defn VarViewer
  []
  (let [{:keys [open-vars]} client-db]
    (dom/div (dom/props {:class "detail-view"})
      (dom/h3 (dom/text "Open files"))
      (doseq [var open-vars]
        (dom/div
          (dom/p (dom/text var " ")
            (ui/button (e/fn [] (swap! !client-db update :open-vars disj var)) 
              (dom/span (dom/text "close"))))
          (let [text (e/server (-> parser/vars-by-name
                                 (get var)
                                 (parser/get-definition)))]
            (new ecm/CodeMirror
              {:parent dom/node}
              identity identity text)))))))

(e/defn App
  []
  (e/client
    (binding [client-db (e/watch !client-db)]
      (dom/link (dom/props {:rel :stylesheet :href "/todo-list.css"}))
      (SearchBox.)
      (dom/div (dom/props {:class "master-detail-view"})
        (VarList.)
        (VarViewer.)))))