package course.advanced.l01_pending_internals

import course.KyoSuite
import kyo.*
import Exercises.*

class L01Spec extends KyoSuite:

  test("Ex 1: sameType"):
    assert(sameType[Var[Int], Var[Int]])
    assert(!sameType[Var[Int], Var[String]])
    assert(sameType[Abort[String] & Env[Int], Env[Int] & Abort[String]])

  test("Ex 2: isSubtype"):
    assert(isSubtype[Int, AnyVal])
    assert(!isSubtype[AnyVal, Int])
    assert(isSubtype[List[Int], Seq[Int]])
    assert(isSubtype[Abort[Throwable], Abort[Exception]], "Abort is contravariant")

  test("Ex 3: callSite reports the caller"):
    val (site, here) = (callSite, summon[munit.Location]) // both captured on this very line
    assertEquals(site, s"L01Spec.scala:${here.line}")

  test("Ex 4: check points at the caller"):
    assertEquals(Abort.run(check(1 < 2, "math works")).eval, Result.succeed(()))
    val failure = Abort.run(check(1 > 2, "math is broken")).eval
    failure match
      case Result.Failure(message) =>
        assert(message.startsWith("math is broken (at L01Spec.scala:"), message)
      case other => fail(s"expected a failure, got $other")

  test("Ex 5: sumTo"):
    assertEquals(runKyo(sumTo(10)), 55L)
    assertEquals(runKyo(sumTo(0)), 0L)

  test("Ex 5: sumTo is stack-safe"):
    assertEquals(runKyo(sumTo(1_000_000)), 500000500000L)

  test("Ex 6: flattenAll"):
    val v: Int < Sync < Sync < Sync = Kyo.lift(Kyo.lift(Sync.defer(42)))
    assertEquals(runKyo(flattenAll(v)), 42)

  test("Ex 7: runAll keeps the state on success and failure"):
    val ok: Int < (Env[Int] & Abort[String] & Var[Int]) =
      Env.get[Int].map(n => Var.update[Int](_ + n))
    val ko: Int < (Env[Int] & Abort[String] & Var[Int]) =
      Env.get[Int].map(n => Var.updateDiscard[Int](_ + n).andThen(Abort.fail("boom")))
    assertEquals(runAll(5)(ok), (5, Result.succeed(5)))
    assertEquals(runAll(7)(ko), (7, Result.fail("boom")))
