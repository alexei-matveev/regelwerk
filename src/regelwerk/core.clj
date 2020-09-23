(ns regelwerk.core
  (:require [datascript.core :as d]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

;; This is significantly slower than the compareable SQLite code. See
;; also
;; https://stackoverflow.com/questions/42457136/recursive-datalog-queries-for-datomic-really-slow
(defn- unused-example [n]
  (let [rules '[[(follows ?a ?b)
                 [?a ?b]]
                [(follows ?a ?b)
                 [?x ?b]
                 (follows ?a ?x)]]
        query '[:find ?a ?b
                :in $ %
                :where
                (follows ?a ?b)]
        ;; Looks like ([0 1] [1 2] [2 3]) for n = 3:
        db (for [i (range n)]
               [i (inc i)])]
    (d/q query db rules)))

(comment
  (unused-example 3)
  =>
  #{[2 3] [1 3] [0 3] [0 2] [1 2] [0 1]})

;; https://www.braveclojure.com/writing-macros/
(defmacro genrel
  "Generate new relation from exsiting facts and rules"
  [vars where facts]
  (let [q (vec (concat [:find] vars '[:in $] [:where] where))
        qq (list 'quote q)]
    (list 'd/q qq facts)))

(comment
  ;;
  ;; Many possible syntaxes for rules ...
  ;;
  ;; vars when then
  ;; vars then when
  ;; when then vars
  ;; when vars then
  ;; then vars when
  ;; then when vars
  ;;
  (forall [?a ?b] [[?a :is ?b]] => [[:a ?a :b ?b]])
  (for-each [?a ?b] :where [[?a :is ?b]] => [:a ?a :b ?b])
  (set-of [:a ?a :b ?b] :for-all [?a ?b] :such-that [[?a :is ?b]])
  (produce [:a ?a :b ?b] :from [?a ?b] :where [[?a :is ?b]])
  (macroexpand
   '(genrel [?a ?b]
            [[?a :is ?b]]
            [[1 :is "odd"]
             [2 :is "even"]]))
  (genrel [?a ?b]
           [[?a :is ?b]]
           [[1 :is "odd"]
            [2 :is "even"]])
  =>
  #{[1 "odd"] [2 "even"]})

(defn- parse [path]
  (edn/read (java.io.PushbackReader. (io/reader path))))

(defn- main [db-file rules-file query-file]
  (let [db (parse db-file)
        rules (parse rules-file)
        query (parse query-file)]
    (d/q query db rules)))

(defn -main [& args]
  (println (apply main args)))

;; For your C-x C-e pleasure:
(comment
  (main "resources/db.edn"
        "resources/rules.edn"
        "resources/query.edn"))
