;;
;; So far  there ist no  CLI functionalitiy  in this project.  This is
;; testing. See also the test/ sibling directory of src/.
;;
(ns regelwerk.main
  (:require [regelwerk.alpha :as alpha])
  (:gen-class))

(defn -main [& args]
  ;; Because I am too lazy to type it every time:
  (alpha/main "resources/facts.edn" "resources/rules.edn"))
