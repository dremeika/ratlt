(ns rat.core
  (:require [clojure.string :refer [split lower-case]]
            [jayq.core :refer [ajax]]
            [enfocus.core :as ef]
            [enfocus.events :as events])
  (:require-macros [enfocus.macros :as em]))

(def state (atom nil))

(defn- log [id status]
  (when (not @state)
    (reset! state status)
    (ajax "/log"
          {:type :post
           :data {:status status
                  :id id}})))

(defn- check-answer [_]
  (let [entered (ef/from "#association" (ef/get-prop :value))
        correct (ef/from "#correct" (ef/get-prop :value))
        words (into #{} (split correct #","))
        guessed (contains? words (lower-case entered))]
    (when guessed (log (ef/from "#id" (ef/get-prop :value)) "guessed"))
    (ef/at "#show-answer" (ef/set-style :display (if guessed "none" "initial")))
    (ef/at "#success" (ef/set-style :display (if guessed "initial" "none")))))

(defn- show-answer [_]
  (log (ef/from "#id" (ef/get-prop :value)) "gaveup")
  (ef/at "#answer-block" (ef/set-style :display "table"))
  (ef/at "#question-block" (ef/set-style :display "none")))

(em/defaction setup []
  ["#association"] (events/listen :keyup check-answer)
  ["#show-answer"] (events/listen :click show-answer))

(set! (.-onload js/window) setup)