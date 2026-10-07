package course.advanced.l03_handlers

import kyo.*
import kyo.kernel.*

/** A3 exercises — replace every `???`, then run `sbt "exercises/testOnly course.advanced.l03_handlers.*"`.
  *
  * The effects `Tell`, `Fresh`, `Stop` and `Flip`, with their operations, are defined in the lesson. You write the
  * handlers.
  */
object Exercises:

  /** Ex 1: collect every message, in order, alongside the result. Hint: `handleLoopState` with a `done` arm (prepend
    * to a list, reverse at the end).
    */
  def runTell[A, S](v: A < (Tell & S))(using Frame): (List[String], A) < S = ???

  /** Ex 2: answer `Fresh.next` with `start`, `start + 1`, `start + 2`... */
  def runFresh[A, S](start: Int)(v: A < (Fresh & S))(using Frame): A < S = ???

  /** Ex 3: answer `Fresh.next` with 0, 1, 2... and also return how many numbers were issued. */
  def runFreshCounted[A, S](v: A < (Fresh & S))(using Frame): (Int, A) < S = ???

  /** Ex 4: `Left(reason)` if the program stops, `Right(result)` otherwise. Hint: `handleLoop`, `Loop.done`. */
  def runStop[A, S](v: A < (Stop & S))(using Frame): Either[String, A] < S = ???

  /** Ex 5: explore both sides of every `Flip` (`true` first) and collect every result.
    *
    * You need to resume twice: use `handleContRepeated`. (With `handleCont`, the test does not fail cleanly: it runs
    * away until `OutOfMemoryError`.)
    */
  def runFlipAll[A, S](v: A < (Flip & S))(using Frame): List[A] < S = ???

  /** Ex 6: handle `Tell` *inside* `Flip`: every branch gets its own log. Reuse your handlers. */
  def logPerBranch[A](v: A < (Tell & Flip))(using Frame): List[(List[String], A)] = ???

  /** Ex 7: handle `Tell` *outside* `Flip`: one log shared by all branches, in the order they ran. */
  def sharedLog[A](v: A < (Tell & Flip))(using Frame): (List[String], List[A]) = ???

end Exercises
