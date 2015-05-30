(ns rat.core
  (:gen-class)
  (:require [clojure.string :as s]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [resources not-found]]
            [environ.core :refer [env]]
            [net.cgrand.enlive-html :as eh]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [redirect]])
  (:import [org.apache.lucene.analysis.miscellaneous ASCIIFoldingFilter]))

(def add-analytics (Boolean/parseBoolean (get env :rat-analytics "false")))

(defn- fold-to-ascii
  "Folds non-ASCII characters to ASCII."
  [term]
  (let [length (count term)
        output (char-array length)]
    (ASCIIFoldingFilter/foldToASCII (char-array term) 0 output 0 length)
    (apply str output)))

(defn- load-triples
  "Loads association triples with answers from file."
  []
  (let [text (slurp (:rat-associations env))]
    (reduce
      (fn [res line]
        (let [[words answers] (s/split line #"\>")
              associations (s/split words  #"[,\s]")
              answers (s/split answers #"[,\s]")
              lc-answers (map s/lower-case answers)
              folded-answers (map fold-to-ascii lc-answers)]
          (assoc res (s/join "-" (map #(-> % s/lower-case fold-to-ascii) associations))
                     {:assocs associations
                      :answers answers
                      :check (into #{} (concat lc-answers folded-answers))})))
      {} (s/split-lines text))))


(def triples (load-triples))
(def triple-ids (sort (keys triples)))

(defn- random-assoc-id []
  (nth triple-ids (rand-int (count triple-ids))))

(defn- prev-assoc-id [id]
  (let [index (.indexOf triple-ids id)]
    (if (> index 0)
      (nth triple-ids (dec index))
      (last triple-ids))))

(defn- next-assoc-id [id]
  (let [index (.indexOf triple-ids id)]
    (if (= (inc index) (count triple-ids))
      (first triple-ids)
      (nth triple-ids (inc index)))))

(eh/deftemplate rat-template "templates/rat.html"
  [id {[w1 w2 w3] :assocs answers :answers check :check}]
  [:script#analytics] (when add-analytics identity)
  [:#answer :strong] (eh/content (first answers))
  [:#correct] (eh/set-attr :value (s/join "," check))
  [:#id] (eh/set-attr :value id)
  [:h1] (eh/transform-content (eh/replace-vars {:w1 w1 :w2 w2 :w3 w3}))
  [:li.previous :a] (eh/set-attr :href (prev-assoc-id id))
  [:li.random :a] (eh/set-attr :href (random-assoc-id))
  [:li.next :a] (eh/set-attr :href (next-assoc-id id)))

(defn- rat-page [id]
  (when (contains? triples id)
    (apply str (rat-template id (get triples id)))))

(defn- log [{:strs [id status]}]
  (log/debugf "ID: '%s' STATUS: '%s'" id status))

(defroutes app
  (resources "/assets")
  (wrap-params
    (POST "/log" {params :params} (do (log params) {:status 201})))
  (GET "/:id" [id] (rat-page id))
  (GET "/*" []
    (redirect (str "/" (first triple-ids)))))

(defn run [host port]
  (run-jetty #'app {:port port :host host :join? false}))

(defn -main [& other]
  (run (:rat-host env) (read-string (:rat-port env))))
