# Java Src

Sometimes you need a *.java class for a clojure app, just a simple class,
which cannot be generated in clojure (like one for -javaagent) - this will
help you to get one into the project

## Installation

Add to `~/.lein/project.clj`:

    :dependencies [[java-src "0.1.0"]]

## Usage

Assume you have `Some.java` and `Another.java` (not packaged classes)

Either do in the project REPL:

    (require '[java-src.core :refer [source-java]])
    (source-java "path/to/Some.java" "path/to/Another.java")

Or use the `java-src-0.1.0-SNAPSHOT-standalone.jar`:

    java -jar java-src-0.1.0-SNAPSHOT-standalone.jar path/to/Some.java path/to/Another.java

After that under `PROJECTNAME/repo` directory will appear a Maven repo, add to `project.clj`:

    :repositories {"local" "file:repo"}
    :dependencies [local/PROJECTNAME-java "0.0.1"]

Reload project REPL and use them:

    (import [Some Another])

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
