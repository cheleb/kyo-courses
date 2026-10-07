package course.beginner.l02_pending

import kyo.*

/** B2 solutions. */
object Exercises:

  def answer: Int < Any = 42

  def double(v: Int < Any): Int < Any = v.map(_ * 2)

  def sum(a: Int < Sync, b: Int < Sync): Int < Sync =
    for
      x <- a
      y <- b
    yield x + y

  def evaluate(v: Int < Any): Int = v.eval

  def widen(v: Int < Sync): Int < (Sync & Abort[String]) = v

  def twice[A](v: A < Sync): (A, A) < Sync =
    for
      first  <- v
      second <- v
    yield (first, second)

  def sumAll(vs: Seq[Int < Sync]): Int < Sync =
    Kyo.foldLeft(vs)(0)((acc, v) => v.map(acc + _))

end Exercises
