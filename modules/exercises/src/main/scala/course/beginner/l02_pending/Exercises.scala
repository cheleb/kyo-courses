package course.beginner.l02_pending

import kyo.*

/** B2 exercises — replace every `???`, then run `sbt "exercises/testOnly course.beginner.l02_pending.*"`. */
object Exercises:

  /** Ex 1: a computation with no effect that produces `42`. */
  def answer: Int < Any = 42

  /** Ex 2: double the result of `v` with `map`. */
  def double(v: Int < Any): Int < Any = v.map(_ * 2)

  /** Ex 3: add the results of `a` and `b` using a for-comprehension. */
  def sum(a: Int < Sync, b: Int < Sync): Int < Sync = 
    for 
     x <- a
     y <- b
    yield x + y

  /** Ex 4: `v` has no pending effect: extract its value. */
  def evaluate(v: Int < Any): Int = v.eval

  /** Ex 5: make `v` fit a wider effect set. (Hint: the answer is very short.) */
  def widen(v: Int < Sync): Int < (Sync & Abort[String]) = v

  /** Ex 6: run `v` twice and return both results, first run first. */
  def twice[A](v: A < Sync): (A, A) < Sync = 
    for
      a <- v
      b <- v
    yield (a, b)

  /** Ex 7: run every computation in order and add up their results. Hint: `Kyo.foldLeft` or `Kyo.collectAll`. */
  def sumAll(vs: Seq[Int < Sync]): Int < Sync = 
    Kyo.foldLeft(vs)(0)((acc, v) => v.map(acc + _ ))

end Exercises
