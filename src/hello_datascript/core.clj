(ns hello-datascript.core
  (:require [datascript.core :as d]))

;; This is significantly slower than SQLite code below:. See also
;; https://stackoverflow.com/questions/42457136/recursive-datalog-queries-for-datomic-really-slow
(defn- bench-O2 [n]
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

(defn -main []
  (println "Hello, Datascript!")
  (time (println (count (bench-O2 200)))))
