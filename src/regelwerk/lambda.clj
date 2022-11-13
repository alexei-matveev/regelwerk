(ns regelwerk.lambda
  (:require [datascript.core :as d]
            [regelwerk.core :as c]))

;; Motivation and thoughts ...
(comment
  ;; Example query over two fact databases: find a variable that maybe
  ;; 0 in  both databases.  We dont  need to  declare parameters  of a
  ;; function [$a $b] and refer to the databases as [$a $b] inside the
  ;; quoted query. But we can and do.
  ;;
  ;; The function "f"  ist a function of two datasets,  $a and $b. But
  ;; the declaration of the function  involves a DS-Query with what we
  ;; called a rule-variable ?v.
  (defn- f [$a $b]
    (d/q '{:find [?v]                   ; then return #{[?v]}
           :in [$a $b]
           :where [[$a ?v :eq 0]
                   [$b ?v :eq 0]]}
         $a
         $b))

  ;; The next  evolution of the  macro should thus  be able to  take a
  ;; Datalog query with  an IN/FROM-clause and define  the function of
  ;; the correspondig number  of datasets like above.   This should be
  ;; functionally equivalent with the "defn" of "f" above:
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
     :then [[?v]]})

  (defn- binary []
    (let [a #{["x" :eq 0]
              ["x" :eq 9]
              ["y" :eq 0]}
          b #{["y" :eq 0]}]
      (f a b)))

  ;; => #{["y"]}
  (binary))

;; Example usage for your C-u C-x C-e pleasure:
(comment
  (let [r1 (c/defrule {:from [$u]
                       :find [?a ?b]
                       :when [[$u ?a :is ?b]]
                       :then [[?b :eq ?a]]})
        r2 (c/defrule {:find [?a ?b]
                       :when [[?a :is ?b]]
                       :then [[?b :eq ?a]]})
        r3 (c/defrule {:from [$u $v]
                       :find [?x ?y]
                       :when [[$u ?x :is ?z]
                              [$v ?y :is ?z]]
                       :then [[?x :le ?y]
                              [?y :le ?x]]})
        u [["a" :is 1]
           ["b" :is 2]]
        v [["A" :is 1]
           ["B" :is 2]]]
    {:r1 (r1 u)
     :r2 (r2 v)
     :r3 (r3 u v)})
  =>
  {:r1 #{[1 :eq "a"] [2 :eq "b"]},
   :r2 #{[2 :eq "B"] [1 :eq "A"]},
   :r3 #{["B" :le "b"]
         ["A" :le "a"]
         ["b" :le "B"]
         ["a" :le "A"]}})
