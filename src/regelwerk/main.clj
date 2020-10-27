;;
;; Try using core from outside.
;;
(ns regelwerk.main
  (:require [regelwerk.core :as rwk])
  (:gen-class))

;; (test-1a) => true
(defn- test-1a []
  (let [f (fn [a b]
            [[b :x a]
             [a :y (clojure.string/upper-case b)]])
        rule (rwk/defrule [?a ?b]
               (f ?a ?b)
               ;; <-
               [[?a :is ?b]])
        facts [[1 :is "odd"]
               [2 :is "even"]]]
    (= (rule facts)
       #{["odd" :x 1] ["even" :x 2] [1 :y "ODD"] [2 :y "EVEN"]})))

;; (test-2) => true
(defn- test-2 []
  (let [rules (rwk/defrules
                ([?a ?b] [[?b ?a]] [[?a :is ?b]])
                ([?a ?b] [[?a ?b]] [[?a :is ?b]]))
        facts [[1 :is "odd"]
               [2 :is "even"]]]
    (= (rules facts)
       #{[1 "odd"] ["odd" 1] [2 "even"] ["even" 2]})))

;; (test-3) => true
(defn- test-3 []
  (let [rules (rwk/load-rules (clojure.java.io/resource "rules.edn"))
        facts [[0 :is :int]
               [100 :is :int]]]
    (= (rules facts)
       #{[-1 :is :int] [0 :is :int] [1 :is :int]
         [99 :is :int] [100 :is :int] [101 :is :int]})))

(defn test-all []
  #_(println (test-1))
  (println (test-1a))
  (println (test-2))
  (println (test-3)))

(defn- main [facts-path rules-path]
  (test-all)
  (rwk/test-all)
  ;; Here slurp-edn would also work  for facts, except it would return
  ;; a list, not a set:
  (let [facts (rwk/read-facts facts-path)
        rules (rwk/load-rules rules-path)]
    ;;
    ;; Very functional way to iterate lazily:
    ;;
    ;; (iterate f x) = (x (f x) (f (f x)) ...)
    ;;
    (doseq [facts (take 3 (iterate rules facts))]
      (println facts))))

(defn -main [& args]
  ;; Because I am too lazy to type it every time.
  (main "resources/facts.edn" "resources/rules.edn"))
