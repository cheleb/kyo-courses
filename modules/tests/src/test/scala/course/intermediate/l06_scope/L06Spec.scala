package course.intermediate.l06_scope

import course.KyoSuite
import kyo.*
import Exercises.*

class L06Spec extends KyoSuite:

  test("Ex 1: connection is closed when the scope ends"):
    val log = EventLog()
    assertEquals(runKyo(Scope.run(connection("a", log).map(_.query("q")))), "result of q")
    assertEquals(runKyo(log.events), List("open a", "a: q", "close a"))

  test("Ex 1: connection is not closed before the scope ends"):
    val log = EventLog()
    val program = Scope.run:
      for
        c      <- connection("a", log)
        before <- log.events
      yield before
    assertEquals(runKyo(program), List("open a"))

  test("Ex 2: queryOnce"):
    val log = EventLog()
    assertEquals(runKyo(queryOnce(log)("select 1")), "result of select 1")
    assertEquals(runKyo(log.events), List("open db", "db: select 1", "close db"))

  test("Ex 3: copy closes in reverse order"):
    val log = EventLog()
    assertEquals(runKyo(copy(log)), "result of write")
    assertEquals(
      runKyo(log.events),
      List("open source", "open target", "source: read", "target: write", "close target", "close source")
    )

  test("Ex 4: queryThenFail still closes"):
    val log = EventLog()
    assertEquals(runKyo(Abort.run(queryThenFail(log))), Result.fail("boom"))
    assertEquals(runKyo(log.events), List("open db", "db: update", "close db"))

  test("Ex 5: slowQuery times out and still closes"):
    val log   = EventLog()
    val start = System.nanoTime()
    assertEquals(runKyo(slowQuery(log)), Maybe.empty[String])
    assert((System.nanoTime() - start) / 1_000_000 < 800, "did not give up early")
    assertEquals(runKyo(log.events), List("open slow", "close slow"))

  test("Ex 6: tracked logs a success"):
    val log = EventLog()
    assertEquals(runKyo(tracked(log)(log.add("working").andThen(1))), 1)
    assertEquals(runKyo(log.events), List("working", "outcome: success"))

  test("Ex 6: tracked logs a failure"):
    val log = EventLog()
    assertEquals(runKyo(Abort.run(tracked(log)(Abort.fail("bad")))), Result.fail("bad"))
    assertEquals(runKyo(log.events), List("outcome: failure bad"))

  test("Ex 6: tracked logs a panic"):
    val log    = EventLog()
    val result = runKyo(Abort.run[Any](tracked(log)(Sync.defer(throw new IllegalStateException("crash")))))
    assert(result.isPanic)
    assertEquals(runKyo(log.events), List("outcome: panic crash"))

  test("Ex 7: openAll opens in order and closes in reverse"):
    val log = EventLog()
    val names = runKyo(Scope.run(openAll(Seq("a", "b", "c"), log).map(_.map(_.name))))
    assertEquals(names.toList, List("a", "b", "c"))
    assertEquals(runKyo(log.events), List("open a", "open b", "open c", "close c", "close b", "close a"))
