package course.intermediate.l05_concurrency

import kyo.*

/** I5 solutions. */
object Exercises:

  def countConcurrently(n: Int): Int < Async =
    for
      counter <- AtomicInt.init
      _       <- Async.foreachDiscard(1 to n)(_ => counter.incrementAndGet)
      total   <- counter.get
    yield total

  def offerAll(capacity: Int, items: Seq[Int]): (Seq[Int], Seq[Int]) < (Sync & Abort[Closed]) =
    Queue.use[Int](capacity): queue =>
      Kyo.foreach(items)(item => queue.offer(item).map(item -> _)).map: results =>
        val (accepted, rejected) = results.partition(_._2)
        (accepted.map(_._1), rejected.map(_._1))

  def sumThroughChannel(items: Seq[Int]): Int < (Async & Scope & Abort[Closed]) =
    for
      channel <- Channel.init[Int](4)
      _       <- Fiber.init(Kyo.foreachDiscard(items)(channel.put).andThen(channel.closeAwaitEmpty))
      total   <- channel.streamUntilClosed().foldPure(0)(_ + _)
    yield total

  def runLimited[A](max: Int)(tasks: Seq[A < Async]): Seq[A] < (Async & Abort[Closed]) =
    Meter.useSemaphore(max)(meter => Async.foreach(tasks)(task => meter.run(task)))

  def depositAll(account: Account, amounts: Seq[Long]): Unit < (Async & Abort[Closed]) =
    Meter.useMutex(mutex => Async.foreachDiscard(amounts)(amount => mutex.run(account.unsafeDeposit(amount))))

  def waitForWorkers(n: Int): List[Int] < (Async & Scope) =
    for
      latch    <- Latch.init(n)
      recorded <- AtomicRef.init(List.empty[Int])
      _ <- Kyo.foreachDiscard(1 to n): i =>
        Fiber.init:
          for
            _ <- Async.sleep(((n - i) * 5).millis)
            _ <- recorded.updateAndGet(_ :+ i)
            _ <- latch.release
          yield ()
      _   <- latch.await
      ids <- recorded.get
    yield ids

  def broadcast(messages: Seq[String], listeners: Int): Seq[Seq[String]] < (Async & Scope & Abort[Closed]) =
    for
      hub         <- Hub.init[String]
      subscribers <- Kyo.fill(listeners)(hub.listen)
      _           <- Kyo.foreachDiscard(messages)(hub.put)
      received    <- Async.foreach(subscribers)(_.takeExactly(messages.size))
    yield received

end Exercises
