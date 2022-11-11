(ns regelwerk.lambda
  (:require [datascript.core :as d]))

;; Example query over two fact databases:
(defn- binary []
  (let [d1 [["a" :eq 0]
            ["a" :eq 9]
            ["b" :eq 0]]
        d2 [["b" :eq 0]]]
    ;; Find a variable that maybe 0 in both databases:
    (d/q '{:find [?x]
           :in [$d1 $d2]
           :where [[$d1 ?x :eq 0]
                   [$d2 ?x :eq 0]]}
         d1
         d2)))

;; (binary) => #{["b"]}

