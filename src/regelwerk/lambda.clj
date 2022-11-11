;; It  is unclear  if  we  can extend  regelwerk.core  in a  backwards
;; compatible way. So we make experiments in a separate namespace:
(ns regelwerk.lambda
  (:require [datascript.core :as d]))

;; Example query over two fact databases: find a variable that maybe 0
;; in both databases. We dont need to declare parameters of a function
;; [$a $b]  and refer to  the databases as  [$a $b] inside  the quoted
;; query. But we can and do.
;;
;; The function "f" ist a funciton of two datasets, $a and $b. But the
;; declaration or definition of the  function involves a DS-Query with
;; what we callt rule-variable ?v.
(defn- f [$a $b]
  (d/q '{:find [?v]
         :in [$a $b]
         :where [[$a ?v :eq 0]
                 [$b ?v :eq 0]]}
       $a
       $b))

;; The  next evolution  of the  macro should  thus be  able to  take a
;; Datalog  query with  an IN-clause  and define  the function  of the
;; correspondig  number  of  datasets  like  above.   This  should  be
;; functionally equivalent with the "defn" of "f" above:
(comment
  (defrules {:in [$a $b]
             :find [?v]
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

