package course.intermediate.l05_concurrency

import kyo.*

/** # I5 — Concurrency primitives
  *
  * Fibers (I4) need ways to share data and coordinate. Kyo provides lock-free primitives, all safe to use from many
  * fibers at once:
  *
  *   - `AtomicInt`, `AtomicLong`, `AtomicBoolean`, `AtomicRef[A]`: shared mutable cells with atomic updates. Unlike
  *     `Var`, which is local to one computation, an atomic is *shared* by every fiber that holds it.
  *   - `Queue[A]`: a bounded, *non-blocking* queue: `offer` returns `false` when full, `poll` returns `Absent` when
  *     empty. Its operations only need `Sync`. Pitfall: the capacity is *rounded up to the next power of two*
  *     (`Queue.init(3)` holds 4 elements).
  *   - `Channel[A]`: a bounded, *asynchronous* queue with back-pressure: `put` waits while it is full, `take` waits
  *     while it is empty. It can be consumed as a `Stream`.
  *   - `Hub[A]`: broadcast: every listener receives every message published after it subscribed.
  *   - `Meter`: limits concurrency: a mutex (1 at a time), a semaphore (n at a time) or a rate limiter.
  *   - `Latch`: waits until a counter, decremented by `release`, reaches zero.
  *
  * Queues, channels and hubs can be *closed*; operations on a closed one fail with `Abort[Closed]`. They are
  * created with `init` (requires `Scope`: closed when the scope ends) or `use(...)(f)` (closed after `f`).
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.intermediate.l05_concurrency.ConcurrencyLesson"
  * }}}
  */
object ConcurrencyLesson extends KyoApp:

  // Atomics: 1000 fibers increment the same counter.
  val atomicCounter: Int < Async =
    for
      counter <- AtomicInt.init
      _       <- Async.foreachDiscard(1 to 1000)(_ => counter.incrementAndGet)
      total   <- counter.get
    yield total

  // Queue: non-blocking. A full queue rejects new elements instead of waiting.
  val queueDemo: (Seq[Boolean], Chunk[Int]) < (Sync & Abort[Closed]) =
    Queue.use[Int](2): queue =>
      for
        offered <- Kyo.foreach(1 to 4)(queue.offer)
        drained <- queue.drain
      yield (offered, drained)

  // Channel: a producer and a consumer, with back-pressure (capacity 2).
  // `closeAwaitEmpty` closes the channel once the consumer has taken everything; plain `close` would *discard* the
  // elements still buffered.
  val channelDemo: Int < (Async & Scope & Abort[Closed]) =
    for
      channel  <- Channel.init[Int](2)
      producer <- Fiber.init(Kyo.foreachDiscard(1 to 10)(channel.put).andThen(channel.closeAwaitEmpty))
      total    <- channel.streamUntilClosed().foldPure(0)(_ + _)
    yield total

  // Hub: every listener gets every message.
  val hubDemo: Chunk[Chunk[String]] < (Async & Scope & Abort[Closed]) =
    for
      hub       <- Hub.init[String]
      listeners <- Kyo.fill(3)(hub.listen)
      _         <- Kyo.foreachDiscard(Seq("a", "b"))(hub.put)
      received  <- Async.foreach(listeners)(_.takeExactly(2))
    yield received

  // Meter: a semaphore lets at most 2 tasks run at the same time.
  val meterDemo: Int < (Async & Abort[Closed]) =
    for
      running <- AtomicInt.init
      maxSeen <- AtomicInt.init
      _ <- Meter.useSemaphore(2): meter =>
        Async.foreachDiscard(1 to 8): _ =>
          meter.run:
            for
              now <- running.incrementAndGet
              _   <- maxSeen.getAndUpdate(_ max now)
              _   <- Async.sleep(20.millis)
              _   <- running.decrementAndGet
            yield ()
      max <- maxSeen.get
    yield max

  // Latch: wait until 3 workers are done.
  val latchDemo: String < (Async & Scope) =
    for
      latch <- Latch.init(3)
      _     <- Kyo.foreachDiscard(1 to 3)(i => Fiber.init(Async.sleep((i * 10).millis).andThen(latch.release)))
      _     <- latch.await
    yield "all 3 workers done"

  run:
    for
      c <- atomicCounter
      _ <- Console.printLine(s"atomic counter after 1000 concurrent increments: $c")
      q <- queueDemo
      _ <- Console.printLine(s"queue of capacity 2, offered 1..4: accepted=${q._1}, drained=${q._2}")
      t <- channelDemo
      _ <- Console.printLine(s"channel: consumer summed $t")
      h <- hubDemo
      _ <- Console.printLine(s"hub: $h")
      m <- meterDemo
      _ <- Console.printLine(s"semaphore(2): at most $m tasks ran at once")
      l <- latchDemo
      _ <- Console.printLine(s"latch: $l")
    yield ()

end ConcurrencyLesson

/** A bank account that is *not* thread-safe, used by the exercises. */
final class Account:
  @volatile private var balance = 0L

  def get: Long < Sync = Sync.defer(balance)

  /** Reads the balance, pauses (as a real system would, e.g. for I/O), then writes it back. Two concurrent deposits
    * can read the same old balance: one of them is lost.
    */
  def unsafeDeposit(amount: Long): Unit < Async =
    for
      current <- Sync.defer(balance)
      _       <- Async.sleep(1.millis)
      _       <- Sync.defer { balance = current + amount }
    yield ()
end Account
