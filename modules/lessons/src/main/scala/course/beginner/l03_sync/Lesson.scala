package course.beginner.l03_sync

import kyo.*

/** # B3 — `Sync`: controlled side effects
  *
  * `Sync` marks a computation that performs side effects: printing, reading a mutable variable, calling a Java
  * API, reading the clock... Kyo never runs them while you *build* a program, only when the program is executed.
  *
  * Key ideas of this lesson:
  *
  *   1. `Sync.defer(expr)` wraps any side-effecting expression; `expr` runs each time the program runs, not before.
  *   2. Forgetting `Sync.defer` is a classic bug: plain values are lifted *eagerly*, so the side effect runs once,
  *      at construction time.
  *   3. Kyo ships safe, `Sync`-based services for the usual sources of non-determinism: `Clock` and `Random`
  *      (and `Console`, seen in B1).
  *   4. Those services can be swapped for deterministic versions: `Random.withSeed`, `Clock.withTimeControl`.
  *      This is what makes such code easy to test.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.beginner.l03_sync.SyncLesson"
  * }}}
  */
object SyncLesson extends KyoApp:

  // 1. A mutable counter, the kind of thing Sync is meant to protect.
  private var counter = 0

  // `Sync.defer` delays the increment until the program runs. Each run increments again.
  val next: Int < Sync =
    Sync.defer:
      counter += 1
      counter

  // 2. The bug: without `Sync.defer`, the block is evaluated right here, once, and its *result* is lifted.
  //    `eager` is just the constant computation `1` (or whatever `counter` was at that moment).
  val eager: Int < Sync =
    counter += 1
    counter

  // 3. Clock: the current time, as a Kyo `Instant`.
  val now: Instant < Sync = Clock.now

  // Measuring elapsed time with a stopwatch.
  def timed[A, S](v: A < S): (A, Duration) < (Sync & S) =
    for
      watch   <- Clock.stopwatch
      result  <- v
      elapsed <- watch.elapsed
    yield (result, elapsed)

  // 3. Random: every call is a new draw.
  val coin: String < Sync = Random.nextBoolean.map(if _ then "heads" else "tails")

  run:
    for
      a <- next
      b <- next
      _ <- Console.printLine(s"deferred counter: $a then $b") // 2 then 3: `eager` already bumped it to 1
      c <- eager
      d <- eager
      _ <- Console.printLine(s"eager counter: $c then $d")    // always the same value
      t <- now
      _ <- Console.printLine(s"now: ${t.show}")
      r <- timed(Kyo.fill(1000)(Random.nextInt))
      _ <- Console.printLine(s"1000 random ints in ${r._2.show}")
      x <- Kyo.fill(5)(coin)
      _ <- Console.printLine(s"coins: ${x.mkString(", ")}")
    yield ()

  // 4. Deterministic services: the same seed always gives the same draws.
  run:
    for
      a <- Random.withSeed(42)(Kyo.fill(5)(coin))
      b <- Random.withSeed(42)(Kyo.fill(5)(coin))
      _ <- Console.printLine(s"seeded draws are equal: ${a == b}")
    yield ()

  // 4. Controlled time: the clock starts at the epoch and only moves when told to.
  run:
    Clock.withTimeControl: control =>
      for
        t0 <- Clock.now
        _  <- control.advance(1.hour)
        t1 <- Clock.now
        _  <- Console.printLine(s"controlled clock: ${t0.show} -> ${t1.show}")
      yield ()

end SyncLesson
