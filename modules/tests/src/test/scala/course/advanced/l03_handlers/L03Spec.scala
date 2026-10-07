package course.advanced.l03_handlers

import course.KyoSuite
import kyo.*
import Exercises.*

class L03Spec extends KyoSuite:

  test("Ex 1: runTell collects messages in order"):
    val program = Tell("a").andThen(Tell("b")).andThen(Tell("c")).andThen(42)
    assertEquals(runTell(program).eval, (List("a", "b", "c"), 42))
    assertEquals(runTell(7).eval, (Nil, 7))

  test("Ex 2: runFresh"):
    val three = Kyo.fill(3)(Fresh.next).map(_.toList)
    assertEquals(runFresh(10)(three).eval, List(10, 11, 12))
    assertEquals(runFresh(0)(three).eval, List(0, 1, 2))

  test("Ex 2: runFresh restarts for every run"):
    val one = Fresh.next
    assertEquals(runFresh(5)(one).eval, 5)
    assertEquals(runFresh(5)(one).eval, 5)

  test("Ex 3: runFreshCounted"):
    assertEquals(runFreshCounted(Kyo.fill(4)(Fresh.next).map(_.sum)).eval, (4, 0 + 1 + 2 + 3))
    assertEquals(runFreshCounted("none").eval, (0, "none"))

  test("Ex 4: runStop"):
    val stops    = Tell("before").andThen(Stop("enough")).andThen(Tell("after"))
    assertEquals(runTell(runStop(stops)).eval, (List("before"), Left("enough")))
    assertEquals(runStop(42).eval, Right(42))

  test("Ex 5: runFlipAll explores every branch"):
    val twoFlips =
      for
        a <- Flip()
        b <- Flip()
      yield (a, b)
    assertEquals(runFlipAll(twoFlips).eval, List((true, true), (true, false), (false, true), (false, false)))
    assertEquals(runFlipAll("no flip").eval, List("no flip"))

  test("Ex 5: runFlipAll counts paths"):
    val path = Kyo.fill(5)(Flip()).map(_.count(identity))
    assertEquals(runFlipAll(path).eval.size, 32)

  val logged: Int < (Tell & Flip) =
    for
      _ <- Tell("start")
      a <- Flip()
      _ <- Tell(s"flipped $a")
    yield if a then 1 else 0

  test("Ex 6: logPerBranch gives each branch its own log"):
    assertEquals(
      logPerBranch(logged),
      List((List("start", "flipped true"), 1), (List("start", "flipped false"), 0))
    )

  test("Ex 7: sharedLog shares one log"):
    assertEquals(sharedLog(logged), (List("start", "flipped true", "flipped false"), List(1, 0)))
