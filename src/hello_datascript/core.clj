(ns hello-datascript.core
  (:require [datascript.core :as d]
            [criterium.core :as c]))

(defn- test-3 []
  ;; Here the database is a rectangular table, a "relation",
  ;; represented by a collection of rows. You may assume the
  ;; collection to be first converted to a set, thus de-duped:
  (let [db [[:red 10]
            [:red 10]                   ; dupe!
            [:red 20]
            [:red 30]
            [:red 40]
            [:red 50]
            [:blue 30]                  ; note the same numbers
            [:blue 40]]]
    ;; Note that the dupes have no effect. The *with* clause is
    ;; necessary if you want count values of blue rows that happen to
    ;; coincide with some values in red rows. If you consider only
    ;; numbers *without* the color there will be only 5 of them:
    (d/q '[:find (count ?x)
           :with ?color
           :in [[?color ?x]]]
         db)
    ;; There are only a few built in aggregate functions that return
    ;; collections. Distinct is one of them, but naturally it would
    ;; remove duplicates:
    (d/q '[:find ?color (distinct ?x)
           :in [[?color ?x]]]
         db)
    ;; (max ?n ?x) is another aggregate that returns n largest values
    ;; of x, not the larger of the two as one might have thought:
    #_(d/q '[:find ?color (max ?amount ?x) (min ?amount ?x)
           :in [[?color ?x]] ?amount]
         db
         3)))

(defn- test-2 []
  (d/q '[:find  ?n ?aka
         :in [[?n ?a ?aka]]
         :where
         [(= ?aka "Maks Otto von Stirlitz")]
         ;; [?n ?a "Maks Otto von Stirlitz"]
         ;; [?n 45 ?aka]
         ;; [?n _ ?aka]
         ]
       [["Alex" 16 "Jungster"]
        ["Maksim" 45 "Maks Otto von Stirlitz"]
        ["Maksim" :literal-45 "Maks Otto von Stirlitz"]]))

(defn- test-0 []
  (d/q '[:find (sum ?heads) .
         :with ?monster
         :in [[?monster _ ?heads]]
         :where
         [_ :ok ?heads]]
       ;; inputs
       [["Cerberus" :ok 3]
        ["Medusa" :ok 1]
        ["Cyclops" :ok 1]
        ["Chimera" :ok 1]]))

(defn- test-1 []
  (let [schema {:aka {:db/cardinality :db.cardinality/many}}
        conn   (d/create-conn schema)]
    (d/transact! conn [ { :db/id -1
                         :name  "Maksim"
                         :age   45
                         :aka   ["Maks Otto von Stirlitz", "Jack Ryan"]}])
    (d/q '[:find  ?n ?a
           :where
           [?e :aka "Maks Otto von Stirlitz"]
           [?e :name ?n]
           [?e :age  ?a]]
         @conn)))

;;
;; Here is an example of using longer tuples inspired by "day of
;; datomic" tutorial [1]. Dont try to deconstruct the source table by
;; using
;;
;;     :in [[?monster ?a ?heads]] ; WRONG!
;;
;; You'll get a cryptic error. Instead give the source a $-name and
;; bind the fields in a where-clause.
;;
;; [1] https://github.com/Datomic/day-of-datomic/blob/master/
;;     tutorial/datalog_on_defrecords.clj
;;
(d/q '[:find ?monster ?a ?heads
       :in $tuples
       :where
       [$tuples ?monster ?a ?heads]]
     ;; inputs
     [["Cerberus" :a 3]
      ["Medusa" :a 1]
      ["Cyclops" :b 1]
      ["Chimera" :b 1]])
;; => #{["Medusa" :a 1]
;;      ["Chimera" :b 1]
;;      ["Cyclops" :b 1]
;;      ["Cerberus" :a 3]}


;; Your rows can be of any length and you can match on any constant:
(d/q '[:find ?n ?aka
       :in $table
       :where
       [$table ?n 3.14 ?aka _ 2 _]]
     [["Alex" 16 "Jungster" 1 2 3]
      ["Maksim" :von "Stirlitz" 4 5 6]
      ["Maksim" 3.14 "Maks Otto von Stirlitz" 3 2 1]])
;; => #{["Maksim" "Maks Otto von Stirlitz"]}

;; Recursive rule example. This is how you specify the DB for the
;; rule. As usual if you dont give DB a name using the default $ to
;; prefix the rule is not necessary. Rules do not reference data
(let [r (quote [[(follows ?e1 ?e2)
                 [?e1 ?e2]]
                [(follows ?e1 ?e2)
                 [?e1 ?t]
                 (follows ?t ?e2)]])
      ;; query:
      q (quote [:find ?u1 ?n2
                :in $a $b %
                :where
                ($a follows ?u1 ?u2)
                [$b ?u2 ?n2]])
      ;; data:
      a [[1 2]
         [2 3]
         [3 4]]
      b [[1 "one"]
         [2 "two"]
         [3 "three"]
         [4 "four"]]]
  (d/q q a b r))
;; #{[1 "four"] [3 "four"] [1 "three"] [2 "four"] [1 "two"] [2 "three"]}

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

(comment
  ;; Fine for small n = 3:
  (bench-O2 3)
  =>
  #{[2 3] [1 3] [0 3] [0 2] [1 2] [0 1]}

  ;; Takes >6s to compute n * (n + 1) / 2 elements for n = 200:
  (time (count (bench-O2 200)))
  =>
  "Elapsed time: 6256.472844 msecs"
  20100

  ;; This looks even steeper than O(n^2):
  ;; n =   3 =>    2.459736 ms
  ;; n =   6 =>    4.883212 ms
  ;; n =  12 =>   12.414191 ms
  ;; n =  24 =>   38.799325 ms
  ;; n =  48 =>  160.912901 ms
  ;; n =  96 =>  835.780947 ms
  ;; n = 192 => 5.352475 s
  (c/quick-bench (bench-O2 192)))

(defn- bench-O2-v2 [n]
  (let [rules (quote
               [[(follows ?a ?b)
                 [?a :lt ?b]]
                [(follows ?a ?b)
                 [?a :lt ?x]
                 (follows ?x ?b)]])
        query (quote
               [:find ?a ?b
                :in $ %
                :where
                (follows ?a ?b)])
        ;; Data. For some reason 0 ist no more a valid entity ID, start
        ;; counting at 1:
        tx-data (for [i (range 1 (inc n))]
                  [:db/add i :lt (inc i)])
        ;; real db with datoms:
        db (d/db-with (d/empty-db) tx-data)]
    (d/q query db rules)))

;; Not any better:
(comment
  (time (count (bench-O2-v2 200)))
  =>
  "Elapsed time: 5946.798873 msecs"
  20100)

;; drop table if exists db;
;; create table db (x integer, y integer);

;; insert into db
;; with recursive
;;      cnt (x, y) as
;;      (values(0, 1)
;;         union all
;;       select
;;         x + 1,
;;         x + 2
;;       from cnt
;;       where x + 1 < 200)
;; select x, y from cnt;

;; select count(*) from (
;; with recursive
;;      follows (a, b) as
;;      (select x as a, y as b from db
;;      union all
;;      select f.a as a, d.y as b
;;      from follows as f
;;      join db as d
;;      on f.b = d.x)
;; select a, b from follows
;; )
;; ;

;; sqlite> .read recursive.sql
;; Run Time: real 0.000 user 0.000000 sys 0.000000
;; Run Time: real 0.000 user 0.000000 sys 0.000000
;; Run Time: real 0.001 user 0.004000 sys 0.000000
;; count (*)
;; 20100
;; Run Time: real 0.047 user 0.044000 sys 0.000000

(defn- make-eav-table [n]
  (vec
   (for [x (range n)]
     [x :a (+ 1000 (rand-int n))])))

(defn- test-tabular [n]
  (let [db (make-eav-table n)]
    #_(println db)
    (d/q (quote
          [:find (count ?v) .           ; point ist for scalar
           :in $db
           :where
           [$db ?e1 :a ?v]
           [$db ?e2 :a ?v]
           ;; You cannot put it between two clauses:
           [(> ?e2 ?e1)]])
         db)))

(comment
  ;; It  takes  ~15s  to  find  doppelgaenger in  a  array  of  random
  ;; numbers. The  result is also  random but should  be statistically
  ;; slightly above a 1/4th of that million:
  (time (test-tabular (* 1000 1000)))
  =>
  "Elapsed time: 14683.982279 msecs"
  263927

  ;; Those 15s  is *much* less  that it  takes to iterate  over 10**12
  ;; loop. Here it took 6s to  iterate over 10**10, so the estimate is
  ;; it would take 600s for 10**12:
  (time
   (let [n (* 1000 100)]
     (dotimes [x (* n n)]
       (+ 1 2)))))

(defn- test-two-tables [n]
  (let [a (make-eav-table n)
        b (make-eav-table n)]
    (d/q (quote
          [:find (count ?v) .           ; point ist for scalar
           :in $a $b
           :where
           [$a ?e1 :a ?v]
           [$b ?e2 :a ?v]])
         a
         b)))

(comment
  ;; Cross checking without third clause takes less time.
  (time (test-two-tables (* 1000 1000)))
  =>
  "Elapsed time: 9715.228009 msecs"
  399358)

(defn -main []
  (println "Hello, Datascript!")
  (time (println (count (bench-O2 200)))))
