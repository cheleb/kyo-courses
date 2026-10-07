package course.intermediate.l05_concurrency

import course.KyoSuite
import kyo.*
import Exercises.*

class L05Spec extends KyoSuite:

  test("Ex 1: countConcurrently"):
    assertEquals(runKyo(countConcurrently(1000)), 1000)
    assertEquals(runKyo(countConcurrently(0)), 0)

  test("Ex 2: offerAll"):
    assertEquals(runKyo(offerAll(4, 1 to 6)), (Seq(1, 2, 3, 4), Seq(5, 6)))
    // The capacity is rounded up to the next power of two: 3 becomes 4.
    assertEquals(runKyo(offerAll(3, 1 to 6)), (Seq(1, 2, 3, 4), Seq(5, 6)))
    assertEquals(runKyo(offerAll(10, Seq(1, 2))), (Seq(1, 2), Seq.empty))

  test("Ex 3: sumThroughChannel does not lose items"):
    assertEquals(runKyo(sumThroughChannel(1 to 100)), 5050)
    assertEquals(runKyo(sumThroughChannel(Nil)), 0)

  test("Ex 4: runLimited respects the limit and the order"):
    val program =
      for
        running <- AtomicInt.init
        maxSeen <- AtomicInt.init
        task = (i: Int) =>
          for
            now <- running.incrementAndGet
            _   <- maxSeen.getAndUpdate(_ max now)
            _   <- Async.sleep(10.millis)
            _   <- running.decrementAndGet
          yield i * 10
        results <- runLimited(3)((1 to 12).map(task))
        max     <- maxSeen.get
      yield (results.toList, max)
    val (results, max) = runKyo(program)
    assertEquals(results, (1 to 12).map(_ * 10).toList)
    assert(max <= 3, s"$max tasks ran at the same time")
    assert(max >= 2, s"only $max task ran at a time: not concurrent?")

  test("Ex 5: depositAll does not lose deposits"):
    val account = Account()
    runKyo(depositAll(account, Seq.fill(50)(10L)))
    assertEquals(runKyo(account.get), 500L)

  test("Ex 6: waitForWorkers waits for everyone"):
    assertEquals(runKyo(waitForWorkers(5)), List(5, 4, 3, 2, 1))

  test("Ex 7: broadcast"):
    val received = runKyo(broadcast(Seq("a", "b", "c"), listeners = 3))
    assertEquals(received.map(_.toList).toList, List.fill(3)(List("a", "b", "c")))
