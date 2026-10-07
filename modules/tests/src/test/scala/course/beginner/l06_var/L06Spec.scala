package course.beginner.l06_var

import course.KyoSuite
import kyo.*
import Exercises.*
import Instr.*

class L06Spec extends KyoSuite:

  def stack(start: List[Int])(v: Unit < (Var[List[Int]] & Abort[String])): Result[String, List[Int]] =
    Abort.run(Var.runTuple(start)(v).map(_._1)).eval

  test("Ex 1: next returns the new value"):
    assertEquals(Var.runTuple(0)(next).eval, (1, 1))
    assertEquals(Var.runTuple(41)(next).eval, (42, 42))

  test("Ex 1: next can be called several times"):
    val (state, values) = Var.runTuple(0)(Kyo.fill(3)(next)).eval
    assertEquals(state, 3)
    assertEquals(values.toList, List(1, 2, 3))

  test("Ex 2: swap returns the previous value"):
    assertEquals(Var.runTuple(1)(swap(5)).eval, (5, 1))

  test("Ex 3: labels numbers names from the current counter"):
    assertEquals(Var.runTuple(0)(labels(Seq("alice", "bob"))).eval._2.toList, List("1. alice", "2. bob"))
    assertEquals(Var.runTuple(10)(labels(Seq("carol"))).eval, (11, Seq("11. carol")))

  test("Ex 4: runLabels starts at 0"):
    assertEquals(runLabels(Seq("alice", "bob", "carol")).toList, List("1. alice", "2. bob", "3. carol"))
    assertEquals(runLabels(Seq.empty).toList, Nil)

  test("Ex 5: finalCount"):
    assertEquals(finalCount(0, 3), 3)
    assertEquals(finalCount(10, 0), 10)
    assertEquals(finalCount(-5, 5), 0)

  test("Ex 6: exec Push"):
    assertEquals(stack(List(1))(exec(Push(2))), Result.succeed(List(2, 1)))

  test("Ex 6: exec Add and Mul"):
    assertEquals(stack(List(3, 4, 9))(exec(Add)), Result.succeed(List(7, 9)))
    assertEquals(stack(List(3, 4))(exec(Mul)), Result.succeed(List(12)))

  test("Ex 6: exec fails on underflow"):
    assertEquals(stack(List(3))(exec(Add)), Result.fail("stack underflow"))
    assertEquals(stack(Nil)(exec(Mul)), Result.fail("stack underflow"))

  test("Ex 7: evaluate runs a whole program"):
    assertEquals(evaluate(Seq(Push(2), Push(3), Add, Push(4), Mul)), Result.succeed(20))

  test("Ex 7: evaluate reports errors"):
    assertEquals(evaluate(Seq(Push(1), Add)), Result.fail("stack underflow"))
    assertEquals(evaluate(Seq.empty), Result.fail("empty stack"))

  test("Ex 7: evaluate stops at the first error"):
    assertEquals(evaluate(Seq(Add, Push(1))), Result.fail("stack underflow"))
