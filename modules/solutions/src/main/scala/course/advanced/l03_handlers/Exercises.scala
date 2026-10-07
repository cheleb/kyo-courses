package course.advanced.l03_handlers

import kyo.*
import kyo.kernel.*

/** A3 solutions. */
object Exercises:

  def runTell[A, S](v: A < (Tell & S))(using Frame): (List[String], A) < S =
    ArrowEffect.handleLoopState(Tag[Tell], List.empty[String], v)(
      [C] => (said, message) => Loop.continue(message :: said, ()),
      done = (said, a) => (said.reverse, a)
    )

  def runFresh[A, S](start: Int)(v: A < (Fresh & S))(using Frame): A < S =
    ArrowEffect.handleLoopState(Tag[Fresh], start, v)([C] => (next, _) => Loop.continue(next + 1, next))

  def runFreshCounted[A, S](v: A < (Fresh & S))(using Frame): (Int, A) < S =
    ArrowEffect.handleLoopState(Tag[Fresh], 0, v)(
      [C] => (next, _) => Loop.continue(next + 1, next),
      done = (issued, a) => (issued, a)
    )

  def runStop[A, S](v: A < (Stop & S))(using Frame): Either[String, A] < S =
    ArrowEffect.handleLoop(Tag[Stop], v)(
      [C] => reason => Loop.done(Left(reason)),
      done = a => Right(a)
    )

  def runFlipAll[A, S](v: A < (Flip & S))(using Frame): List[A] < S =
    ArrowEffect.handleContRepeated(Tag[Flip], v.map(List(_)))(
      [C] => (_, cont) => cont(true).map(heads => cont(false).map(tails => heads ++ tails))
    )

  def logPerBranch[A](v: A < (Tell & Flip))(using Frame): List[(List[String], A)] =
    runFlipAll(runTell(v)).eval

  def sharedLog[A](v: A < (Tell & Flip))(using Frame): (List[String], List[A]) =
    runTell(runFlipAll(v)).eval

end Exercises
