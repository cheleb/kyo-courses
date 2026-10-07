package course.advanced.l07_rebuild

import kyo.*
import kyo.kernel.*

/** A7 exercises — replace every `???`, then run `sbt "exercises/testOnly course.advanced.l07_rebuild.*"`.
  *
  * The effects `MiniAbort[E]` and `MiniVar[V]` (with `VarOp`) are declared in the lesson. Rebuild them.
  */
object Exercises:

  // ----- MiniAbort -----

  /** Ex 1: fail with `error`. The result type is `Nothing`: build it like `Stop` in A3 (`suspendWith`). */
  def abortFail[E](error: E)(using Tag[MiniAbort[E]], Frame): Nothing < MiniAbort[E] = ???

  /** Ex 2: handle `MiniAbort[E]`: `Left(error)` on failure (the rest of the program never runs), `Right(result)`
    * otherwise.
    */
  def abortRun[E, A, S](v: A < (MiniAbort[E] & S))(using Tag[MiniAbort[E]], Frame): Either[E, A] < S = ???

  /** Ex 3: recover from a failure with `onFail`. Reuse `abortRun`. */
  def abortRecover[E, A, S](onFail: E => A < S)(v: A < (MiniAbort[E] & S))(using Tag[MiniAbort[E]], Frame): A < S =
    ???

  // ----- MiniVar -----

  /** Ex 4: read the current state. */
  def varGet[V](using Tag[MiniVar[V]], Frame): V < MiniVar[V] = ???

  /** Ex 5: replace the state, returning the *previous* value (like `Var.set`). */
  def varSet[V](value: V)(using Tag[MiniVar[V]], Frame): V < MiniVar[V] = ???

  /** Ex 6: apply `f` to the state, returning the *new* value (like `Var.update`). */
  def varUpdate[V](f: V => V)(using Tag[MiniVar[V]], Frame): V < MiniVar[V] = ???

  /** Ex 7: handle `MiniVar[V]` from `initial`, returning the final state and the result. */
  def varRunTuple[V, A, S](initial: V)(v: A < (MiniVar[V] & S))(using Tag[MiniVar[V]], Frame): (V, A) < S = ???

end Exercises
