(ns regelwerk.core-test
  (:require [clojure.test :refer :all]
            [regelwerk.core :refer :all]))

(deftest demo-0
  (testing "Demo for the README ..."
    (let [facts [[1 :is "odd"]
                 [2 :is "even"]]
          rules (defrules
                  {:find [?number]
                   ;; But only even numbers from the set of English
                   ;; facts by Datalog query:
                   :when [[?number :is "even"]]
                   ;; For each number produce this set of facts in
                   ;; German:
                   :then [[?number :ist "gerade"]
                          [(inc ?number) :ist "ungerade"]
                          [(dec ?number) :ist "ungerade"]]})]
      (is (= (rules facts)
             #{[1 :ist "ungerade"]
               [2 :ist "gerade"]
               [3 :ist "ungerade"]})))))

(deftest binary-rule
  (testing "Binary rule with two fact databases as input  ..."
    (let [en [[1 :is "odd"]
              [2 :is "even"]]
          de [[1 :ist "ungerade"]
              [2 :ist "gerade"]]
          tr (defrules {:from [$en $de]
                        :find [?word ?wort]
                        :when [[$en ?n :is ?word]
                               [$de ?n :ist ?wort]]
                        :then [[?word :eqv ?wort]
                               [?wort :eqv ?word]]})]
      (is (= (tr en de)
             #{["odd" :eqv "ungerade"]
               ["gerade" :eqv "even"]
               ["ungerade" :eqv "odd"]
               ["even" :eqv "gerade"]})))))

(deftest test-1
  (testing "Function calls in expressions ..."
    (let [rule (defrules
                 {:find [?a ?b]
                  :then [[?b :x ?a]
                         [?a :y (clojure.string/upper-case ?b)]]
                  :when [[?a :is ?b]]})
          facts [[1 :is "odd"]
                 [2 :is "even"]]]
      (is (= (rule facts)
             #{["odd" :x 1] ["even" :x 2] [1 :y "ODD"] [2 :y "EVEN"]})))))

(deftest test-1a
  (testing "Function producing facts in expressions ..."
    (let [f (fn [a b]
              [[b :x a]
               [a :y (clojure.string/upper-case b)]])
          rule (defrules
                 {:find [?a ?b]
                  :then (f ?a ?b)
                  :when [[?a :is ?b]]})
          facts [[1 :is "odd"]
                 [2 :is "even"]]]
      (is (= (rule facts)
             #{["odd" :x 1] ["even" :x 2] [1 :y "ODD"] [2 :y "EVEN"]})))))


(deftest test-2
  (testing "Multiple rules in a single macro call ..."
    (let [rules (defrules
                  {:find [?a ?b], :then [[?b ?a]], :when [[?a :is ?b]]}
                  {:find [?a ?b], :then [[?a ?b]], :when [[?a :is ?b]]})
          facts [[1 :is "odd"]
                 [2 :is "even"]]]
      (is (= (rules facts)
             #{[1 "odd"] ["odd" 1] [2 "even"] ["even" 2]})))))

(deftest test-3
  (testing "Facts and queries for longer rows beyond EAV triples ..."
    (let [rules (defrules
                  {:find [?a ?b]
                   :then [[?b ?a]]
                   :when [[?a :is :like ?b]]})
          facts [[1 :is :like "one" :score 42 "many" "more" "attrs"]
                 [2 :is :like "two" :score 99]]]
      (is (= (rules facts)
             #{["two" 2] ["one" 1]})))))

;; Do you still hope to be able to extract free logic variables out of
;; an arbitrary expression?
(deftest test-4
  (testing "Logic variables and bindings in a single expression ..."
    (let [rules (defrules
                  {:find [?a]
                   :then [[?a :vs (let [?a (str "p" ?a)] ?a)]]
                   :when [[?a]]})
          facts [[1] [2]]]
      (is (= (rules facts)
             #{[2 :vs "p2"] [1 :vs "p1"]})))))

;; DPRECATED: Because you  should not be able to  deduce anything from
;; an empty set  of assumptions. If you just want  some facts use fact
;; database. If you  want them to come from a  rule function pass them
;; to the build in identity function, if it must be.
(deftest test-5
  (testing "Rule without logic variables delivers facts unconditionally (DEPRECATED) ..."
    (let [rules (defrules
                  {:then [[1 :is "odd"]
                          [2 :is "even"]]})
          facts [[:does] [:not] [:matter]]]
      (is (= (rules facts)
             #{[1 :is "odd"] [2 :is "even"]})))))
