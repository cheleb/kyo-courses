package course.beginner.l01_hello

import kyo.*

/** # B1 — Hello Kyo
  *
  * A Kyo program is a *value* that describes what to do. Nothing happens when you build it; it runs only when
  * something at the edge of the world executes it.
  *
  * The usual edge is `KyoApp`: every `run:` block registers a program, and the blocks are executed in order when
  * the application starts.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.beginner.l01_hello.HelloKyo"
  * }}}
  */
object HelloKyo extends KyoApp:

  // `Console.printLine` does not print anything by itself: it returns a description of type `Unit < Sync`.
  // `KyoApp` is what eventually executes it.
  run:
    Console.printLine("Hello, Kyo!")

  // Programs compose like any other value. A for-comprehension sequences them: the second line is printed
  // only after the first one.
  run:
    for
      _ <- Console.printLine("First line,")
      _ <- Console.printLine("then the second one.")
    yield ()

  // Because programs are values, they can be stored, passed to functions and reused.
  val greeting: Unit < Sync = Console.printLine("Values can be run more than once.")

  run:
    for
      _ <- greeting
      _ <- greeting
    yield ()

  // `Kyo.foreachDiscard` runs a program for every element of a collection and drops the results.
  run:
    Kyo.foreachDiscard(Seq("Alice", "Bob"))(name => Console.printLine(s"Hi $name"))

end HelloKyo
