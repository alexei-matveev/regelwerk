(ns regelwerk.core-test
  (:require [clojure.test :refer :all]
            [regelwerk.core :refer :all]))

#_(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

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
