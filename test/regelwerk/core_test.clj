(ns regelwerk.core-test
  (:require [clojure.test :refer :all]
            [regelwerk.core :refer :all]))

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
