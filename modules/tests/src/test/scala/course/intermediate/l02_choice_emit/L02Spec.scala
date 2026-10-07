package course.intermediate.l02_choice_emit

import course.KyoSuite
import kyo.*
import Exercises.*

class L02Spec extends KyoSuite:

  def all[A](v: A < Choice): List[A] = Choice.run(v).eval.toList

  def emitted[A](v: A < Emit[Int]): (List[Int], A) =
    Emit.run(v).map((values, a) => (values.toList, a)).eval

  test("Ex 1: coin"):
    assertEquals(all(coin), List("heads", "tails"))

  test("Ex 2: pairs"):
    assertEquals(all(pairs(3)), List((1, 2), (1, 3), (2, 3)))
    assertEquals(all(pairs(1)), Nil)

  test("Ex 3: pythagorean"):
    assertEquals(all(pythagorean(15)), List((3, 4, 5), (5, 12, 13), (6, 8, 10), (9, 12, 15)))

  test("Ex 4: permutations"):
    assertEquals(
      all(permutations(List(1, 2, 3))),
      List(List(1, 2, 3), List(1, 3, 2), List(2, 1, 3), List(2, 3, 1), List(3, 1, 2), List(3, 2, 1))
    )
    assertEquals(all(permutations(Nil)), List(Nil))
    assertEquals(all(permutations(List(1, 2, 3, 4, 5))).distinct.size, 120)

  test("Ex 5: queens"):
    assertEquals(all(queens(4)), List(List(1, 3, 0, 2), List(2, 0, 3, 1)))
    assertEquals(all(queens(6)).size, 4)
    assertEquals(all(queens(8)).size, 92)
    assertEquals(all(queens(3)), Nil)

  test("Ex 6: countdown"):
    assertEquals(emitted(countdown(3)), (List(3, 2, 1), ()))
    assertEquals(emitted(countdown(0)), (Nil, ()))

  test("Ex 7: collatz"):
    assertEquals(emitted(collatz(6)), (List(6, 3, 10, 5, 16, 8, 4, 2, 1), 8))
    assertEquals(emitted(collatz(1)), (List(1), 0))

  test("Ex 8: sumEmitted"):
    assertEquals(sumEmitted(countdown(4)), 10)
    assertEquals(sumEmitted(()), 0)

  test("Ex 9: filterEmitted"):
    assertEquals(emitted(filterEmitted(2)(countdown(5))), (List(5, 4, 3), ()))
    assertEquals(emitted(filterEmitted(10)(countdown(5))), (Nil, ()))
