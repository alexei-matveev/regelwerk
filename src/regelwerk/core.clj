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

;;
;; Macros [1] might  be the second best choice to  define rules in you
;; are going  to define them in  the source code.  The  best choice is
;; supposedly "data", because "data" = "code", you've heard the strory
;; many times.  How  usefull can a makro  be if you need  to read your
;; rules at run time from a user-supplied file or URL?
;;
;; [1] https://www.braveclojure.com/writing-macros/
;;
(defmacro defrule [vars where expr]
  ;; This will be a funciton of the fact database:
  `(fn [facts#]
     ;; Compute the result set by querieng the facts:
     (let [rows# (d/q '[:find ~@vars :where ~@where] facts#)]
       ;; Generate another set of objects from the supplied expression
       ;; binding each row of the result set to the variables:
       (into #{} (for [row# rows#]
                   (let [~vars row#] ~expr))))))

(comment

  (macroexpand '(defrule [?a ?b] [[?a :is ?b]] [?b ?a]))
  =>
  ...

  (let [rule (defrule [?a ?b] [[?a :is ?b]] {:x ?a :y "is" :z ?b})
        facts [[1 :is "odd"]
               [2 :is "even"]]]
    (rule facts))
  =>
  #{{:x 1, :y "is", :z "odd"} {:x 2, :y "is", :z "even"}})

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
