# Simulate Rule-Engine with Datalog

To keep thing simple and see where  it leads us we postulate here that
a  rule or  a rule  set  is just  a  function of  facts producing  new
facts. No more, no less.

See [Datascript](https://github.com/tonsky/datascript) dialect of
Datalog.

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
