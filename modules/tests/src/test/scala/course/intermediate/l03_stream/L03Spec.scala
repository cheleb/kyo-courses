package course.intermediate.l03_stream

import course.KyoSuite
import kyo.*
import Exercises.*

class L03Spec extends KyoSuite:

  def all[V: Tag](s: Stream[V, Any]): List[V] = s.run.eval.toList

  test("Ex 1: fromList"):
    assertEquals(all(fromList(Seq(3, 1, 2))), List(3, 1, 2))
    assertEquals(all(fromList(Nil)), Nil)

  test("Ex 2: evens"):
    assertEquals(all(evens(5)), List(0, 2, 4, 6, 8))
    assertEquals(all(evens(0)), Nil)

  test("Ex 3: fibonacci"):
    assertEquals(all(fibonacci.take(10)), List(0L, 1L, 1L, 2L, 3L, 5L, 8L, 13L, 21L, 34L))
    assertEquals(all(fibonacci.drop(90).take(1)), List(2880067194370816120L))

  test("Ex 4: wordCount"):
    assertEquals(wordCount(Stream.init(Seq("hello kyo", "  streams   are lazy ", ""))).eval, 5)
    assertEquals(wordCount(Stream.empty).eval, 0)

  test("Ex 5: parseAll"):
    def run(lines: String*) = Abort.run(parseAll(Stream.init(lines)).run).eval.map(_.toList)
    assertEquals(run("1", "2", "3"), Result.succeed(List(1, 2, 3)))
    assertEquals(run("1", "two", "3", "four"), Result.fail("not a number: two"))

  test("Ex 6: parseFirst ignores the lines after the first n"):
    assertEquals(parseFirst(2)(Seq("1", "2", "x", "4")).map(_.toList), Result.succeed(List(1, 2)))
    assertEquals(parseFirst(3)(Seq("1", "2", "x", "4")), Result.fail("not a number: x"))
    assertEquals(parseFirst(0)(Seq("x")).map(_.toList), Result.succeed(Nil))

  test("Ex 7: firstOver works on infinite streams"):
    assertEquals(firstOver(100)(Stream.range(0, Int.MaxValue).mapPure(n => n * n)).eval, Maybe(121))
    assertEquals(firstOver(10)(Stream.init(Seq(1, 2, 3))).eval, Maybe.empty[Int])

  test("Ex 8: runningTotal"):
    assertEquals(all(runningTotal(Stream.init(Seq(1, 2, 3, 4)))), List(1, 3, 6, 10))
    assertEquals(all(runningTotal(Stream.empty)), Nil)

  test("Ex 8: runningTotal is lazy"):
    assertEquals(all(runningTotal(Stream.range(1, Int.MaxValue)).take(4)), List(1, 3, 6, 10))
