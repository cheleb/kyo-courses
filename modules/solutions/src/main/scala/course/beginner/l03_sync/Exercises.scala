package course.beginner.l03_sync

import java.util.concurrent.atomic.AtomicInteger
import kyo.*

/** B3 solutions. */
object Exercises:

  def increment(counter: AtomicInteger): Int < Sync = Sync.defer(counter.incrementAndGet())

  def rollDie: Int < Sync = Random.nextInt(6).map(_ + 1)

  def pickWinner(names: Seq[String]): String < Sync = Random.nextValue(names)

  def timestamped(message: String): String < Sync =
    Clock.now.map(now => s"[${now.show}] $message")

  def timed[A, S](v: A < S): (A, Duration) < (Sync & S) =
    for
      start  <- Clock.now
      result <- v
      end    <- Clock.now
    yield (result, end.minusOrZero(start))

end Exercises
