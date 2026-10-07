package course.intermediate.l08_resilience

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kyo.*
import scala.jdk.CollectionConverters.*

/** A `Log` backend for tests: records `"<level>: <message>"` instead of printing. Install it with
  * `Log.let(recording.safe)(program)`, and call `Log.flush` before reading `entries`: logging is asynchronous.
  */
final class RecordingLog extends Log.Unsafe:
  private val buffer = ConcurrentLinkedQueue[String]()

  def entries: List[String] < Sync = Sync.defer(buffer.asScala.toList)

  private def record(level: Log.Level, msg: String): Unit =
    val _ = buffer.add(s"$level: $msg")

  def level: Log.Level                 = Log.Level.trace
  def name: String                     = "recording"
  def withName(name: String): Log.Unsafe = this

  def trace(msg: => String)(using Frame, AllowUnsafe): Unit                 = record(Log.Level.trace, msg)
  def trace(msg: => String, t: => Throwable)(using Frame, AllowUnsafe): Unit = record(Log.Level.trace, msg)
  def debug(msg: => String)(using Frame, AllowUnsafe): Unit                 = record(Log.Level.debug, msg)
  def debug(msg: => String, t: => Throwable)(using Frame, AllowUnsafe): Unit = record(Log.Level.debug, msg)
  def info(msg: => String)(using Frame, AllowUnsafe): Unit                  = record(Log.Level.info, msg)
  def info(msg: => String, t: => Throwable)(using Frame, AllowUnsafe): Unit  = record(Log.Level.info, msg)
  def warn(msg: => String)(using Frame, AllowUnsafe): Unit                  = record(Log.Level.warn, msg)
  def warn(msg: => String, t: => Throwable)(using Frame, AllowUnsafe): Unit  = record(Log.Level.warn, msg)
  def error(msg: => String)(using Frame, AllowUnsafe): Unit                 = record(Log.Level.error, msg)
  def error(msg: => String, t: => Throwable)(using Frame, AllowUnsafe): Unit = record(Log.Level.error, msg)
end RecordingLog

/** Errors of a remote service, used by the exercises. */
final case class Transient(message: String) derives CanEqual
final case class Fatal(message: String) derives CanEqual

/** # I8 — Resilience: `Schedule`, `Retry`, `Log`
  *
  * Real systems fail transiently: a timeout, a busy server. This lesson shows how to *retry* with a policy, and how
  * to *log* what happens.
  *
  *   1. `Schedule` describes a sequence of delays: `Schedule.fixed(1.second)`, `Schedule.exponential(100.millis,
  *      2)`, `Schedule.exponentialBackoff(initial, factor, maxBackoff)`... and combinators: `take(n)` (at most n
  *      delays), `jitter(0.2)` (randomize ±20%), `maxDuration(d)`, `andThen(other)`, `min` / `max`. A schedule is a
  *      pure value: `schedule.next(now)` returns the next delay and the remaining schedule, or `Absent` when done.
  *   2. `Retry[E](schedule)(op)` runs `op` and, on a failure of type `E`, waits for the next delay and tries again;
  *      once the schedule is exhausted, the last failure is propagated. Errors of other types are *not* retried.
  *   3. `Clock.repeatWithDelay(d)(task)` / `Clock.repeatAtInterval(d)(task)` run a task periodically in a fiber.
  *   4. `Log.info(...)`, `Log.warn(...)`, `Log.error(...)`... log through the current logger, which can be replaced
  *      with `Log.let(logger)(program)` or `Log.withConsoleLogger(...)`. Two surprises: the default logger only
  *      shows `warn` and above, and logging is *asynchronous* (`Log.flush` waits until everything is written).
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.intermediate.l08_resilience.ResilienceLesson"
  * }}}
  */
object ResilienceLesson extends KyoApp:

  // 1. Inspecting schedules without waiting.
  def firstDelays(schedule: Schedule, n: Int): List[String] =
    Iterator
      .unfold(schedule)(s => s.next(Instant.Epoch).map((d, rest) => (d.show, rest)).toOption)
      .take(n)
      .toList

  val schedules = List(
    "fixed(1s).take(3)"                      -> Schedule.fixed(1.second).take(3),
    "exponential(100ms, 2)"                  -> Schedule.exponential(100.millis, 2),
    "exponentialBackoff(100ms, 2, max 1s)"   -> Schedule.exponentialBackoff(100.millis, 2, 1.second),
    "fixed(10ms).take(2) andThen fixed(1s)"  -> Schedule.fixed(10.millis).take(2).andThen(Schedule.fixed(1.second))
  )

  // 2. A flaky operation: fails twice, then succeeds.
  def flaky(attempts: AtomicInteger): String < (Sync & Abort[Transient]) =
    Sync.defer(attempts.incrementAndGet()).map: n =>
      if n < 3 then Abort.fail(Transient(s"attempt $n failed")) else s"succeeded on attempt $n"

  def retried(attempts: AtomicInteger): String < (Async & Abort[Transient]) =
    Retry[Transient](Schedule.fixed(10.millis).take(5)):
      Log.warn("calling the flaky service").andThen(flaky(attempts))

  // 3. A periodic task, stopped after a while.
  val ticks: Int < (Async & Scope) =
    for
      count <- AtomicInt.init
      fiber <- Clock.repeatWithDelay(20.millis)(count.incrementAndGet)
      _     <- Async.sleep(110.millis)
      _     <- fiber.interrupt
      n     <- count.get
    yield n

  run:
    for
      _ <- Kyo.foreachDiscard(schedules)((name, s) => Console.printLine(s"$name: ${firstDelays(s, 5).mkString(", ")}"))
      r <- Abort.run(retried(AtomicInteger()))
      _ <- Log.flush
      _ <- Console.printLine(s"retry: ${r.show}")
      n <- ticks
      _ <- Console.printLine(s"repeatWithDelay(20ms) for 110ms: $n ticks")
      _ <- Log.info("not shown: the default level is warn")
      _ <- Log.withConsoleLogger(Log.info("shown: withConsoleLogger defaults to debug"))
      _ <- Log.flush
    yield ()

end ResilienceLesson
