(ns regelwerk.core
  (:require [datascript.core :as d]
            [clojure.java.io :as io]
            [clojure.string :as str]
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
;; Macros [1] might  be the second best choice to  define rules if you
;; are going  to define them in  the source code.  The  best choice is
;; supposedly "data", because "data" =  "code", you've heard the story
;; many times.  But, how usefull will  makros be when you need to read
;; rules at run time from a user-supplied file or URL?
;;
;; Hm, on  the second thought you  only need a macro  to turn symbolic
;; expressions  "expr"  int  a  fn/lambda of  arguments  "vars".   The
;; Darascript query function takes find-query as plain data.
;;
;; [1] https://www.braveclojure.com/writing-macros/
;;
(defmacro defrule [vars where expr]
  ;; This will be a funciton of a fact database:
  `(fn [facts#]
     ;; Compute the  result set by  querieng facts with  Datascript. A
     ;; Datascript  DB can  be as  simple as  a collection  of tuples,
     ;; mostly EAV-tuples.
     (let [rows# (d/q '[:find ~@vars :where ~@where] facts#)]
       ;; Generate another set of objects from the supplied collection
       ;; valued  expression binding  each row  of the  result set  to
       ;; variables  of a  vector.   Clojure indeed  allows binding  a
       ;; vector of values to vector of  symbols --- a special case of
       ;; "destructuring bind", so it is called, I think.
       (into #{} cat
             (for [row# rows#]
               (let [~vars row#] ~expr))))))

;; C-u C-x C-e if you want to see the expansion:
(comment
  (macroexpand '(defrule [?a ?b] [[?a :is ?b]] [[?b ?a]])))

(defn- test-1 []
  (let [rule (defrule [?a ?b]
               [[?a :is ?b]]
               ;; =>
               [[?b :x ?a]
                [?a :y (str/upper-case ?b)]])
        facts [[1 :is "odd"]
               [2 :is "even"]]]
    (= (rule facts)
       #{["odd" :x 1] ["even" :x 2] [1 :y "ODD"] [2 :y "EVEN"]})))

;; (test-1) => true

;;
;; It will rarely stay  by one rule. Do we need a  makro for that? The
;; simplest  extension is  to accept  a list  of 3-tuples  (vars where
;; expr).
;;
(comment
  (define-rules
    ([?a ?b]
     ([?a :eq ?b] => [?b :eq ?a])
     ([?a :le ?b] [?b :le ?a] => [?a :eq ?b]))
    ([?x ?y]
     ([?x :eq ?t] [?t :eq ?y] => [?x :eq ?y]))))

;; C-u C-x C-e if you want to see the expansion:
(comment
  (macroexpand '(defrules
                  ([?a ?b] [[?a :is ?b]] [[?b ?a]])
                  ([?x ?y] [[?y :is ?y]] [[?x ?y]]))))

(defmacro defrules [& arities]
  (let [fs (for [[vars where expr] arities]
             `(fn [facts#]
                (let [rows# (d/q '[:find ~@vars :where ~@where] facts#)]
                  (into #{} cat
                        (for [row# rows#]
                          (let [~vars row#] ~expr))))))]
    `(fn [facts#]
       (into #{} cat (for [f# [~@fs]]
                       (f# facts#))))))

(defn- test-2 []
  (let [rules (defrules
                ([?a ?b] [[?a :is ?b]] [[?b ?a]])
                ([?a ?b] [[?a :is ?b]] [[?a ?b]]))
        facts [[1 :is "odd"]
               [2 :is "even"]]]
    (= (rules facts)
       #{[1 "odd"] ["odd" 1] [2 "even"] ["even" 2]})))

;; (test-2) => true

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
