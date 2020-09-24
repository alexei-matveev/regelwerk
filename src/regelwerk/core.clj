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

(comment
  ;;
  ;; Many possible syntaxes for rules. This is plain data:
  ;;
  {:vars '[?a ?b] :when '[[?a :is ?b]] :then '[[:a ?a :b ?b]]}
  ;;
  ;; With makros you may get rid of quoting, but likely have to decide
  ;; for syntax ...
  ;;
  ;; vars when then
  ;; vars then when
  ;; when then vars
  ;; when vars then
  ;; then vars when
  ;; then when vars
  ;;
  (defrule [?a ?b]
    [[?a :is ?b]] => [[:a ?a :b ?b]])
  (defrule named-rule [?a ?b]
    [[?a :is ?b]] => [[:a ?a :b ?b]])
  (forall [?a ?b] [[?a :is ?b]] => [[:a ?a :b ?b]])
  (for-each [?a ?b] :where [[?a :is ?b]] => [:a ?a :b ?b])
  (set-of [:a ?a :b ?b] :for-all [?a ?b] :such-that [[?a :is ?b]])
  (produce [:a ?a :b ?b] :from [?a ?b] :where [[?a :is ?b]]))

(defn- make-query [vars where]
  (vec (concat [:find] vars '[:in $] [:where] where)))

;; (make-query '[?a ?b] '[[?a :is ?b]])
;; =>
;; [:find ?a ?b :in $ :where [?a :is ?b]]

;; https://www.braveclojure.com/writing-macros/
(defmacro defrule
  "Generate new relation from exsiting facts and rules"
  [vars where]
  (let [q1 (make-query vars where)
        facts (gensym)]
    `(fn [~facts] (d/q (quote ~q1) ~facts))))

(comment

  (macroexpand '(defrule [?a ?b] [[?a :is ?b]]))
  =>
  (fn* ([G__XXX] (datascript.core/q (quote [:find ?a ?b :in $ :where [?a :is ?b]]) G__XXX)))

  (let [rule (defrule [?a ?b] [[?a :is ?b]])
        facts [[1 :is "odd"]
               [2 :is "even"]]]
    (rule facts))
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
