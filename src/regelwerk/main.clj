;;
;; Try using core  from outside. See also the  test/ sibling directory
;; of src/.
;;
(ns regelwerk.main
  (:require [regelwerk.core :as core]
            [regelwerk.lambda :as lambda])
  (:gen-class))

;;
;; These rules can be used to  simulate reading rules at run time from
;; some external source. The expression  part will be eval-ed, so that
;; it  effectively  alows  calling   arbitrary  code  ---  a  security
;; nightmare in some scenarios.
;;
;; FIXME: we had  imported clojure.string as str befor.  In CIDER that
;; abbreviation  is understood.   With "lein  run" one  gets "No  such
;; namespace: str". Hence fully qualified symbols here.
;;
(defn- unused-rules []
  (quote
   [([?a ?b]
     [[?b :x ?a]
      [?a :y (clojure.string/upper-case ?b)]]
     ;; when:
     [[?a :is ?b]])
    ([?a ?b] [[:ab :glued (clojure.string/join "-" [?a ?b])]] [[?a :is ?b]])
    ;; Side effect are possibles, but do you really mean it?
    ([?a ?b]
     (println "nil expression is like an empty seq")
     ;; when
     [[?a :is ?b]])
    ([?a ?b]
     (println {:a ?a, :b ?b})
     ;; when
     [[?a :is ?b]])
    ;; The  *ns*  dynvar  evaluates  to  clojure.core  when  run  from
    ;; Leiningen  or a  Jar  file.  Only  in CIDER  it  happens to  be
    ;; regelwerk.core, accidentally  this is  also when the  alias str
    ;; for clojure.string happens to work.
    ([?a ?b]
     (do
       (println *ns*)
       (println "fire missles, at every match")
       [[:missles :were "fired"]])
     ;; when
     [[?a :is ?b]])]))

;; This is  how you adapt rules  that produce new facts  to make rules
;; that *insert* new facts:
(defn- dress [rules]
  (fn [facts]
    ;; In case it was just an vector, convert it to facts:
    (let [db (set facts)]
      (clojure.set/union db (rules db)))))

(defn- main [facts-path rules-path]
  ;; Here slurp-edn would also work  for facts, except it would return
  ;; a list, not a set:
  (let [facts (core/read-facts facts-path)
        rules (core/load-rules rules-path)]

    ;; Fixpoint  calculation  only  terminates  if the  rules  do  not
    ;; inflate the fact table indefinitely:
    (println (lambda/fix rules facts))

    ;; Very functional way to iterate lazily:
    ;;
    ;;     (iterate f x) = (x (f x) (f (f x)) ...)
    ;;
    ;; You may want to compare behaviour of the original and "dressed"
    ;; rules.
    (doseq [facts (take 4 (iterate (dress rules) facts))]
      (println facts))))

(defn -main [& args]
  ;; Because I am too lazy to type it every time.
  (main "resources/facts.edn" "resources/rules.edn"))
