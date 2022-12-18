(ns regelwerk.lambda)

;; Fixpoint   calculation.    As   a   "poor   man"   recursive   rule
;; application. Should  we use  clojure.set/union or (into  facts ...)
;; or yet  something else? Do we  convert the input with  (set ...) or
;; assume the  caller is responsible?   What do we actually  assume of
;; our  databases representations?   Otherwise  this  is very  generic
;; procedure that requires  neither regelwerk.core nor datascript.core
;; of the imports above.
(defn fix [rules facts & rest]
  ;; In case some "careless" caller supplied facts as a vector convert
  ;; them to a set at the outset:
  (loop [facts (set facts)
         extra #{}]
    (let [extra' (apply rules facts rest)]
      ;; (println "did it again ...")
      (if (= extra' extra)
        facts
        (recur (clojure.set/union facts extra')
               extra')))))

;; What were  the name  for a  pattern when  searching for  a fixpoint
;; while  keeping the  two tables  separate?  Think  of a  binary rule
;; [$extra $facts] -> $extra' and repeating again and again.  This can
;; be viewed  as a fixpoint on  $extra in a world  of global immutable
;; $facts.  Or,  equivalently, a  fixpoint on  tuple where  the second
;; half is  passed unmodified. However returning  multiple fact tables
;; is not how a plain rule set works ...
