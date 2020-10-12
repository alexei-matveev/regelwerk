(ns regelwerk.core
  (:require [datascript.core :as d]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.edn :as edn])
  (:gen-class))

;; This is significantly slower than the compareable SQLite code. See
;; also
;; https://stackoverflow.com/questions/42457136/recursive-datalog-queries-for-datomic-really-slow
(defn- unused-example [n]
  (let [rules '[[(follows ?a ?b)
                 [?a ?b]]
                [(follows ?a ?b)
                 [?x ?b]
                 (follows ?a ?x)]]
        query '[:find ?a ?b
                :in $ %
                :where
                (follows ?a ?b)]
        ;; Looks like ([0 1] [1 2] [2 3]) for n = 3:
        db (for [i (range n)]
               [i (inc i)])]
    (d/q query db rules)))

(comment
  (unused-example 3)
  =>
  #{[2 3] [1 3] [0 3] [0 2] [1 2] [0 1]})

(comment
  ;;
  ;; Many possible syntaxes for rules. This is plain data:
  ;;
  {:vars '[?a ?b] :when '[[?a :is ?b]] :then '[[:a ?a :b ?b]]}
  ;;
  ;; With makros you may get rid of quoting, but likely have to decide
  ;; for syntax ...
  ;;
  ;; vars when then
  ;; vars then when
  ;; when then vars
  ;; when vars then
  ;; then vars when
  ;; then when vars
  ;;
  (defrule [?a ?b]
    [[?a :is ?b]] => [[:a ?a :b ?b]])
  (defrule named-rule [?a ?b]
    [[?a :is ?b]] => [[:a ?a :b ?b]])
  (forall [?a ?b] [[?a :is ?b]] => [[:a ?a :b ?b]])
  (for-each [?a ?b] :where [[?a :is ?b]] => [:a ?a :b ?b])
  (set-of [:a ?a :b ?b] :for-all [?a ?b] :such-that [[?a :is ?b]])
  (produce [:a ?a :b ?b] :from [?a ?b] :where [[?a :is ?b]]))

;;
;; Macros  [1]  might be  the  second  best  choice to  define  rules,
;; especially if you define them in  the source code.  The best choice
;; is supposedly  "data", because  "data" =  "code", you've  heard the
;; story many times.  But, how usefull will makros be when you need to
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
(defn- sexp-rule [vars where expr]
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

(defmacro defrule [vars where expr]
  (sexp-rule vars where expr))

;; C-u C-x C-e if you want to see the expansion:
(comment
  (macroexpand '(defrule [?a ?b] [[?a :is ?b]] [[?b ?a]])))

(defn- test-1 []
  (let [rule (defrule [?a ?b]
               [[?a :is ?b]]
               ;; =>
               [[?b :x ?a]
                [?a :y (str/upper-case ?b)]])
        facts [[1 :is "odd"]
               [2 :is "even"]]]
    (= (rule facts)
       #{["odd" :x 1] ["even" :x 2] [1 :y "ODD"] [2 :y "EVEN"]})))

;; (test-1) => true

;;
;; It will rarely stay  by one rule. Do we need a  makro for that? The
;; simplest  extension is  to accept  a list  of 3-tuples  (vars where
;; expr).
;;
(comment
  (define-rules
    ([?a ?b]
     ([?a :eq ?b] => [?b :eq ?a])
     ([?a :le ?b] [?b :le ?a] => [?a :eq ?b]))
    ([?x ?y]
     ([?x :eq ?t] [?t :eq ?y] => [?x :eq ?y]))))

;; C-u C-x C-e if you want to see the expansion:
(comment
  (macroexpand '(defrules
                  ([?a ?b] [[?a :is ?b]] [[?b ?a]])
                  ([?x ?y] [[?y :is ?y]] [[?x ?y]]))))

(defmacro defrules [& arities]
  (let [fs (for [[vars where expr] arities]
             (sexp-rule vars where expr))]
    `(fn [facts#]
       (into #{} cat (for [f# [~@fs]]
                       (f# facts#))))))

;; (test-2) => true
(defn- test-2 []
  (let [rules (defrules
                ([?a ?b] [[?a :is ?b]] [[?b ?a]])
                ([?a ?b] [[?a :is ?b]] [[?a ?b]]))
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
   [([?a ?b] [[?a :is ?b]] [[?b :x ?a]
                            [?a :y (clojure.string/upper-case ?b)]])
    ([?a ?b] [[?a :is ?b]] [[:ab :glued (clojure.string/join "-" [?a ?b])]])
    ;; Side effect are possibles, but do you really mean it?
    ([?a ?b] [[?a :is ?b]] (println
                            "nil expression is like an empty seq"))
    ([?a ?b] [[?a :is ?b]] (println
                            {:a ?a, :b ?b}))
    ;; The  *ns*  dynvar  evaluates  to  clojure.core  when  run  from
    ;; Leiningen  or a  Jar  file.  Only  in CIDER  it  happens to  be
    ;; regelwerk.core, accidentally  this is  also when the  alias str
    ;; for clojure.string happens to work.
    ([?a ?b] [[?a :is ?b]] (do
                             (println *ns*)
                             (println "fire missles, at every match")
                             [[:missles :were "fired"]]))]))

;;
;; This should read all objects from  the file with edn data. Not just
;; the fist one that is returned bei clojure.edn/read ...
;;
(defn- read-seq [stream]
  ;; This unique sentinel object will be returned on EOF bei edn/read:
  (let [eof (Object.)]
    (letfn [(parse [stream]
              (lazy-seq
               (cons (edn/read {:eof eof} stream)
                     (parse stream))))]
      ;; EOF itself is however not part of the sequence:
      (take-while #(not= eof %) (parse stream)))))

;; Really   read  them,   still  from   the  resource   file  in   the
;; Jar.
(defn- read-rules []
  (-> "rules-1.edn"
      (io/resource)
      (io/reader)
      (java.io.PushbackReader.)
      (read-seq)))

(comment
  (read-rules)
  =>
  (([?a ?b] [[?a :is ?b]] [[?a :le ?b] [?a :ge ?b]])
   ([?a ?b] [[?b :is ?a]] [[?a :le ?b] [?a :ge ?b]])))

;; Read rules, splice them into the macro form and eval. This produces
;; "rules-as-a-function"  basically  in  the  same way  as  the  macro
;; "defrules", albeit at run time.
(defn- make-rules []
  (let [arities (read-rules)
        code `(defrules ~@arities)]
    (eval code)))

;; (test-3) => true
(defn- test-3 []
  (let [rules (make-rules)
        facts [[1 :is "one"]
               [2 :is "two"]]]
    (= (rules facts)
       #{["two" :ge 2] [2 :ge "two"]
         [1 :le "one"] ["one" :le 1]
         ["one" :ge 1] [1 :ge "one"]
         [2 :le "two"] ["two" :le 2]})))

;;
;; This is something else entirely ...
;;
(defn- parse [path]
  (edn/read (java.io.PushbackReader. (io/reader path))))

(defn- main [db-file rules-file query-file]
  (let [db (parse db-file)
        rules (parse rules-file)
        query (parse query-file)]
    (d/q query db rules)))

(defn -main [& args]
  (println (test-1))
  (println (test-2))
  (println (test-3))
  #_(println (apply main args)))

;; For your C-x C-e pleasure:
(comment
  (main "resources/db.edn"
        "resources/rules.edn"
        "resources/query.edn"))
