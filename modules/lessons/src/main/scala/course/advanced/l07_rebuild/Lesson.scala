package course.advanced.l07_rebuild

import kyo.*
import kyo.kernel.*

// ----- Effects you rebuild in the exercises -----

/** A minimal `Abort`: fails with an `E`. The output is `Const[Unit]`; the operation maps it to `Nothing` (A3). */
sealed trait MiniAbort[E] extends ArrowEffect[Const[E], Const[Unit]]

/** The operations of a minimal `Var`. */
enum VarOp[V]:
  case Get()
  case Set(value: V)
  case Update(f: V => V)

/** A minimal `Var`: every operation answers with a `V`. */
sealed trait MiniVar[V] extends ArrowEffect[Const[VarOp[V]], Const[V]]

// ----- Effects rebuilt in this lesson -----

/** A minimal `Emit`, rebuilt below. */
sealed trait MiniEmit[V] extends ArrowEffect[Const[V], Const[Unit]]

object MiniEmit:
  def value[V](v: V)(using Tag[MiniEmit[V]], Frame): Unit < MiniEmit[V] =
    ArrowEffect.suspend[Any](Tag[MiniEmit[V]], v)

  def run[V, A, S](v: A < (MiniEmit[V] & S))(using Tag[MiniEmit[V]], Frame): (Chunk[V], A) < S =
    ArrowEffect.handleLoopState(Tag[MiniEmit[V]], Chunk.empty[V], v)(
      [C] => (emitted, value) => Loop.continue(emitted.append(value), ()),
      done = (emitted, a) => (emitted, a)
    )
end MiniEmit

/** A minimal `Env`, rebuilt below on `ContextEffect`. */
sealed trait MiniEnv[R] extends ContextEffect[R]

object MiniEnv:
  def get[R](using Tag[MiniEnv[R]], Frame): R < MiniEnv[R] = ContextEffect.suspend(Tag[MiniEnv[R]])

  def run[R, A, S](value: R)(v: A < (MiniEnv[R] & S))(using Tag[MiniEnv[R]], Frame): A < S =
    ContextEffect.handleInheritable(Tag[MiniEnv[R]], value)(v)
end MiniEnv

/** # A7 — Rebuilding the basics
  *
  * Everything in the Beginner part is a library built on the two kernel primitives of A2–A4. This lesson removes the
  * magic by rebuilding `Emit` and `Env`; in the exercises you rebuild `Abort` and `Var`, then check that your versions
  * behave like the real ones, handler-order semantics included (I1).
  *
  * Recipe for an effect:
  *   1. pick its *shape*: `ArrowEffect[Input, Output]` for operations, `ContextEffect[A]` for a bound value;
  *   2. make it *generic* when it carries user types (`MiniEmit[V]`): every operation and handler then takes a
  *      `Tag[MiniEmit[V]]`, which the compiler derives at the call site where `V` is known (A1);
  *   3. write the *operations* with `suspend` / `suspendWith`;
  *   4. write the *handlers*, choosing the family that fits (A3): state → `handleLoopState`; early exit →
  *      `handleLoop` with `Loop.done`; bound value → `handleInheritable`.
  *
  * What the real implementations add on top: variance (`Abort[-E]` is contravariant, so `Abort[Nothing]` fits
  * everywhere), `Result` with panics and exception catching, `Isolate` instances for fibers (A5), and many
  * convenience methods.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.advanced.l07_rebuild.RebuildLesson"
  * }}}
  */
object RebuildLesson extends KyoApp:

  // The same program written against the real effects and against the rebuilt ones.
  val real: Int < (Emit[String] & Env[Int]) =
    for
      n <- Env.get[Int]
      _ <- Emit.value(s"got $n")
      _ <- Emit.value("doubling")
    yield n * 2

  val rebuilt: Int < (MiniEmit[String] & MiniEnv[Int]) =
    for
      n <- MiniEnv.get[Int]
      _ <- MiniEmit.value(s"got $n")
      _ <- MiniEmit.value("doubling")
    yield n * 2

  run:
    for
      _ <- Console.printLine(s"real:    ${Emit.run(Env.run(21)(real)).eval}")
      _ <- Console.printLine(s"rebuilt: ${MiniEmit.run(MiniEnv.run(21)(rebuilt)).eval}")
      _ <- Console.printLine(s"rebuilt, unhandled: $rebuilt")
    yield ()

end RebuildLesson
