package course.intermediate.l08_resilience

import course.KyoSuite
import java.util.concurrent.atomic.AtomicInteger
import kyo.*
import Exercises.*

class L08Spec extends KyoSuite:

  /** An operation that fails with `error` for the first `failures` attempts, then returns the attempt number. */
  def failing[E](attempts: AtomicInteger, failures: Int, error: Int => E): Int < (Sync & Abort[E]) =
    Sync.defer(attempts.incrementAndGet()).map(n => if n <= failures then Abort.fail(error(n)) else n)

  /** Runs `v` with a recording logger and returns its result with the log entries. */
  def withLog[A](v: A < (Async & Abort[Any])): (Result[Any, A], List[String]) =
    val log = RecordingLog()
    runKyo:
      for
        result  <- Abort.run[Any](Log.let(log.safe)(v))
        _       <- Log.flush
        entries <- log.entries
      yield (result, entries)

  test("Ex 1: delays"):
    assertEquals(delays(Schedule.fixed(1.second).take(2), 5), List(1.second, 1.second))
    assertEquals(delays(Schedule.exponential(100.millis, 3), 3), List(100.millis, 300.millis, 900.millis))
    assertEquals(delays(Schedule.done, 3), Nil)

  test("Ex 2: backoff"):
    assertEquals(delays(backoff, 10), List(10.millis, 20.millis, 40.millis, 80.millis, 80.millis))

  test("Ex 3: retryTransient succeeds after transient failures"):
    val attempts = AtomicInteger()
    assertEquals(runKyo(retryTransient(failing(attempts, 2, n => Transient(s"#$n")))), 3)

  test("Ex 3: retryTransient gives up after 3 retries"):
    val attempts = AtomicInteger()
    val result   = runKyo(Abort.run(retryTransient(failing(attempts, 10, n => Transient(s"#$n")))))
    assertEquals(result, Result.fail(Transient("#4")))
    assertEquals(attempts.get, 4)

  test("Ex 4: retryOnlyTransient retries transient failures"):
    val attempts = AtomicInteger()
    val op       = failing[Transient | Fatal](attempts, 2, n => Transient(s"#$n"))
    assertEquals(runKyo(retryOnlyTransient(op)), 3)

  test("Ex 4: retryOnlyTransient does not retry fatal failures"):
    val attempts = AtomicInteger()
    val op       = failing[Transient | Fatal](attempts, 10, n => Fatal(s"#$n"))
    assertEquals(runKyo(Abort.run[Transient | Fatal](retryOnlyTransient(op))), Result.fail(Fatal("#1")))
    assertEquals(attempts.get, 1)

  test("Ex 5: logged on success"):
    assertEquals(withLog(logged("job")(42)), (Result.succeed(42), List("info: job: start", "info: job: done")))

  test("Ex 5: logged on failure"):
    assertEquals(
      withLog(logged("job")(Abort.fail("boom"))),
      (Result.fail("boom"), List("info: job: start", "error: job: failed: boom"))
    )

  test("Ex 6: retryLogged logs every failed attempt"):
    val attempts = AtomicInteger()
    val (result, entries) = withLog(retryLogged(failing(attempts, 2, n => Transient(s"#$n"))))
    assertEquals(result, Result.succeed(3))
    assertEquals(entries, List("warn: attempt failed: #1", "warn: attempt failed: #2"))
