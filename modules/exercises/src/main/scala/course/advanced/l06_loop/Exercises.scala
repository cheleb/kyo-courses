package course.advanced.l06_loop

import kyo.*
import kyo.kernel.*

/** A6 exercises — replace every `???`, then run `sbt "exercises/testOnly course.advanced.l06_loop.*"`. */
object Exercises:

  /** Ex 1: the sum of the squares `1² + 2² + ... + n²`, with `Loop` carrying two values (no tuple, no recursion). */
  def sumOfSquares(n: Int): Long < Any = ???

  /** Ex 2: the `n`-th Fibonacci number (`fib(0) = 0`, `fib(1) = 1`), with `Loop` carrying three values. */
  def fib(n: Int): BigInt < Any = ???

  /** Ex 3: the number of Collatz steps from `n` down to 1 (`n / 2` if even, `3n + 1` otherwise). Hint:
    * `Loop.indexed`, carrying the current value.
    */
  def collatzSteps(n: Long): Int < Any = ???

  /** Ex 4: while the `Var` is positive, emit its value and decrement it. Hint: `Loop.whileTrue`. */
  def drain: Unit < (Var[Int] & Emit[Int]) = ???

  /** Ex 5: run the computations in order and return the first result above `threshold`. Computations after the match
    * must *not* run.
    */
  def firstAbove(threshold: Int)(xs: Seq[Int < Sync]): Maybe[Int] < Sync = ???

  /** Ex 6: a pipeline `String => Int`: trim, parse (`"not a number: <s>"`), require it even (`"odd: <n>"`), and
    * halve it. Build it by chaining arrows.
    */
  def pipeline: Arrow[String, Int, Abort[String]] = ???

  /** Ex 7: the factorial as a recursive arrow. It must handle `factorial(5000)` without overflowing the stack. */
  def factorial: Arrow[Int, BigInt, Any] = ???

end Exercises
