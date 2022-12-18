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
;; Many  syntaxes for  rules  are  possible to  choose  from. This  is
;; probably  the  case when  the  choice  is  bad.  Maybe  one  should
;; postulate that a "rule without body is a fact"?  "A fact is true no
;; matter what", like PAIP says.  This could limit the choises for the
;; "most parctical" syntax  by e.g.  favoring the  "head first" syntax
;; as in Prolog:
;;
;;      Fact.
;;      Head (Vars) <- Body (Vars).
;;
;; We try to think  of rules as plain data with  Vars, Head, and Body.
;; There are six  permutations of those three  building blocks.  Maybe
;; we should not even try to choose an order and just give them names?
;; These three blocks are:  generated facts aka consequent, conditions
;; aka antecedents,  and mostly  for technichal  reasons, a  vector of
;; variables.   Here is  a structure  with three  blocks and  "catchy"
;; 4-letter names:
;;
;;   {:find [?a ?b] :when [[?a :is ?b]] :then [[?a :is "thing"]]}
;;   {:then [["water" :is "wet"]]}   ;; But see the empty graph lemma!
;;
;; The former  is almost a  valid Datascript query  with s/where/when/
;; and extended  by a "then"  expression.  Basically any  syntax would
;; encode the same three parts, so why bother?
;;

;; This will return the *code* of a  function for use in a macro or in
;; an eval.  Hence the name.  N-ary  ready, see (fn [&  args#] ...) in
;; the code.
(defn- compile-rule [rule]
  ;; Here (:from rule) may be nil, but even in that case query expects
  ;; exactly one dataset as input. Datascript leave declaration of the
  ;; single dataset, {:from [$], :when [[$ ...], ...], ...}, optional.
  (let [{from :from, vars :find, expr :then, when :when} rule]
    ;; It will be a function one  or more datasets.  As declared, this
    ;; function  accepts arbitrary  number of  datasets.  However  the
    ;; Datalog  query in  the body  may  complain about  "Too few"  or
    ;; "Extra"  inputs  if   the  number  does  not   agree  with  the
    ;; declarations in the IN/FROM-clause.
    ;;
    ;; Compute the  result set  by querieng  facts with  Datascript. A
    ;; Datascript  DB can  be as  simple  as a  collection of  tuples,
    ;; mostly EAV-tuples. The map-form of the Datascript query is more
    ;; readable  for machines,  no need  for unquote-splicing  either.
    ;; FWIW, the Datascript syntax does not seem to make sense without
    ;; vars. So that  the rules with empty list of  logic variables do
    ;; not compile  at the  moment.  FIXME:  how do  you check  for an
    ;; existing  EAV fact  with concrete  E,  A and  V in  the DB  and
    ;; generate  new  facts  depending  on its  existence?   Think  of
    ;; "conditional facts" as opposed to "unconditional facts".
    `(fn [& args#]
       ;; Datascript appears to handle the case of nil for the
       ;; IN-clause just OK:
       (let [rows# (apply d/q
                          '{:find ~vars, :in ~from, :where ~when}
                          args#)]
         ;; Generate another set of objects from the supplied collection
         ;; valued  expression binding  each row  of the  result set  to
         ;; variables  of a  vector.   Clojure indeed  allows binding  a
         ;; vector of values to vector of  symbols --- a special case of
         ;; "destructuring bind", so it is called, I think.
         (into #{} cat     ; transducer works as (reduce into #{} ...)
               (for [row# rows#]
                 (let [~vars row#] ~expr)))))))

;; Hm, these  functions of arbitrary  number of (unused)  datasets are
;; not quite  intuitive. No  sane rule  will return  anything starting
;; from an  empty fact  table: (rule #{})  == #{},  quite intuitively.
;; But these  fact functions may  and will. Think of  Clojure function
;; "constantly" that takes input but ignores it completly.
;;
;; N-ary ready.   See (fn [&  unused-args#] ...).  In fact  it accepts
;; arbitrary number of the databases and ignores them.
(defn- compile-facts [expr]
  ;; This must be also a function of a fact database:
  `(fn [& unused-args#] (set ~expr)))

;; This will also return the unevaled  *code* of the function, not the
;; actual function:  Only Maps like  {:find [...], :when  [...], :then
;; [...]}  are  accepted, find-clause  maybe missing for  a deprecated
;; use case.
;;
;; N-ary ready because compile-rule & compile-facts are.
(defn- compile-legacy [form]
  (if (:find form)
    ;; Regular case:
    (compile-rule form)
    ;; FIXME: get  rid of  this ugly  special case  for "standalone"
    ;; unconditional facts.   We even ignore  whetever it is  in the
    ;; when clause here.  See "Empty  Graph Lemma": The empty set of
    ;; triples is  entailed by  any graph, and  does not  entail any
    ;; graph except itself [1]. This basically means en empty set of
    ;; facts does not allow anything to be derived from it!
    ;;
    ;; [1] https://www.w3.org/TR/rdf-mt/#entail
    (compile-facts (:then form))))

;; This defrule only works with a map. N-ary ready because
;; compile-rule is.
(defmacro defrule [rule]
  (compile-rule rule))

;; C-u C-x C-e if you want to see expansions:
(comment
  (do-compile-rule
   '{:find [?a ?b]
     :then [[?b ?a]]
     :when [[?a :is ?b]]})
  (macroexpand
   '(defrule {:find [?a ?b]
              :then [[?b ?a]]
              :when [[?a :is ?b]]})))

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
;;   Datascript query will accept a proper  DB of facts as input. Will
;;   it also support (into ...)?
;;
;; - A  generalization  of  the  above objection  might  be  a  strict
;;   distinction between  domain and  range of a  function ---  we did
;;   postulate   that   "rules   are   just   a   function   producing
;;   facts". Inserting new facts into existing DB kind of assumes that
;;   range and domain are identical. This is a serious assumption.
;;
;; It will rarely stay by one  rule.  Other rule engines will probably
;; want  to  get all  rule  definitions  at  once  in order  to  apply
;; optimizations  when transforming  them to  code.  We  are not  that
;; sophisticated and  "compile" rules  one by one  here.  But  this is
;; likely better suited to become a part of the public interface.
;;
;; DEPRECATED: Normally a  rule consists of variable  vector :find, an
;; expression :then  and a condition/query  :when. However we  have to
;; still handle a  special case of the  "universal" or "unconditional"
;; facts free of  variables conditions --- a rule with  just one form,
;; the fact-valued expression :then itself. There is still a test case
;; for that.  The working horse "compile-legacy" is supposed to handle
;; this case for a while.
;;
;; N-ary ready, see (fn [& args#] ...) in the code.
(defn- compile-rules [rules]
  ;; FIXME: use compile-rule directly once legacy is gone:
  (let [fs (map compile-legacy rules)]
    `(fn [& args#]
       (into #{} cat (for [f# [~@fs]]
                       (apply f# args#))))))

(defmacro defrules [& rules]
  (compile-rules rules))

;; C-u C-x C-e if you want to see the expansion:
(comment
  (macroexpand
   '(defrules
      {:find [?a ?b], :then [[?a ?b]], :when [[?a :is ?b]]}
      {:find [?x ?y], :then [[?y ?x]], :when [[?x :is ?y]]})))

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
  (let [rules (slurp-edn source)
        code `(defrules ~@rules)]
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
