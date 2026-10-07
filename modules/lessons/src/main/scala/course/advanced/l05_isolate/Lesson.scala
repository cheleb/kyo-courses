package course.advanced.l05_isolate

import kyo.*
import kyo.kernel.*

/** # A5 — Effects across fibers: `Isolate`
  *
  * I4 noted that `Async.foreach` refuses a computation using `Var`. Here is why, and what to do about it.
  *
  * A `Var` is not a shared mutable cell: its state is threaded by a handler through *one* sequential computation.
  * When that computation forks, every fiber would need the state at once. Kyo refuses to guess, and asks for an
  * `Isolate`: a policy in three phases (see `kyo/kernel/Isolate.scala`):
  *   1. *capture* the current state before forking;
  *   2. run each fork *isolated*, on its own copy of that state;
  *   3. *restore*: decide how the forks' final states come back.
  *
  * Without a policy, the compiler stops with "This operation requires isolation for effects: kyo.Var[...]".
  * Context effects (`Env`, `Local`...) need nothing special: their value is simply copied (A4).
  *
  * Policies for `Var[V]`, applied with `.use { parallel operation }`:
  *   - `Var.isolate.update[V]`: set the state to a fork's final value. With several forks, each one is restored in
  *     turn, *in input order*, so the last fork wins and the others' changes are lost;
  *   - `Var.isolate.merge[V](f)`: combine `f(current, forkFinal)` for each fork. Careful: `forkFinal` includes the
  *     captured starting value, so merging counters with `_ + _` counts the start once per fork;
  *   - `Var.isolate.discard[V]`: forks may use the state, but their changes are dropped.
  *
  * For `Emit[V]`: `Emit.isolate.merge[V]` re-emits every fork's values (in input order), `discard` drops them.
  * Policies compose with `andThen`: `Var.isolate.update[Int].andThen(Emit.isolate.merge[String])`.
  *
  * Rule of thumb: to *share* mutable state between concurrent fibers, use an atomic (I5); use `Var` + `Isolate` when
  * each task has its own local state and you need a principled way to reconcile it.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.advanced.l05_isolate.IsolateLesson"
  * }}}
  */
object IsolateLesson extends KyoApp:

  /** Adds `n` to the state after a delay (task 3 finishes first, task 1 last). */
  def add(n: Int): Unit < (Var[Int] & Async) =
    Async.sleep((40 - n * 10).millis).andThen(Var.updateDiscard[Int](_ + n))

  val tasks: Seq[Unit < (Var[Int] & Async)] = (1 to 3).map(add)

  // Does not compile: "This operation requires isolation for effects: kyo.Var[scala.Int]".
  // val noPolicy = Async.foreachDiscard(tasks)(identity)

  val update: Int < (Var[Int] & Async)  = Var.isolate.update[Int].use(Async.foreachDiscard(tasks)(identity)).andThen(Var.get[Int])
  val naive: Int < (Var[Int] & Async)   = Var.isolate.merge[Int](_ + _).use(Async.foreachDiscard(tasks)(identity)).andThen(Var.get[Int])
  val discard: Int < (Var[Int] & Async) = Var.isolate.discard[Int].use(Async.foreachDiscard(tasks)(identity)).andThen(Var.get[Int])

  // A correct merge for counters adds each fork's *delta* from the captured start.
  val deltas: Int < (Var[Int] & Async) =
    Var.get[Int].map: start =>
      Var.isolate.merge[Int]((current, fork) => current + (fork - start))
        .use(Async.foreachDiscard(tasks)(identity))
        .andThen(Var.get[Int])

  // Emit: every fork's values are kept, in input order (not completion order).
  val emitted: Chunk[String] < Async =
    Emit.run[String](
      Emit.isolate.merge[String].use:
        Async.foreachDiscard(1 to 3)(n => Async.sleep((40 - n * 10).millis).andThen(Emit.value(s"from task $n")))
    ).map(_._1)

  run:
    for
      u <- Var.run(100)(update)
      _ <- Console.printLine(s"start 100, tasks add 1, 2, 3 in parallel")
      _ <- Console.printLine(s"  update:             $u  (the last fork wins)")
      n <- Var.run(100)(naive)
      _ <- Console.printLine(s"  merge(_ + _):       $n  (100 counted once per fork)")
      d <- Var.run(100)(deltas)
      _ <- Console.printLine(s"  merge with deltas:  $d")
      x <- Var.run(100)(discard)
      _ <- Console.printLine(s"  discard:            $x")
      e <- emitted
      _ <- Console.printLine(s"  Emit.isolate.merge: $e")
    yield ()

end IsolateLesson
