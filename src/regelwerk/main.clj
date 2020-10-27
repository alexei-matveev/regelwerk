;;
;; Try using core from outside.
;;
(ns regelwerk.main
  (:require [regelwerk.core :as rwk])
  (:gen-class))

(defn- main [facts-path rules-path]
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
