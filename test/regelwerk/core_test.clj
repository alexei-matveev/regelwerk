(ns regelwerk.core-test
  (:require [clojure.test :refer :all]
            [regelwerk.core :refer :all]))

(deftest demo-0
  (testing "Demo for the README ..."
    (let [facts [[1 :is "odd"]
                 [2 :is "even"]]
          rule (defrule [?even-number]
                 ;; For each even number produce these facts in
                 ;; German:
                 [[?even-number :ist "gerade"]
                  [(inc ?even-number) :ist "ungerade"]
                  [(dec ?even-number) :ist "ungerade"]]
                 ;; Datalog query for even numbers from the set of
                 ;; English facts:
                 [[?even-number :is "even"]])]
      (is (= (rule facts)
             #{[1 :ist "ungerade"]
               [2 :ist "gerade"]
               [3 :ist "ungerade"]})))))

(deftest test-1
  (testing "Function calls in expressions ..."
    (let [rule (defrule [?a ?b]
                 [[?b :x ?a]
                  [?a :y (clojure.string/upper-case ?b)]]
                 ;; <-
                 [[?a :is ?b]])
          facts [[1 :is "odd"]
                 [2 :is "even"]]]
      (is (= (rule facts)
             #{["odd" :x 1] ["even" :x 2] [1 :y "ODD"] [2 :y "EVEN"]})))))

(deftest test-1a
  (testing "Function producing facts in expressions ..."
    (let [f (fn [a b]
              [[b :x a]
               [a :y (clojure.string/upper-case b)]])
          rule (defrule [?a ?b]
                 (f ?a ?b)
                 ;; <-
                 [[?a :is ?b]])
          facts [[1 :is "odd"]
                 [2 :is "even"]]]
      (is (= (rule facts)
             #{["odd" :x 1] ["even" :x 2] [1 :y "ODD"] [2 :y "EVEN"]})))))


(deftest test-2
  (testing "Multiple rules in a single macro call (map syntax)"
    (let [rules (defrules
                  {:find [?a ?b], :then [[?b ?a]], :when [[?a :is ?b]]}
                  {:find [?a ?b], :then [[?a ?b]], :when [[?a :is ?b]]})
          facts [[1 :is "odd"]
                 [2 :is "even"]]]
      (is (= (rules facts)
             #{[1 "odd"] ["odd" 1] [2 "even"] ["even" 2]})))))

(deftest test-3
  (testing "Facts and queries for longer rows beyond EAV (list syntax)"
    (let [rules (defrules
                  ([?a ?b] [[?b ?a]] [[?a :is :like ?b]]))
          facts [[1 :is :like "one" :score 42 "many" "more" "attrs"]
                 [2 :is :like "two" :score 99]]]
      (is (= (rules facts)
             #{["two" 2] ["one" 1]})))))

;; Do you still hope to be able to extract free logic variables out of
;; an arbitrary expression?
(deftest test-4
  (testing "Logic variables and bindings in a single expression (map syntax)"
    (let [rules (defrules
                  {:find [?a]
                   :then [[?a :vs (let [?a (str "p" ?a)] ?a)]]
                   :when [[?a]]})
          facts [[1] [2]]]
      (is (= (rules facts)
             #{[2 :vs "p2"] [1 :vs "p1"]})))))

(deftest test-5
  (testing "Single arity rule is plain facts ..."
    (let [rules (defrules
                  ([[1 :is "odd"]
                    [2 :is "even"]]))
          rulez (defrules
                  {:then [[1 :is "odd"]
                          [2 :is "even"]]})
          facts [[:does] [:not] [:matter]]]
      (is (= (rules facts)
             (rulez facts)
             #{[1 :is "odd"] [2 :is "even"]})))))
