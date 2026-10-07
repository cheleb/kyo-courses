package course.advanced.l01_pending_internals

import kyo.*

/** # A1 — How `A < S` works
  *
  * The Advanced part opens the kernel (`kyo.kernel`). This lesson builds the mental model the next ones rely on.
  *
  * ## 1. Representation
  *
  * `A < S` is an *opaque type* over a three-arm union (see `kyo/kernel/Pending.scala`):
  * {{{
  * opaque type <[+A, -S] = A | Pending[A, S] | Nested[A]
  * }}}
  *   - a raw `A`: a plain value *is* a computation, with no wrapper and no allocation. That is why lifting is free
  *     and why `42: Int < Any` costs nothing;
  *   - a `Pending` node: a suspended step. `Defer` nodes reify "apply this continuation to that value" (what `map`
  *     builds); `Suspend` nodes are *effect requests* (`Abort.fail`, `Var.get`...) carrying the effect's `Tag` and a
  *     continuation, waiting for a handler;
  *   - `Nested`: a wrapper used when a computation is itself the value of another computation (`A < S < S2`), so the
  *     two levels cannot be confused. `flatten` merges them.
  *
  * ## 2. Handlers and the evaluation loop
  *
  * Running a program means walking these nodes. A handler (`Abort.run`, `Var.run`...) looks for `Suspend` nodes whose
  * `Tag` matches its effect, answers them, and resumes the continuation; other requests are passed outwards. Once no
  * effect remains (`A < Any`), `.eval` runs the remaining `Defer` steps in a loop. Because each step is a *node* the
  * loop unfolds rather than a nested call, deep recursion through `map` does not overflow the JVM stack.
  *
  * `Sync` is not even a real effect: it is an opaque alias of `Abort[Nothing]`. Side effects are simply *deferred*
  * nodes, and the type system guarantees that only `.eval` / the unsafe runners execute them.
  *
  * ## 3. `Frame`: where am I?
  *
  * Almost every Kyo method takes a `using Frame`. A `Frame` is materialized *at compile time* by a macro and records
  * the call site: file, line, enclosing method and a code snippet. Kyo uses it for logs (I8 showed
  * `[Lesson.scala:83:44]`), effect traces in error reports, and debugging. A helper that takes `(using Frame)` reports
  * its *caller's* location.
  *
  * ## 4. `Tag`: runtime type identity
  *
  * Handlers must recognise their effect at runtime, despite erasure. `Tag[A]` is a compile-time-generated runtime
  * representation of `A`, supporting `=:=` and `<:<` (subtyping, including variance). `Tag[Var[Int]]` and
  * `Tag[Var[String]]` differ, which is why two `Var`s of different types never mix (B6). Generic code that suspends or
  * handles an effect parameterised by a type variable needs a `Tag` for it — hence `def f[V: Tag]` for streams (I3).
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.advanced.l01_pending_internals.InternalsLesson"
  * }}}
  */
object InternalsLesson extends KyoApp:

  // 1. A pure computation is the value itself.
  val text: String         = new String("hello")
  val pure: String < Any   = text
  val sameObject: Boolean  = pure.asInstanceOf[AnyRef] eq text

  // A pending computation is a node; its toString reveals the structure.
  val suspended: Int < Abort[String] = Abort.fail("boom")
  val deferred: Int < Sync           = Sync.defer(1).map(_ + 1)

  // Nested computations, flattened.
  // `Kyo.lift` makes the inner computation the *value* of the outer one, instead of chaining it.
  val inner: Int < Sync         = Sync.defer(41).map(_ + 1)
  val nested: Int < Sync < Sync = Kyo.lift(inner)
  val flat: Int < Sync          = nested.flatten

  // 2. Deep recursion through `map` is stack-safe.
  def countDown(n: Int): Int < Sync =
    if n == 0 then 0 else Sync.defer(countDown(n - 1)).map(_ + 1)

  // A pipeline of handlers, applied left to right.
  val program: Int < (Env[Int] & Abort[String]) =
    Env.get[Int].map(n => if n < 0 then Abort.fail("negative") else n * 2)

  val handled: Result[String, Int] = program.handle(Env.run(21), Abort.run, _.eval)

  // 3. Frames: a helper reporting its caller's position.
  def whereAmI(using frame: Frame): String = s"${frame.position.fileName}:${frame.position.lineNumber}"

  // 4. Tags.
  val tagChecks = List(
    s"Tag[Var[Int]] =:= Tag[Var[Int]]: ${Tag[Var[Int]] =:= Tag[Var[Int]]}",
    s"Tag[Var[Int]] =:= Tag[Var[String]]: ${Tag[Var[Int]] =:= Tag[Var[String]]}",
    s"Tag[Int] <:< Tag[AnyVal]: ${Tag[Int] <:< Tag[AnyVal]}",
    s"Tag[Abort[Throwable]] <:< Tag[Abort[Exception]]: ${Tag[Abort[Throwable]] <:< Tag[Abort[Exception]]} (Abort is contravariant)",
    s"Tag[List[String]].show: ${Tag[List[String]].show}"
  )

  run:
    for
      _ <- Console.printLine(s"pure value is the very same object: $sameObject")
      _ <- Console.printLine(s"suspended: $suspended")
      _ <- Console.printLine(s"deferred:  $deferred")
      f <- flat
      _ <- Console.printLine(s"flattened: $f")
      c <- countDown(1_000_000)
      _ <- Console.printLine(s"countDown(1_000_000) = $c, no StackOverflowError")
      _ <- Console.printLine(s"handled pipeline: ${handled.show}")
      _ <- Console.printLine(s"called from ${whereAmI}")
      _ <- Kyo.foreachDiscard(tagChecks)(Console.printLine(_))
    yield ()

end InternalsLesson
