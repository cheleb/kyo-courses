package course.beginner.l01_hello

import kyo.*

/** B1 solutions. */
object Exercises:

  def hello: Unit < Sync = Console.printLine("Hello, Kyo!")

  def greet(name: String): Unit < Sync = Console.printLine(s"Hello, $name!")

  def twoLines: Unit < Sync =
    for
      _ <- Console.printLine("Hello")
      _ <- Console.printLine("Kyo")
    yield ()

  def greetAll(names: Seq[String]): Unit < Sync = Kyo.foreachDiscard(names)(greet)

end Exercises
