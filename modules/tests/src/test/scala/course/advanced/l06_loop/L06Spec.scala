package course.advanced.l06_loop

import course.KyoSuite
import java.util.concurrent.atomic.AtomicInteger
import kyo.*
import Exercises.*

class L06Spec extends KyoSuite:

  test("Ex 1: sumOfSquares"):
    assertEquals(sumOfSquares(3).eval, 14L)
    assertEquals(sumOfSquares(0).eval, 0L)
    assertEquals(sumOfSquares(1_000_000).eval, 333333833333500000L)

  test("Ex 2: fib"):
    assertEquals((0 to 10).map(fib(_).eval).toList, List(0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55).map(BigInt(_)))
    assertEquals(fib(100).eval, BigInt("354224848179261915075"))

  test("Ex 3: collatzSteps"):
    assertEquals(collatzSteps(1).eval, 0)
    assertEquals(collatzSteps(6).eval, 8)
    assertEquals(collatzSteps(27).eval, 111)

  test("Ex 4: drain"):
    val (emitted, (state, _)) = Emit.run[Int](Var.runTuple(4)(drain)).eval
    assertEquals(emitted.toList, List(4, 3, 2, 1))
    assertEquals(state, 0)
    assertEquals(Emit.run[Int](Var.run(0)(drain)).eval._1.toList, Nil)

  test("Ex 5: firstAbove stops at the first match"):
    val evaluated = AtomicInteger()
    val xs        = Seq(1, 5, 9, 2).map(n => Sync.defer { evaluated.incrementAndGet(); n })
    assertEquals(runKyo(firstAbove(4)(xs)), Maybe(5))
    assertEquals(evaluated.get, 2)
    assertEquals(runKyo(firstAbove(100)(xs)), Maybe.empty[Int])

  test("Ex 6: pipeline"):
    def run(s: String) = Abort.run(pipeline(s)).eval
    assertEquals(run(" 42 "), Result.succeed(21))
    assertEquals(run("abc"), Result.fail("not a number: abc"))
    assertEquals(run("7"), Result.fail("odd: 7"))

  test("Ex 7: factorial"):
    assertEquals(factorial(0).eval, BigInt(1))
    assertEquals(factorial(10).eval, BigInt(3628800))

  test("Ex 7: factorial is stack-safe"):
    assertEquals(factorial(5000).eval.toString.length, 16326)
