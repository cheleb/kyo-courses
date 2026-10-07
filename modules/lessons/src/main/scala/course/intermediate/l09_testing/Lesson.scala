package course.intermediate.l09_testing

import java.io.IOException
import kyo.*

/** Code under test in this lesson and its exercises. */
object App:

  /** Waits `after`, then returns the message. */
  def remind(after: Duration, message: String): String < Async = Async.sleep(after).andThen(s"reminder: $message")

  /** Asks for a name on the console and greets it. */
  val greet: Unit < (Sync & Abort[IOException]) =
    for
      _    <- Console.printLine("name?")
      name <- Console.readLine
      _    <- Console.printLine(s"hello $name")
    yield ()

  /** Rolls `n` six-sided dice. */
  def roll(n: Int): Seq[Int] < Sync = Kyo.fill(n)(Random.nextInt(6).map(_ + 1))

  /** Fails on negative input. */
  def sqrt(x: Double): Double < Abort[String] =
    if x < 0 then Abort.fail(s"negative: $x") else math.sqrt(x)
end App

/** # I9 — Testing Kyo code
  *
  * Since B1, every spec of this course extends `course.KyoSuite` (in `modules/tests`), which runs Kyo programs with
  * `KyoApp.Unsafe.runAndBlock`. This lesson opens the box: running programs from plain test code, and making
  * effectful code deterministic.
  *
  *   1. Running. A program with no pending effect (`A < Any`) is run with `.eval`. Anything else needs its effects
  *      handled first, then an *unsafe* entry point: `Sync.Unsafe.evalOrThrow(v)` for `Sync` code,
  *      `KyoApp.Unsafe.runAndBlock(timeout)(v)` for `Async` code. Unsafe APIs require an `AllowUnsafe` permission:
  *      `import AllowUnsafe.embrace.danger` grants it, and should only appear at the edges (tests, `main`).
  *   2. Failures. Handle `Abort` with `Abort.run` and compare `Result`s: `Result.fail(e)`, `Result.succeed(a)`.
  *   3. Console. `Console.withIn(lines)(v)` feeds the input; `Console.withOut(v)` captures what is printed.
  *   4. Randomness. `Random.withSeed(seed)(v)` makes draws reproducible.
  *   5. Time. `Clock.withTimeControl(control => ...)` replaces the clock with a virtual one starting at the epoch:
  *      `control.advance(d)` moves it forward and wakes the sleepers whose time has come, so a one-hour sleep is
  *      tested instantly. Start the sleeping code in a fiber, wait until it actually sleeps
  *      (`control.awaitPendingSleepers(1)`), then advance. Advancing *before* the fiber sleeps would schedule the
  *      sleep relative to the new time, and it would never fire.
  *   6. Services. Replace dependencies with test doubles via `Env` / `Layer` (B5, I7) or `Log.let` (I8).
  *   7. Composition. Wrappers such as `Console.withOut`, `Random.withSeed` or `Clock.withTimeControl` return a new
  *      *program*; runners such as `evalOrThrow` *execute* one. Stack all the wrappers first, run once at the end:
  *      `run(Console.withOut(Console.withIn(lines)(program)))`.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.intermediate.l09_testing.TestingLesson"
  * }}}
  */
object TestingLesson:
  import AllowUnsafe.embrace.danger

  def main(args: Array[String]): Unit =
    // 1. Pure code: .eval. Sync code: evalOrThrow.
    println(s"sqrt(16) handled: ${Abort.run(App.sqrt(16)).eval.show}")
    println(s"sqrt(-1) handled: ${Abort.run(App.sqrt(-1)).eval.show}")
    println(s"seeded rolls: ${Sync.Unsafe.evalOrThrow(Random.withSeed(42)(App.roll(5)))}")

    // 3. Console in and out.
    val (out, _) = Sync.Unsafe.evalOrThrow(Console.withOut(Console.withIn(Seq("Ada"))(App.greet)))
    println(s"captured output: ${out.stdOut.replace("\n", "\\n")}")

    // 5. A one-hour reminder, tested in virtual time.
    val start = System.nanoTime()
    val reminder: String < Async =
      Scope.run:
        Clock.withTimeControl: control =>
          for
            fiber   <- Fiber.init(App.remind(1.hour, "stand up"))
            _       <- control.awaitPendingSleepers(1)
            early   <- control.advance(59.minutes).andThen(fiber.done)
            _       <- control.advance(1.minute)
            message <- fiber.get
          yield s"$message (done after 59 minutes: $early)"
    val result = KyoApp.Unsafe.runAndBlock(5.seconds)(reminder).getOrThrow
    println(s"$result, in ${(System.nanoTime() - start) / 1_000_000} ms of real time")
  end main

end TestingLesson
