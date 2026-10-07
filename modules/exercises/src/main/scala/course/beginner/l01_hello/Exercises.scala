package course.beginner.l01_hello

import kyo.*

/** B1 exercises — replace every `???`, then run `sbt "exercises/testOnly course.beginner.l01_hello.*"`. */
object Exercises:

  /** Ex 1: print exactly `Hello, Kyo!` followed by a new line. */
  def hello: Unit < Sync = ???

  /** Ex 2: print `Hello, <name>!`, e.g. `Hello, Alice!`. */
  def greet(name: String): Unit < Sync = ???

  /** Ex 3: print `Hello` then `Kyo`, each on its own line. Use a for-comprehension. */
  def twoLines: Unit < Sync = ???

  /** Ex 4: greet every name, in order, reusing `greet`. Hint: `Kyo.foreachDiscard`. */
  def greetAll(names: Seq[String]): Unit < Sync = ???

end Exercises
