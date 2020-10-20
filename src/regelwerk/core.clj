;;
;; Macros  [1]  might be  the  second  best  choice to  define  rules,
;; especially if you define them in  the source code.  The best choice
;; is supposedly  "data", because  "data" =  "code", you've  heard the
;; story many times.  But, how usefull will macros be when you need to
;; read rules  at run time  from a  user-supplied file or  URL?  Well,
;; Clojure  "eval" does  macro  expansion as  expected  [2], see  also
;; below. At that point it is not very far from reading data, building
;; and evaluating the code with plain old functions.
;;
;; FWIW, the  Datascript query  is already  plain data.   You actually
;; only  need a  macro  to  turn symbolic  expressions  "expr" into  a
;; function of arguments "vars".
;;
;; [1] https://www.braveclojure.com/writing-macros/
;; [2] https://www.braveclojure.com/read-and-eval/
;;
(ns regelwerk.core
  (:require [datascript.core :as d]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.edn :as edn])
  (:gen-class))

;;
;; Many  possible  syntaxes  for   rules.   FIXME:  Maybe  one  should
;; postulate that a "rule without body is a fact"?  "A fact is true no
;; matter what", like  PAIP says. This could constrain  the search the
;; "most parctical" syntax  by e.g.  favoring the  "head first" syntax
;; as in Prolog:
;;
;;      Fact.
;;      Head (Vars) <- Body (Vars).
;;
;; Rules could be plain data.  There is only six permutations.
;;
;;     vars head body <- current
;;     vars body head <- does not reduce to fact with empty body?
;;     body head vars
;;     body vars head
;;     head vars body
;;     head body vars
;;
;; It may help to think of "body" = "when" & "head" = "then".
;;
(comment
  (quote
   {:then [["water" :is "wet"]]}
   {:vars [?a ?b] :when [[?a :is ?b]] :then [[?a :is :object]
                                             [?b :is :adjective]]})
  (defrule [?a ?b]
    [[?a :is ?b]] => [[:a ?a :b ?b]])
  (defrule named-rule [?a ?b]
    [[?a :is ?b]] => [[:a ?a :b ?b]])
  (forall [?a ?b] [[?a :is ?b]] => [[:a ?a :b ?b]])
  (for-each [?a ?b] :where [[?a :is ?b]] => [:a ?a :b ?b])
  (set-of [:a ?a :b ?b] :for-all [?a ?b] :such-that [[?a :is ?b]])
  (produce [:a ?a :b ?b] :from [?a ?b] :where [[?a :is ?b]]))

;; This  will  return   the  *code*  for  the  rule   as  function  of
;; facts. People tend to call it compilaiton, that is why the name:
(defn- compile-rule [vars expr where]
  ;; This will be a funciton of a fact database:
  `(fn [facts#]
     ;; Compute the  result set by  querieng facts with  Datascript. A
     ;; Datascript  DB can  be as  simple as  a collection  of tuples,
     ;; mostly EAV-tuples.
     (let [rows# (d/q '[:find ~@vars :where ~@where] facts#)]
       ;; Generate another set of objects from the supplied collection
       ;; valued  expression binding  each row  of the  result set  to
       ;; variables  of a  vector.   Clojure indeed  allows binding  a
       ;; vector of values to vector of  symbols --- a special case of
       ;; "destructuring bind", so it is called, I think.
       (into #{} cat
             (for [row# rows#]
               (let [~vars row#] ~expr))))))

(defmacro defrule [vars expr where]
  (compile-rule vars expr where))

;; C-u C-x C-e if you want to see the expansion:
(comment
  (macroexpand '(defrule [?a ?b] [[?b ?a]] [[?a :is ?b]])))

;; (test-1) => true
(defn- test-1 []
  (let [rule (defrule [?a ?b]
               [[?b :x ?a]
                [?a :y (str/upper-case ?b)]]
               ;; <-
               [[?a :is ?b]])
        facts [[1 :is "odd"]
               [2 :is "even"]]]
    (= (rule facts)
       #{["odd" :x 1] ["even" :x 2] [1 :y "ODD"] [2 :y "EVEN"]})))

;; (test-1a) => true
(defn- test-1a []
  (let [f (fn [a b]
            [[b :x a]
             [a :y (str/upper-case b)]])
        rule (defrule [?a ?b]
               (f ?a ?b)
               ;; <-
               [[?a :is ?b]])
        facts [[1 :is "odd"]
               [2 :is "even"]]]
    (= (rule facts)
       #{["odd" :x 1] ["even" :x 2] [1 :y "ODD"] [2 :y "EVEN"]})))

;;
;; It will rarely stay  by one rule. Do we need a  macro for that? The
;; simplest  extension is  to accept  a  list of  3-tuples (vars  head
;; body).  This could  be one of the possible syntaxes  -- if you dont
;; enclose head  and body into  extra praens  [] you need  a separator
;; like :- or <- between them:
;;
(comment
  (define-rule [?x ?y]
    [?x :eq ?y] <- [?x :eq ?t] [?t :eq ?y])

  (define-rule [?x ?y]
    [?x :eq ?y] [?y :eq ?x] <- [?x :eq ?t] [?t :eq ?y])

  (define-rule []
    [1 :eq "one"]
    ["two" :eq 2])

  (define-rules
    ([?a ?b]
     [?b :eq ?a] <- [?a :eq ?b])

    ;; Two facts in the head:
    ([?a ?b]
     [?a :eq ?b] [?b :eq ?a] <- [?a :le ?b] [?b :le ?a])

    ;; This  is how  a  "rule  without body"  aka  "facts" could  look
    ;; like. Is it worth it?
    ([]
     [1 :eq "one"]
     [2 :eq "two"])))

;; C-u C-x C-e if you want to see the expansion:
(comment
  (macroexpand '(defrules
                  ([?a ?b] [[?b ?a]] [[?a :is ?b]])
                  ([?x ?y] [[?x ?y]] [[?y :is ?y]]))))

(defmacro defrules [& arities]
  (let [fs (for [[vars expr where] arities]
             (compile-rule vars expr where))]
    `(fn [facts#]
       (into #{} cat (for [f# [~@fs]]
                       (f# facts#))))))

;; (test-2) => true
(defn- test-2 []
  (let [rules (defrules
                ([?a ?b] [[?b ?a]] [[?a :is ?b]])
                ([?a ?b] [[?a ?b]] [[?a :is ?b]]))
        facts [[1 :is "odd"]
               [2 :is "even"]]]
    (= (rules facts)
       #{[1 "odd"] ["odd" 1] [2 "even"] ["even" 2]})))

;;
;; Simulate reading rules  at run time from some  external source. The
;; expression  part  will be  eval-ed,  so  that it  effectivel  alows
;; calling arbitrary code --- a security nightmare in some scenarios.
;;
;; FIXME:  we  imported clojure.string  as  str  here. In  CIDER  that
;; abbreviation  is understood.   With "lein  run" one  gets "No  such
;; namespace: str". Hence fully qualified symbols here.
;;
(defn- demo-rules []
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

;; This should read all objects from a strea of edn text. Not just the
;; first object that is returned bei clojure.edn/read ...
(defn- read-seq [stream]
  ;; This unique sentinel object will be returned on EOF bei edn/read:
  (let [eof (Object.)]
    (letfn [(parse [stream]
              (lazy-seq
               (cons (edn/read {:eof eof} stream)
                     (parse stream))))]
      ;; EOF itself is however not part of the sequence:
      (take-while #(not= eof %) (parse stream)))))

;; Read EDN  in full, from  a file or a  resource in the  JAR.  Supply
;; either  a  path  like  "resources/rules.edn"  or  a  resource  like
;; (io/resource "rules.edn"):
(defn- slurp-edn [source]
  (-> source
      (io/reader)
      (java.io.PushbackReader.)
      (read-seq)))

(comment
  (= (slurp-edn "resources/rules.edn")
     (slurp-edn (io/resource "rules.edn"))))

;; Read and eval rules, splice them into the macro form and eval. This
;; produces  "rules-as-a-function" basically  in the  same way  as the
;; macro "defrules", albeit at run time.
(defn- load-rules [source]
  (let [arities (slurp-edn source)
        code `(defrules ~@arities)]
    (eval code)))

;; (test-3) => true
(defn- test-3 []
  (let [rules (load-rules (io/resource "rules.edn"))
        facts [[0 :is :int]
               [100 :is :int]]]
    (= (rules facts)
       #{[-1 :is :int] [0 :is :int] [1 :is :int]
         [99 :is :int] [100 :is :int] [101 :is :int]})))

(defn- main [facts-path rules-path]
  (println (test-1))
  (println (test-1a))
  (println (test-2))
  (println (test-3))
  ;; Here slurp-edn just slurps any EDN in full, work also for facts:
  (let [facts (slurp-edn facts-path)
        rules (load-rules rules-path)]
    (println facts)
    (println (rules facts))
    (println (rules (rules facts)))))

(defn -main [& args]
  ;; Because I am too lazy to type it every time.
  (main "resources/facts.edn" "resources/rules.edn"))
