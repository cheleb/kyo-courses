package course.beginner.l07_data

import course.KyoSuite
import kyo.*
import Exercises.*

class L07Spec extends KyoSuite:

  test("Ex 1: firstEven"):
    assertEquals(firstEven(Seq(1, 3, 4, 6)), Maybe(4))
    assertEquals(firstEven(Seq(1, 3)), Maybe.empty[Int])
    assertEquals(firstEven(Nil), Maybe.empty[Int])

  test("Ex 2: nameLength handles null"):
    assertEquals(nameLength("alice"), 5)
    assertEquals(nameLength(null), 0)

  test("Ex 3: parsePort accepts valid ports"):
    assertEquals(parsePort("8080"), Result.succeed(8080))
    assertEquals(parsePort("1"), Result.succeed(1))
    assertEquals(parsePort("65535"), Result.succeed(65535))

  test("Ex 3: parsePort rejects invalid input"):
    assertEquals(parsePort("http"), Result.fail("not a number: http"))
    assertEquals(parsePort("0"), Result.fail("out of range: 0"))
    assertEquals(parsePort("70000"), Result.fail("out of range: 70000"))

  test("Ex 4: firstValidPort"):
    assertEquals(firstValidPort(Seq("x", "0", "443", "80")), Maybe(443))
    assertEquals(firstValidPort(Seq("x", "0")), Maybe.empty[Int])

  test("Ex 5: rollingLog keeps the last entries"):
    assertEquals(rollingLog(Chunk("a", "b"), "c", 3), Chunk("a", "b", "c"))
    assertEquals(rollingLog(Chunk("a", "b", "c"), "d", 3), Chunk("b", "c", "d"))
    assertEquals(rollingLog(Chunk.empty, "a", 2), Chunk("a"))

  test("Ex 6: parseTimeout"):
    assertEquals(parseTimeout("5 seconds"), 5.seconds)
    assertEquals(parseTimeout("250 millis"), 250.millis)
    assertEquals(parseTimeout("whenever"), 30.seconds)

  test("Ex 7: totalTime"):
    assertEquals(totalTime(Seq(1.minute, 30.seconds, 500.millis)), 90500.millis)
    assertEquals(totalTime(Nil), Duration.Zero)

  test("Ex 8: isExpired"):
    val start = Instant.Epoch
    assert(!isExpired(start, 10.seconds, start + 5.seconds))
    assert(!isExpired(start, 10.seconds, start + 10.seconds))
    assert(isExpired(start, 10.seconds, start + 11.seconds))

  test("Ex 9: requireEven"):
    assertEquals(Abort.run(requireEven(Seq(1, 2))).eval, Result.succeed(2))
    assert(Abort.run(requireEven(Seq(1, 3))).eval.isFailure)
