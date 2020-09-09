(ns regelwerk.core
  (:require [datascript.core :as d]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

;; This is significantly slower than the compareable SQLite code. See
;; also
;; https://stackoverflow.com/questions/42457136/recursive-datalog-queries-for-datomic-really-slow
(defn- unused-example [n]
  (let [rules (quote
               [[(follows ?a ?b)
                 [?a ?b]]
                ;; Order of  clauses matters! Recursion in  1st or 2nd
                ;; positon does not:
                [(follows ?a ?b)
                 [?x ?b]
                 (follows ?a ?x)]])
        query (quote
               [:find ?a ?b
                :in $ %
                :where
                ;; [0 ?b] --- would be fast
                (follows ?a ?b)
                ;; [0 ?b] --- would be slow
                ])
        ;; Looks like ([0 1] [1 2] [2 3]) for n = 3:
        db (for [i (range n)]
               [i (inc i)])]
    (d/q query db rules)))

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
