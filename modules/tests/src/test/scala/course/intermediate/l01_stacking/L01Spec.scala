package course.intermediate.l01_stacking

import course.KyoSuite
import kyo.*
import Exercises.*

class L01Spec extends KyoSuite:

  val succeeds: String < (Var[Int] & Abort[String]) = Var.update[Int](_ + 1).map(n => s"done at $n")

  val fails: String < (Var[Int] & Abort[String]) =
    Var.updateDiscard[Int](_ + 10).andThen(Abort.fail("boom"))

  test("Ex 1: stateLostOnFailure"):
    assertEquals(stateLostOnFailure(1)(succeeds), Result.succeed((2, "done at 2")))
    assertEquals(stateLostOnFailure(1)(fails), Result.fail("boom"))

  test("Ex 2: stateKeptOnFailure"):
    assertEquals(stateKeptOnFailure(1)(succeeds), (2, Result.succeed("done at 2")))
    assertEquals(stateKeptOnFailure(1)(fails), (11, Result.fail("boom")))

  test("Ex 3: processAll skips failures"):
    assertEquals(processAll(Seq(1, -2, 3, -4, 5)), (9, Seq("negative: -2", "negative: -4")))
    assertEquals(processAll(Seq(1, 2)), (3, Seq.empty))
    assertEquals(processAll(Seq.empty), (0, Seq.empty))

  test("Ex 4: processUntilError stops at the first failure"):
    assertEquals(processUntilError(Seq(1, 2, -3, 4)), (3, Maybe("negative: -3")))
    assertEquals(processUntilError(Seq(1, 2)), (3, Maybe.empty[String]))

  test("Ex 5: classify"):
    assertEquals(classify(42), "value 42")
    assertEquals(classify(Abort.fail(404)), "code 404")
    assertEquals(classify(Abort.fail("not found")), "message not found")

  test("Ex 6: transaction keeps the changes on success"):
    assertEquals(stateKeptOnFailure(1)(transaction(succeeds)), (2, Result.succeed("done at 2")))

  test("Ex 6: transaction rolls back on failure"):
    assertEquals(stateKeptOnFailure(1)(transaction(fails)), (1, Result.fail("boom")))

  test("Ex 6: transaction only rolls back its own changes"):
    val program =
      for
        _ <- Var.updateDiscard[Int](_ + 100)
        r <- Abort.run(transaction(fails))
      yield r
    assertEquals(stateKeptOnFailure(0)(program), (100, Result.succeed(Result.fail("boom"))))
