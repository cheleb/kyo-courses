package course.intermediate.l02_choice_emit

import kyo.*

/** I2 exercises — replace every `???`, then run `sbt "exercises/testOnly course.intermediate.l02_choice_emit.*"`. */
object Exercises:

  // ----- Choice -----

  /** Ex 1: `"heads"` or `"tails"`, in that order. */
  def coin: String < Choice = ???

  /** Ex 2: every pair `(a, b)` with `1 <= a < b <= n`, ordered by `a` then `b`. */
  def pairs(n: Int): (Int, Int) < Choice = ???

  /** Ex 3: every Pythagorean triple `(a, b, c)` with `1 <= a <= b <= c <= n` and `a² + b² = c²`.
    * Hint: `Choice.dropIf`.
    */
  def pythagorean(n: Int): (Int, Int, Int) < Choice = ???

  /** Ex 4: every permutation of `xs` (assume distinct elements). Order: pick the first element in the order of
    * `xs`, then recursively permute the rest. E.g. `List(1, 2, 3)` gives `123, 132, 213, 231, 312, 321`.
    */
  def permutations(xs: List[Int]): List[Int] < Choice = ???

  /** Ex 5: the N-queens puzzle. Place `n` queens on an `n×n` board so that none attack each other. A solution is
    * the list of column indices (0-based), one per row, starting with the first row.
    *
    * Hint: place queens row by row, choosing a column with `Choice.evalSeq` and dropping the branches where the new
    * queen is attacked (same column or same diagonal as a previous one).
    */
  def queens(n: Int): List[Int] < Choice = ???

  // ----- Emit -----

  /** Ex 6: emit `n`, `n - 1`, ..., `1`. */
  def countdown(n: Int): Unit < Emit[Int] = ???

  /** Ex 7: the Collatz sequence from `n`: emit every value from `n` down to `1` included (next value: `n / 2` if
    * `n` is even, `3n + 1` otherwise). Return the number of steps, i.e. how many values were emitted minus one.
    */
  def collatz(n: Int): Int < Emit[Int] = ???

  /** Ex 8: run `v` and return the sum of everything it emitted, without collecting them. Hint: `Emit.runFold`. */
  def sumEmitted(v: Unit < Emit[Int]): Int = ???

  /** Ex 9: run `v` and return only the emitted values that are strictly greater than `threshold`, in order.
    * Hint: handle the emissions with `Emit.runForeach` and re-emit the selected ones.
    */
  def filterEmitted(threshold: Int)(v: Unit < Emit[Int]): Unit < Emit[Int] = ???

end Exercises
