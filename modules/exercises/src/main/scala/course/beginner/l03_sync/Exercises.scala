package course.beginner.l03_sync

import java.util.concurrent.atomic.AtomicInteger
import kyo.*

/** B3 exercises — replace every `???`, then run `sbt "exercises/testOnly course.beginner.l03_sync.*"`. */
object Exercises:

  /** Ex 1: increment `counter` and return the new value. The increment must happen when the program runs, not
    * when it is built. Hint: `Sync.defer`.
    */
  def increment(counter: AtomicInteger): Int < Sync = ???

  /** Ex 2: roll a six-sided die: a number between 1 and 6 included. Hint: `Random.nextInt(bound)`. */
  def rollDie: Int < Sync = ???

  /** Ex 3: pick one of the (non-empty) `names` at random. */
  def pickWinner(names: Seq[String]): String < Sync = ???

  /** Ex 4: prefix `message` with the current time, e.g. `[1970-01-01T00:00:00Z] hello`. Hint: `Clock.now`,
    * `Instant.show`.
    */
  def timestamped(message: String): String < Sync = ???

  /** Ex 5: run `v` and return its result together with how long it took, according to `Clock.now`.
    *
    * `v` may have other effects `S`: they simply remain in the result type. Hint: `Instant.minusOrZero`.
    */
  def timed[A, S](v: A < S): (A, Duration) < (Sync & S) = ???

end Exercises
