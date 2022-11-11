(ns regelwerk.lambda
  (:require [datascript.core :as d]))

;; Example query over two fact databases:
(defn- binary []
  (let [a [["x" :eq 0]
           ["x" :eq 9]
           ["y" :eq 0]]
        b [["y" :eq 0]]]
    ;; Find a variable that maybe 0 in both databases:
    (d/q '{:find [?v]
           :in [$a $b]
           :where [[$a ?v :eq 0]
                   [$b ?v :eq 0]]}
         a
         b)))

(binary) ;; => #{["y"]}

