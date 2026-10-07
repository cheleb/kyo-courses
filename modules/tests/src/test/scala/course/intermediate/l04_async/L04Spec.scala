package course.intermediate.l04_async

import course.KyoSuite
import kyo.*
import Exercises.*

class L04Spec extends KyoSuite:

  /** Runs `v` and returns its result with the elapsed wall-clock time in milliseconds. */
  def timed[A](v: => A < (Async & Scope & Abort[Any])): (A, Long) =
    val start  = System.nanoTime()
    val result = runKyo(v)
    (result, (System.nanoTime() - start) / 1_000_000)

  test("Ex 1: slowDouble waits, then doubles"):
    val (result, millis) = timed(slowDouble(21))
    assertEquals(result, 42)
    assert(millis >= 50, s"took only $millis ms")

  test("Ex 2: doubleAll runs concurrently and keeps the order"):
    val (result, millis) = timed(doubleAll(1 to 10))
    assertEquals(result.toList, (1 to 10).map(_ * 2).toList)
    assert(millis < 400, s"took $millis ms: not concurrent?")

  test("Ex 3: both runs concurrently"):
    val (result, millis) = timed(both(Async.delay(100.millis)("a"), Async.delay(100.millis)(1)))
    assertEquals(result, ("a", 1))
    assert(millis < 180, s"took $millis ms: not concurrent?")

  test("Ex 4: fastest"):
    assertEquals(runKyo(fastest(Seq("eu" -> 120.millis, "us" -> 20.millis, "asia" -> 80.millis))), "us")

  test("Ex 4: fastest does not wait for the slow mirrors"):
    val (_, millis) = timed(fastest(Seq("slow" -> 2.seconds, "fast" -> 10.millis)))
    assert(millis < 1000, s"took $millis ms: losers not interrupted?")

  test("Ex 5: firstSuccess ignores faster failures"):
    val attempts = Seq(
      Async.delay(10.millis)(Abort.fail("down")),
      Async.delay(60.millis)("backup"),
      Async.delay(500.millis)("slow")
    )
    assertEquals(runKyo(Abort.run(firstSuccess(attempts))), Result.succeed("backup"))

  test("Ex 5: firstSuccess fails when everything fails"):
    val attempts = Seq(Async.delay(10.millis)(Abort.fail("a")), Async.delay(20.millis)(Abort.fail("b")))
    assert(runKyo(Abort.run(firstSuccess(attempts))).isFailure)

  test("Ex 6: withTimeout"):
    assertEquals(runKyo(withTimeout(500.millis)(Async.delay(10.millis)(1))), Maybe(1))
    assertEquals(runKyo(withTimeout(20.millis)(Async.delay(2.seconds)(1))), Maybe.empty[Int])

  test("Ex 7: tickFor ticks, then stops"):
    val count = runKyo(AtomicInt.init)
    runKyo(tickFor(100.millis, count))
    val afterRun = runKyo(count.get)
    assert(afterRun >= 3 && afterRun <= 12, s"ticked $afterRun times in 100 ms")
    runKyo(Async.sleep(60.millis))
    assertEquals(runKyo(count.get), afterRun)
