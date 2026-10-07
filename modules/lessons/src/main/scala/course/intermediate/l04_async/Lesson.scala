package course.intermediate.l04_async

import kyo.*

/** # I4 — `Async` and fibers
  *
  * `Async` is the effect of computations that may *wait*: sleeping, waiting for another fiber, for a queue... A
  * fiber is a lightweight thread managed by Kyo's scheduler: you can start millions of them.
  *
  * `Async` includes `Sync`, so an `A < Async` can do everything an `A < Sync` can. (B3 noted that a computation that
  * only declares `Sync` never blocks: `Async` in a signature is the signal that it may.)
  *
  * Key ideas of this lesson:
  *
  *   1. Waiting: `Async.sleep(d)`, `Async.delay(d)(v)`.
  *   2. Parallelism, high level: `Async.zip(a, b)`, `Async.gather(a, b, c)`, `Async.foreach(xs)(f)` run
  *      computations concurrently and wait for all of them.
  *   3. Racing: `Async.race` returns the first *success* and interrupts the others; `Async.raceFirst` returns the
  *      first result, success or failure.
  *   4. Timeouts: `Async.timeout(d)(v)` fails with `Abort[Timeout]` if `v` is too slow (and interrupts it).
  *   5. Fibers, low level: `Fiber.init(v)` starts `v` in the background and returns a `Fiber`; `fiber.get` waits for
  *      its result; `fiber.interrupt` stops it. `Fiber.init` requires `Scope`: when the scope closes, the fibers it
  *      started are interrupted, so none can leak.
  *
  * State effects such as `Var` cannot silently cross fiber boundaries: running `Async.foreach` over a computation
  * that uses `Var` requires telling Kyo how to merge the states (`Isolate`, see the Advanced part).
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.intermediate.l04_async.AsyncLesson"
  * }}}
  */
object AsyncLesson extends KyoApp:

  // A slow "remote call".
  def fetchUser(id: Int): String < Async =
    Async.sleep(100.millis).andThen(s"user-$id")

  def timed[A, S](label: String)(v: A < S): A < (S & Sync) =
    for
      watch   <- Clock.stopwatch
      result  <- v
      elapsed <- watch.elapsed
      _       <- Console.printLine(s"$label: $result in ~${elapsed.toMillis / 10 * 10} ms")
    yield result

  // 2. Sequential vs parallel.
  val sequential: Seq[String] < Async = Kyo.foreach(1 to 5)(fetchUser)
  val parallel: Seq[String] < Async   = Async.foreach(1 to 5)(fetchUser)

  val pair: (String, Int) < Async = Async.zip(fetchUser(1), Async.delay(50.millis)(42))

  // 3. Racing two mirrors: the fastest wins, the other is interrupted.
  def mirror(name: String, latency: Duration): String < Async = Async.sleep(latency).andThen(name)

  val fastest: String < Async = Async.race(mirror("eu", 80.millis), mirror("us", 30.millis))

  // `race` ignores failures as long as something succeeds; `raceFirst` takes whatever finishes first.
  val failsFast: String < (Async & Abort[String]) = Async.sleep(10.millis).andThen(Abort.fail("eu is down"))

  val raceIgnoresFailure: Result[String, String] < Async =
    Abort.run(Async.race(failsFast, mirror("us", 50.millis)))

  val raceFirstTakesFailure: Result[String, String] < Async =
    Abort.run(Async.raceFirst(failsFast, mirror("us", 50.millis)))

  // 4. Timeouts.
  val tooSlow: Result[Timeout, String] < Async = Abort.run[Timeout](Async.timeout(50.millis)(fetchUser(1)))

  // 5. A background fiber, started, then interrupted.
  def heartbeat(count: AtomicInt): Unit < Async =
    count.incrementAndGet.andThen(Async.sleep(20.millis)).andThen(heartbeat(count))

  val withHeartbeat: Int < (Async & Scope) =
    for
      count <- AtomicInt.init
      fiber <- Fiber.init(heartbeat(count))
      _     <- Async.sleep(100.millis)
      _     <- fiber.interrupt
      beats <- count.get
    yield beats

  run:
    for
      _ <- timed("sequential")(sequential)
      _ <- timed("parallel")(parallel)
      _ <- timed("zip")(pair)
      _ <- timed("race")(fastest)
      r <- raceIgnoresFailure
      _ <- Console.printLine(s"race with a failure: ${r.show}")
      f <- raceFirstTakesFailure
      _ <- Console.printLine(s"raceFirst with a failure: ${f.show}")
      t <- tooSlow
      _ <- Console.printLine(s"timeout: ${t.isFailure}")
      b <- withHeartbeat
      _ <- Console.printLine(s"heartbeats in 100 ms: $b")
    yield ()

end AsyncLesson
