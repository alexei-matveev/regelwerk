# Simulate Rule-Engine with Datalog

See [Datascript](https://github.com/tonsky/datascript) dialect of
Datalog.

Usage:

    $ lein run resources/db.edn resources/rules.edn resources/query.edn

## Links

* [Souffle](https://souffle-lang.github.io/simple) Lang uses plain
  *.facts for input- and output relations.
* [Formulog](https://github.com/HarvardPL/formulog) delivers an
  executable JAR that transforms program "text" to fact database
  "text" bei executing Rules
* [Crepe](https://crates.io/crates/crepe) DSL in Rust that
  Acknowledges Souffle & Datalog for inspiration.

## License

Copyright © 2020 Alexei Matveev <alexei.matveev@gmail.com>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
