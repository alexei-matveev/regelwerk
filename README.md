# Rules:: Facts -> Facts'

[![Clojars Project](https://img.shields.io/clojars/v/f0bec0d/regelwerk.svg)](https://clojars.org/f0bec0d/regelwerk)

To keep thing simple and see where it leads us we postulate here that
a rule or a rule set is just a function of facts producing new
facts. No more, no less.

Another   "weaker"    constraint   of   the   design    was   to   use
Datalog/Datascript Query  to produce  a "Result  Set" from  the "Input
Facts" and to  generate the "Output Facts" from the  "Result Set" in a
sufficiently  flexible  way.   The ``mapcat``-  aka  ``flatMap``-style
concatenation of the "Output  Facts" independently generated from each
row of the "Result Set" appeared then to be a natural choice.

## Example Usage

Note that  it does  not always  make sense  to mix/conflate  the input
facts and the  output facts, here illustrated by use  of two different
human languages:

```clojure
(let [facts [[1 :is "odd"]
             [2 :is "even"]]
      rules (defrules
              {:find [?number]
               ;; But only even numbers from the set of English
               ;; facts by Datalog query:
               :when [[?number :is "even"]]
               ;; For each number produce this set of facts in
               ;; German:
               :then [[?number :ist "gerade"]
                      [(inc ?number) :ist "ungerade"]
                      [(dec ?number) :ist "ungerade"]]})]
  (rules facts))
;; =>
#{[1 :ist "ungerade"]
  [2 :ist "gerade"]
  [3 :ist "ungerade"]})))
```

Still many rule  engines insist on "inserting" deduced  facts into the
very database that was "queried".

## Next Level: Lamda Calculus with Sets of Facts

Would such a calculus be useful or is it just mind gymnastics just to
[reinvent](https://en.wikipedia.org/wiki/Language_Integrated_Query)
[the](https://www.w3.org/TR/rdf-mt/#rules)
[wheel](https://en.wikipedia.org/wiki/Relational_algebra)? Here is an
example of Binary Rule that compares two datasets, `en` and `de`, to
deduce a translation between English and German:

```clojure
(let [en [[1 :is "odd"]
          [2 :is "even"]]
      de [[1 :ist "ungerade"]
          [2 :ist "gerade"]]
      tr (defrules
           {:from [$en $de]
            :find [?word ?wort]
            :when [[$en ?n :is ?word]
                   [$de ?n :ist ?wort]]
            :then [[?word :eqv ?wort]
                   [?wort :eqv ?word]]})]
  (tr en de))
;; =>
#{["odd" :eqv "ungerade"]
  ["gerade" :eqv "even"]
  ["ungerade" :eqv "odd"]
  ["even" :eqv "gerade"]}
```

Think of composable programs with "Rules" operating on "Sets of
Facts".  Think of N-ary Rules taking zero, one or more "Sets of Facts"
as arguments and producing even more of that as a result:

    Rules:: Facts1 -> Facts2 -> ... -> FactsN

Think of  `select ... from  facts1 join facts2  ...`.  Or is  a `join`
rather  bad analogy  because  it  is too  "cartesian"?   What kind  of
expressions besides  function application should be  allowed? In other
words,  what are  the built-ins?  Some set  operations like  union and
diff?   Joins?   A  plain  irreversible  union  that  trows  facts  of
different domains  in a  single pot/set  is probably  not a  good idea
type-wise.

## Build & Deploy to Clojars

First you may consider bumping the version in
``project.clj``. Remember that you cannot overwrite non-SNAPSHOT
versions and what gets published, stays public. You habe been warned!
With that said, here is the memo:

    $ lein clean
    $ lein jar
    $ lein test
    $ lein deploy clojars

The artifact goes to
[f0bec0d/regelwerk](https://clojars.org/f0bec0d/regelwerk) repository
so the user name will be ``f0bec0d``.  The passwort is one of the
Deploy Tokens generated at Clojars, likely stored in your KeePass.

## Links

* Maybe one shlould read [PAIP](https://github.com/norvig/paip-lisp)
  before even attempting ...
* The whole RDF Subject and
  [inference](https://jena.apache.org/documentation/inference/) is
  huge to swallow.
* See [Datascript](https://github.com/tonsky/datascript) dialect of
  Datalog.
* [Minikusari](https://github.com/frankiesardo/minikusari) in less two
  dosen lines of code generates transaction datoms (facts) from the
  result set of a Datascript query.
* [naga](https://github.com/quoll/naga) seems to be generating facts
  from rules.
* [Souffle](https://souffle-lang.github.io/simple) Lang uses plain
  *.facts for input- and output relations.
* [Formulog](https://github.com/HarvardPL/formulog) delivers an
  executable JAR that transforms program "text" to fact database
  "text" bei executing Rules
* [Crepe](https://crates.io/crates/crepe) DSL in Rust that
  Acknowledges Souffle & Datalog for inspiration.
* Functional Production
  [Rules](https://leanpub.com/readevalprintlove004/read#leanpub-auto-a-simple-implementation-of-a-purely-functional-production-rules-system)
* Another unifier-based query
  [engine](https://github.com/jimmyhmiller/one-hundred-lines-or-less/tree/master/libraries/clojure/query-engine/src/query_engine)
* [Meander](https://github.com/noprompt/meander)
* [Knowledge Machine](https://www.cs.utexas.edu/%7Emfkb/km.html) from
  around 1997 or before.

Maybe an interesting read:

* [Is Datalog a good language for
  authorization?](https://neilmadden.blog/2022/02/19/is-datalog-a-good-language-for-authorization/)
  and the HN
  [discussion](https://news.ycombinator.com/item?id=30400886)
* [Fixpoints for the Masses:
  Programming with First-Class Datalog Constraints](https://dl.acm.org/doi/pdf/10.1145/3428193) proposes composable Datalog programs as Data.

## ChangeLog

Version 0.0.3-SNAPSHOT:

* Rules involving more than one dataset: `{:from [$a $b], :when [[$a
  ...], [$b ...]], :then [...]}`.
* BREAKING: no more list-syntax for rules. Convert the lists to maps
  with `:find`, `:then`, and `:when` keys in that sequence.
* BREAKING: Singular form `(defrule vars expr where)` is no more
  supported. Use maps instead.
* BREAKING: `(comment ...)` forms in Rules are no more handled
  specially. Use Reader Comment `#_` instead.
* DEPRECATED: A rule that returns any facts derived from an empty
  dataset likely violates "empty graph lemma". Therefore the special
  case of `{:then [...]}` akin to `(constantly [...])` that returns
  facts "no matter what" must go. In the unlikely case that this edge
  case affects you, consider some seed- or ground facts in your
  dataset like `["version" :ge "0.0.3"]` or `["water" :is "wet"]`...

## License

Copyright © 2022 Alexei Matveev <alexei.matveev@gmail.com>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
