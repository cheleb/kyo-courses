package course.intermediate.l08_resilience

import kyo.*

/** I8 solutions. */
object Exercises:

  def delays(schedule: Schedule, n: Int): List[Duration] =
    Iterator.unfold(schedule)(s => s.next(Instant.Epoch).toOption).take(n).toList

  def backoff: Schedule = Schedule.exponentialBackoff(10.millis, 2, 80.millis).take(5)

  private val threeQuickRetries = Schedule.fixed(10.millis).take(3)

  def retryTransient[A](op: A < (Sync & Abort[Transient])): A < (Async & Abort[Transient]) =
    Retry[Transient](threeQuickRetries)(op)

  def retryOnlyTransient[A](op: A < (Sync & Abort[Transient | Fatal])): A < (Async & Abort[Transient | Fatal]) =
    Retry[Transient](threeQuickRetries)(op)

  def logged[A](name: String)(op: A < (Async & Abort[String])): A < (Async & Abort[String]) =
    for
      _ <- Log.info(s"$name: start")
      a <- Abort.recover[String](error => Log.error(s"$name: failed: $error").andThen(Abort.fail(error)))(op)
      _ <- Log.info(s"$name: done")
    yield a

  def retryLogged[A](op: A < (Sync & Abort[Transient])): A < (Async & Abort[Transient]) =
    Retry[Transient](threeQuickRetries):
      Abort.recover[Transient](e => Log.warn(s"attempt failed: ${e.message}").andThen(Abort.fail(e)))(op)

end Exercises
