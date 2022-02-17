# Rules:: Facts -> Facts

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
               ;; For each number produce this set of facts set in
               ;; German:
               :then [[?number :ist "gerade"]
                      [(inc ?number) :ist "ungerade"]
                      [(dec ?number) :ist "ungerade"]]
               ;; Restrict to even numbers from the set of English
               ;; facts by Datalog query:
               :when [[?number :is "even"]]})]
  (rules facts))
;; =>
#{[1 :ist "ungerade"]
  [2 :ist "gerade"]
  [3 :ist "ungerade"]})))
```

Still many rule  engines insist on "inserting" deduced  facts into the
very database that was "queried".

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
[f0becod/regelwerk](https://clojars.org/f0bec0d/regelwerk) repository
so the user name will be ``f0bec0d``.  The passwort is one of the
Deploy Tokens generated at Clojars, likely stored in your KeePass.

## Links

* Maybe one shlould read [PAIP](https://github.com/norvig/paip-lisp)
  before even attempting ...
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

## License

Copyright © 2020 Alexei Matveev <alexei.matveev@gmail.com>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
