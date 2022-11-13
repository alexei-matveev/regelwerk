;; It  is unclear  if  we  can extend  regelwerk.core  in a  backwards
;; compatible way. So we make experiments in a separate namespace:
(ns regelwerk.lambda
  (:require [datascript.core :as d]))

;; Example query over two fact databases: find a variable that maybe 0
;; in both databases. We dont need to declare parameters of a function
;; [$a $b]  and refer to  the databases as  [$a $b] inside  the quoted
;; query. But we can and do.
;;
;; The function "f" ist a function of two datasets, $a and $b. But the
;; declaration of the function involves a DS-Query with what we called
;; a rule-variable ?v.
(defn- f [$a $b]
  (d/q '{:find [?v]                     ; then return #{[?v]}
         :in [$a $b]
         :where [[$a ?v :eq 0]
                 [$b ?v :eq 0]]}
       $a
       $b))

;; The  next evolution  of the  macro should  thus be  able to  take a
;; Datalog query with an IN/FROM-clause and define the function of the
;; correspondig  number  of  datasets  like  above.   This  should  be
;; functionally equivalent with the "defn" of "f" above:
(comment
  (defrule {:in [$a $b]                ; defines the arity of the rule
            :find [?v]                 ; rows of the query result set
            :when [[$a ?v :eq 0]
                   [$b ?v :eq 0]]
            :then [[?v]]})              ; rows of rule-output

  ;; With SQL/LINQ flavored IN/FROM-Clause:
  (defrule {:from [$a $b]
            :find [?v]
            :when [[$a ?v :eq 0]
                   [$b ?v :eq 0]]
            :then [[?v]]})

  ;; Implicit IN-Clause,  but the  Map is  not quite  a self-contained
  ;; Datalog Query. This is less suitable  for "rules as data". On the
  ;; other  hand if  you  really  go all  the  way  defining Rules  of
  ;; different arity taking  different datasets, the case  of a single
  ;; file with single purpose rule set will likely be the rare special
  ;; case.
  (defrule [$a $b]
    {:find [?v]
     :when [[$a ?v :eq 0]
            [$b ?v :eq 0]]
     :then [[?v]]}))


(defn- binary []
  (let [a #{["x" :eq 0]
            ["x" :eq 9]
            ["y" :eq 0]}
        b #{["y" :eq 0]}]
    (f a b)))

(binary) ;; => #{["y"]}

;; This will  return the  *code* for  the rule  as function  of facts,
;; hence tha name:
(defn- compile-rule [rule]
  (let [from (:from rule)               ; maybe nil
        vars (:find rule)
        expr (:then rule)
        when (:when rule)]
    ;; This will be a function one or more datasets sometime:
    `(fn [facts#]
       ;; Datascript appears to handle the case of nil for the
       ;; IN-clause just OK:
       (let [rows# (d/q '{:find ~vars, :in ~from, :where ~when} facts#)]
         ;; Generate another set of objects from the supplied collection
         ;; valued  expression binding  each row  of the  result set  to
         ;; variables  of a  vector.   Clojure indeed  allows binding  a
         ;; vector of values to vector of  symbols --- a special case of
         ;; "destructuring bind", so it is called, I think.
         (into #{} cat     ; transducer works as (reduce into #{} ...)
               (for [row# rows#]
                 (let [~vars row#] ~expr)))))))

(defmacro defrule [map]
  (compile-rule map))

(let [r1 (defrule {:from [$t]
                   :find [?a ?b]
                   :when [[$t ?a :is ?b]]
                   :then [[?b :eq ?a]]})
      r2 (defrule {:find [?a ?b]
                   :when [[?a :is ?b]]
                   :then [[?b :eq ?a]]})
      t [["a" :is 1]
         ["b" :is 2]]]
  (r2 t))
