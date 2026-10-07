package course.advanced.l07_rebuild

import kyo.*
import kyo.kernel.*

/** A7 solutions. */
object Exercises:

  def abortFail[E](error: E)(using Tag[MiniAbort[E]], Frame): Nothing < MiniAbort[E] =
    ArrowEffect.suspendWith[Any](Tag[MiniAbort[E]], error)(_ => throw IllegalStateException("a MiniAbort was resumed"))

  def abortRun[E, A, S](v: A < (MiniAbort[E] & S))(using Tag[MiniAbort[E]], Frame): Either[E, A] < S =
    ArrowEffect.handleLoop(Tag[MiniAbort[E]], v)(
      [C] => error => Loop.done(Left(error)),
      done = a => Right(a)
    )

  def abortRecover[E, A, S](onFail: E => A < S)(v: A < (MiniAbort[E] & S))(using Tag[MiniAbort[E]], Frame): A < S =
    abortRun(v).map:
      case Left(error) => onFail(error)
      case Right(a)    => a

  def varGet[V](using Tag[MiniVar[V]], Frame): V < MiniVar[V] =
    ArrowEffect.suspend[Any](Tag[MiniVar[V]], VarOp.Get())

  def varSet[V](value: V)(using Tag[MiniVar[V]], Frame): V < MiniVar[V] =
    ArrowEffect.suspend[Any](Tag[MiniVar[V]], VarOp.Set(value))

  def varUpdate[V](f: V => V)(using Tag[MiniVar[V]], Frame): V < MiniVar[V] =
    ArrowEffect.suspend[Any](Tag[MiniVar[V]], VarOp.Update(f))

  def varRunTuple[V, A, S](initial: V)(v: A < (MiniVar[V] & S))(using Tag[MiniVar[V]], Frame): (V, A) < S =
    ArrowEffect.handleLoopState(Tag[MiniVar[V]], initial, v)(
      [C] =>
        (state, op) =>
          op match
            case VarOp.Get()     => Loop.continue(state, state)
            case VarOp.Set(next) => Loop.continue(next, state)
            case VarOp.Update(f) =>
              val next = f(state)
              Loop.continue(next, next)
      ,
      done = (state, a) => (state, a)
    )

end Exercises
