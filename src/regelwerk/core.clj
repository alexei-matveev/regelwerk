;;
;; Macros  [1]  might be  the  second  best  choice to  define  rules,
;; especially if you define them in  the source code.  The best choice
;; is supposedly  "data", because  "data" =  "code", you've  heard the
;; story many times.  But, how usefull will macros be when you need to
;; read rules  at run time  from a  user-supplied file or  URL?  Well,
;; Clojure  "eval" does  macro  expansion as  expected  [2], see  also
;; below. At that point it is not very far from reading rules from the
;; source, and compiling them into code with plain old functions.
;;
;; FWIW, the Datascript query is already plain data.  You actually may
;; only need  a macro  to turn  symbolic expressions  "expr" producing
;; arbitrary new facts  into a function of arguments  "vars".  Also it
;; appears to be  a hairy problem to "extract free  variables" from an
;; expression. Is  it?  For  simplicity we supply  a vector  of "vars"
;; everywhere.  See also clojure.core.unify  where the unused function
;; to search for logical variables, lvars, does not look deep into the
;; form:
;;
;;     (extract-lvars '[1 ?x (str ?y) (quote ?z)]) => #{?x}
;;
;; [1] https://www.braveclojure.com/writing-macros/
;; [2] https://www.braveclojure.com/read-and-eval/
;;
(ns regelwerk.core
  (:require [datascript.core :as d]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

;;
;; Many possible syntaxes for rules  are possible to choose from. This
;; is probably  the case  when the  choice is  bad.  Maybe  one should
;; postulate that a "rule without body is a fact"?  "A fact is true no
;; matter what", like PAIP says.  This could limit the choises for the
;; "most parctical" syntax  by e.g.  favoring the  "head first" syntax
;; as in Prolog:
;;
;;      Fact.
;;      Head (Vars) <- Body (Vars).
;;
;; There is only six permutations.
;;
;;     vars head body <- current
;;     vars body head <- does not reduce to fact with empty body?
;;     body head vars
;;     body vars head
;;     head vars body
;;     head body vars
;;
;; One might  start to think of  rules as plain data  with essentially
;; three  blocks:   generated  facts,   conditions,  and   mostly  for
;; technichal reasons a vector of variables.   It may help to think of
;; "body" = "when" & "head" = "then" as in this structure:
;;
;;   {:then [["water" :is "wet"]]}
;;   {:find [?a ?b] :when [[?a :is ?b]] :then [[?a :is "object"]]}
;;
;; The  latter   form  is  almost   a  valid  Datascript   query  with
;; s/where/when/ and  extended by a "then"  expression.  Basically any
;; syntax would encode the same three parts, so why bother?
;;
;;   (define-rule [?a ?b]
;;     [[?a :is ?b]] => [[:a ?a :b ?b]])
;;
;;   (define-rule named-rule [?a ?b]
;;     [[?a :is ?b]] => [[:a ?a :b ?b]])
;;

;; This  will  return   the  *code*  for  the  rule   as  function  of
;; facts. People tend to call it compilaiton, that is why the name:
(defn- do-compile-rule [vars expr where]
  ;; This will be a function of a fact database:
  `(fn [facts#]
     ;; Compute the  result set by  querieng facts with  Datascript. A
     ;; Datascript  DB can  be as  simple as  a collection  of tuples,
     ;; mostly  EAV-tuples. The  map-form of  the Datascript  query is
     ;; more  readable  for  machines, no  need  for  unquote-splicing
     ;; either.  FWIW,  the Datascript  syntax does  not seem  to make
     ;; sense without vars. So that the rules with empty list of logic
     ;; variables do  not compile  at the moment.   FIXME: how  do you
     ;; check for an existing EAV fact with concrete E, A and V in the
     ;; DB and generate  new facts depending on  its existence?  Think
     ;; of "conditional facts" as opposed to "unconditional facts".
     (let [rows# (d/q '{:find ~vars, :where ~where} facts#)]
       ;; Generate another set of objects from the supplied collection
       ;; valued  expression binding  each row  of the  result set  to
       ;; variables  of a  vector.   Clojure indeed  allows binding  a
       ;; vector of values to vector of  symbols --- a special case of
       ;; "destructuring bind", so it is called, I think.
       (into #{} cat       ; transducer works as (reduce into #{} ...)
             (for [row# rows#]
               (let [~vars row#] ~expr))))))

;; Hm,  these  arity-1  functions  of (unused)  facts  are  not  quite
;; intuitive. No sane rule will return anything starting from an empty
;; fact table: (rule  #{}) == #{}, quite intuitively.   But these fact
;; functions may and will. Think of Clojure function "constantly" that
;; takes input but ignores it completly. What is the arity-0 for?
(defn- do-compile-facts [expr]
  ;; This must be also a function of a fact database:
  `(fn [unused-facts#] (set ~expr)))

;; This will also return the unevaled  *code* of the function, not the
;; actual function:
(defn- compile-rule [form]
  (cond
    ;; Case of a map like {:find [...], :when [...], :then [...]}:
    (map? form)
    (if-let [vars (:find form)]
      ;; Regular case with all three clauses:
      (do-compile-rule vars
                       (:then form)
                       (:when form))
      ;; FIXME: get  rid of  this ugly  special case  for "standalone"
      ;; unconditional facts.  We even ignore  whetever ist is  in the
      ;; :when clause here.
      (do-compile-facts (:then form)))

    ;; Another ugly special  case. These (comment ...)  forms are read
    ;; as such.  Is it too much of a special case?  Or are we starting
    ;; to  write an  interpreter?  FIXME:  one should  probably filter
    ;; them  out earlier  in compile-rules  instead of  producing noop
    ;; stubs.
    (= 'comment (first form))
    `(constantly #{})

    ;; FIXME:  get  rid of  this  ugly  special case  of  "standalone"
    ;; unconditional  facts.   One  expression in  list  evaluates  to
    ;; facts:
    (= 1 (count form))
    (let [[expr] form]
      (do-compile-facts expr))

    ;; Regular case. Three expressions in a list => regular rule:
    :else
    (let [[vars then when] form]
      (do-compile-rule vars then when))))

;; This defrule would not work with a map
(defmacro defrule [& forms]
  (compile-rule forms))

;; C-u C-x C-e if you want to see expansions:
(comment
  (compile-rule '{:find [?a ?b]
                  :then [[?b ?a]]
                  :when [[?a :is ?b]]})
  (compile-rule '([?a ?b]
                  [[?b ?a]]
                  [[?a :is ?b]]))
  (macroexpand '(defrule [?a ?b] [[?b ?a]] [[?a :is ?b]])))

;;
;; That (into #{} ...) in the  compile-rule code for the new generated
;; facts could have been also (into facts# ...) if we decided that the
;; rules not only return the *new*  facts but instead *extend* the set
;; of facts we started from.  Many  rule engines do *insert* new facts
;; into some "global" database of facts.   It might be not a very good
;; idea at  the end,  but I  cannot quite articulate  why. Here  is an
;; attempt:
;;
;; - The initial facts dont actually need to  be a Set like #{}. It is
;;   common, also in this code to use  a vector [] when actually a Set
;;   ist  meant  --- just  look  at  all  the examples.   Moreover,  a
;;   Datascript quqery will accept a proper DB of facts as input. Will
;;   it also support (into ...)?
;;
;; - A  generalization  of  the  above objection  might  be  a  strict
;;   distinction between  domain and  range of a  function ---  we did
;;   postulate   that   "rules   are   just   a   function   producing
;;   facts". Inserting new facts into existing DB kind of assumes that
;;   range and domain are identical. This is a serious assumption.
;;

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

;; Other rule engines  will probably want to get  all rule definitions
;; at once in  order to apply optimizations when  transforming them to
;; code.  We are not that sophisticated and "compile" rules one by one
;; here.  But  this is likely  better suited to  become a part  of the
;; public interface.
;;
;; Normally the rule consists of  variable vector, an expression and a
;; where query --- a  list of three forms. However we  want to be able
;; to  special  case  on  the  "universal"  facts  free  of  variables
;; conditions  ---  a  rule  with   just  one  form,  the  fact-valued
;; expression itself. The  length of lists in  the sequence "artities"
;; will  thus  be  typicall  3  and somtimes  1.   The  working  horse
;; "compile-rule" is supposed to handle both cases accordingly.
(defn- compile-rules [arities]
  (let [fs (map compile-rule arities)]
    `(fn [facts#]
       (into #{} cat (for [f# [~@fs]]
                       (f# facts#))))))

(defmacro defrules [& arities]
  (compile-rules arities))

;; C-u C-x C-e if you want to see the expansion:
(comment
  (macroexpand
   '(defrules
      ;; vars |  head   |  body
      ;;------|---------|------------
      ([?a ?b] [[?a ?b]] [[?a :is ?b]])
      ([?x ?y] [[?y ?x]] [[?x :is ?y]]))))

;; This should  read all objects from  a stream of edn  text. Not just
;; the first object that is returned bei clojure.edn/read ...
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
(defn load-rules [source]
  (let [arities (slurp-edn source)
        code `(defrules ~@arities)]
    (eval code)))

;; For completness we provide read-facts too. Facts should preferrably
;; handled as a Sets.
(defn read-facts [source]
  (set (slurp-edn source)))

;; There might be  a use-case for read-rules too.   In fact read-rules
;; could be identical with read-facts because both are just slurp-edn.
;; Imagine  reading  rules from  several  sources,  merging them  with
;; set/union and compiling.  Ah, yes, one would  need compile-rules as
;; well.
