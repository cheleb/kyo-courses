package course.intermediate.l03_stream

import kyo.*

/** I3 exercises — replace every `???`, then run `sbt "exercises/testOnly course.intermediate.l03_stream.*"`. */
object Exercises:

  /** Ex 1: a stream of the given numbers. */
  def fromList(xs: Seq[Int]): Stream[Int, Any] = ???

  /** Ex 2: the first `n` even numbers, starting at 0. Start from an infinite (or huge) stream and use `take`. */
  def evens(n: Int): Stream[Int, Any] = ???

  /** Ex 3: the infinite Fibonacci stream: 0, 1, 1, 2, 3, 5, 8, ... Hint: `Stream.unfold` with a pair as state. */
  def fibonacci: Stream[Long, Any] = ???

  /** Ex 4: the number of words in all lines (words are separated by whitespace; ignore empty words). */
  def wordCount(lines: Stream[String, Any]): Int < Any = ???

  /** Ex 5: parse every line as an `Int`, failing with `"not a number: <line>"` on the first invalid one. */
  def parseAll(lines: Stream[String, Any]): Stream[Int, Abort[String]] = ???

  /** Ex 6: parse only the first `n` lines; invalid lines *after* them must not cause a failure.
    *
    * Watch out: laziness works per chunk (see the lesson).
    */
  def parseFirst(n: Int)(lines: Seq[String]): Result[String, Seq[Int]] = ???

  /** Ex 7: the first value of `s` strictly greater than `threshold`, if any. `s` may be infinite. */
  def firstOver(threshold: Int)(s: Stream[Int, Any]): Maybe[Int] < Any = ???

  /** Ex 8: a custom operator: the running total of `s`. `Seq(1, 2, 3, 4)` gives `1, 3, 6, 10`.
    *
    * Hint: a stream is an emitting computation. Build one with `Stream(...)`: consume `s` with `foreach`, keep the
    * total in a `Var[Int]`, and `Emit.value(Chunk(total))` after each value.
    */
  def runningTotal(s: Stream[Int, Any]): Stream[Int, Any] = ???

end Exercises
