package course.intermediate.l01_stacking

import kyo.*

/** I1 exercises — replace every `???`, then run `sbt "exercises/testOnly course.intermediate.l01_stacking.*"`. */
object Exercises:

  /** Ex 1: run `v` from `initial` so that a failure *discards* the state. */
  def stateLostOnFailure[A](initial: Int)(v: A < (Var[Int] & Abort[String])): Result[String, (Int, A)] = ???

  /** Ex 2: run `v` from `initial` so that the state is *kept* even when `v` fails. */
  def stateKeptOnFailure[A](initial: Int)(v: A < (Var[Int] & Abort[String])): (Int, Result[String, A]) = ???

  /** A step of a batch job: adds `n` to the running total, but fails with `"negative: <n>"` for negative numbers
    * (without touching the total).
    */
  def step(n: Int): Unit < (Var[Int] & Abort[String]) =
    for
      _ <- Abort.when(n < 0)(s"negative: $n")
      _ <- Var.updateDiscard[Int](_ + n)
    yield ()

  /** Ex 3: run `step` on every item, *skipping* the failing ones. Return the total and the error messages, in
    * order. Hint: handle `Abort` around each step.
    */
  def processAll(items: Seq[Int]): (Int, Seq[String]) = ???

  /** Ex 4: run `step` on every item, *stopping* at the first failure. Return the total accumulated so far and the
    * error, if any.
    */
  def processUntilError(items: Seq[Int]): (Int, Maybe[String]) = ???

  /** Ex 5: `v` can fail with two error types. Describe its outcome:
    *   - `"value <n>"` on success,
    *   - `"code <n>"` for an `Int` error,
    *   - `"message <s>"` for a `String` error.
    *
    * Hint: handle one error type, then the other; the results nest.
    */
  def classify(v: Int < Abort[String | Int]): String = ???

  /** Ex 6: make `v` atomic: if it fails, the state goes back to what it was before `v` started, and the failure
    * is propagated unchanged. On success, the changes made by `v` are kept.
    */
  def transaction[A](v: A < (Var[Int] & Abort[String])): A < (Var[Int] & Abort[String]) = ???

end Exercises
