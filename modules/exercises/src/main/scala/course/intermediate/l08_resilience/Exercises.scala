package course.intermediate.l08_resilience

import kyo.*

/** I8 exercises — replace every `???`, then run `sbt "exercises/testOnly course.intermediate.l08_resilience.*"`.
  *
  * `Transient`, `Fatal` and `RecordingLog` are defined in the lesson.
  */
object Exercises:

  /** Ex 1: the first `n` delays of `schedule` (fewer if it ends before). Hint: `schedule.next(now)`; any `now`
    * works for these schedules, e.g. `Instant.Epoch`.
    */
  def delays(schedule: Schedule, n: Int): List[Duration] = ???

  /** Ex 2: a schedule starting at 10 ms, doubling each time, never above 80 ms, and giving up after 5 retries. */
  def backoff: Schedule = ???

  /** Ex 3: run `op`, retrying `Transient` failures up to 3 times, 10 ms apart. */
  def retryTransient[A](op: A < (Sync & Abort[Transient])): A < (Async & Abort[Transient]) = ???

  /** Ex 4: like `retryTransient`, but `op` can also fail with `Fatal`, which must *not* be retried. */
  def retryOnlyTransient[A](op: A < (Sync & Abort[Transient | Fatal])): A < (Async & Abort[Transient | Fatal]) = ???

  /** Ex 5: log around `op`:
    *   - `info` `"<name>: start"` before;
    *   - `info` `"<name>: done"` after a success;
    *   - `error` `"<name>: failed: <error>"` after a failure, which is then propagated unchanged.
    *
    * Hint: `Abort.recover`, or `Abort.run` and a `match`.
    */
  def logged[A](name: String)(op: A < (Async & Abort[String])): A < (Async & Abort[String]) = ???

  /** Ex 6: retry `Transient` failures of `op` up to 3 times, 10 ms apart, logging a `warn`
    * `"attempt failed: <message>"` after *each* failed attempt.
    */
  def retryLogged[A](op: A < (Sync & Abort[Transient])): A < (Async & Abort[Transient]) = ???

end Exercises
