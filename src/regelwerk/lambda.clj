(ns regelwerk.lambda
  (:require [datascript.core :as d]))

;; Example query over two fact databases: find a variable that maybe 0
;; in both databases. We dont need to declare parameters of a function
;; [$a $b]  and refer to  the databases as  [$a $b] inside  the quoted
;; query. But we can and do.
(defn- f [$a $b]
  (d/q '{:find [?v]
         :in [$a $b]
         :where [[$a ?v :eq 0]
                 [$b ?v :eq 0]]}
       $a
       $b))

(defn- binary []
  (let [a #{["x" :eq 0]
            ["x" :eq 9]
            ["y" :eq 0]}
        b #{["y" :eq 0]}]
    (f a b)))

(binary) ;; => #{["y"]}

