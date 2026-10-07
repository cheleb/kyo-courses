package course.beginner.l07_data

import kyo.*

/** B7 exercises — replace every `???`, then run `sbt "exercises/testOnly course.beginner.l07_data.*"`. */
object Exercises:

  /** Ex 1: the first even number of `xs`, if any. */
  def firstEven(xs: Seq[Int]): Maybe[Int] = ???

  /** Ex 2: the length of `name`, or 0 when it is `null` (it comes from a Java API). Hint: `Maybe(...)`. */
  def nameLength(name: String): Int = ???

  /** Ex 3: parse a TCP port:
    *   - `"not a number: <s>"` if `s` is not an integer;
    *   - `"out of range: <port>"` if it is not between 1 and 65535 included.
    *
    * Hint: `Result.catching`, `mapFailure`, `flatMap`.
    */
  def parsePort(s: String): Result[String, Int] = ???

  /** Ex 4: the first input that is a valid port, if any. Reuse `parsePort`. */
  def firstValidPort(inputs: Seq[String]): Maybe[Int] = ???

  /** Ex 5: append `entry` to `log`, keeping only the last `max` entries. */
  def rollingLog(log: Chunk[String], entry: String, max: Int): Chunk[String] = ???

  /** Ex 6: parse a duration such as `"5 seconds"`, falling back to 30 seconds when it is invalid. */
  def parseTimeout(s: String): Duration = ???

  /** Ex 7: the sum of all durations (`Duration.Zero` for none). */
  def totalTime(parts: Seq[Duration]): Duration = ???

  /** Ex 8: whether a task started at `start` with the given `timeout` has expired at `now` (strictly after
    * `start + timeout`).
    */
  def isExpired(start: Instant, timeout: Duration, now: Instant): Boolean = ???

  /** Ex 9: the first even number, as an `Abort` computation that fails with `Absent` if there is none.
    * Reuse `firstEven`.
    */
  def requireEven(xs: Seq[Int]): Int < Abort[Absent] = ???

end Exercises
