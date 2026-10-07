package course.beginner.l04_abort

import course.KyoSuite
import kyo.*
import Exercises.*

class L04Spec extends KyoSuite:

  def result[A](v: A < Abort[AgeError]): Result[AgeError, A] = Abort.run(v).eval

  val users = Map("alice" -> "42", "bob" -> "abc", "carol" -> "-3")

  test("Ex 1: checkAge accepts the bounds"):
    assertEquals(result(checkAge(0)), Result.succeed(0))
    assertEquals(result(checkAge(150)), Result.succeed(150))

  test("Ex 1: checkAge rejects out-of-range ages"):
    assertEquals(result(checkAge(-1)), Result.fail(AgeError.Negative(-1)))
    assertEquals(result(checkAge(151)), Result.fail(AgeError.TooOld(151)))

  test("Ex 2: parseInt parses numbers"):
    assertEquals(result(parseInt("42")), Result.succeed(42))
    assertEquals(result(parseInt("-7")), Result.succeed(-7))

  test("Ex 2: parseInt turns the exception into NotANumber"):
    assertEquals(result(parseInt("abc")), Result.fail(AgeError.NotANumber("abc")))

  test("Ex 3: parseAge parses and checks"):
    assertEquals(result(parseAge("42")), Result.succeed(42))
    assertEquals(result(parseAge("abc")), Result.fail(AgeError.NotANumber("abc")))
    assertEquals(result(parseAge("-1")), Result.fail(AgeError.Negative(-1)))
    assertEquals(result(parseAge("200")), Result.fail(AgeError.TooOld(200)))

  test("Ex 4: safeParseAge returns a Result"):
    assertEquals(safeParseAge("42"), Result.succeed(42))
    assertEquals(safeParseAge("200"), Result.fail(AgeError.TooOld(200)))

  test("Ex 5: ageOrZero falls back to 0"):
    assertEquals(runKyo(ageOrZero("42")), 42)
    assertEquals(runKyo(ageOrZero("abc")), 0)
    assertEquals(runKyo(ageOrZero("-1")), 0)

  test("Ex 6: describe every outcome"):
    assertEquals(runKyo(describe("42")), "age: 42")
    assertEquals(runKyo(describe("abc")), "not a number: abc")
    assertEquals(runKyo(describe("-1")), "negative: -1")
    assertEquals(runKyo(describe("200")), "too old: 200")

  test("Ex 7: findUser"):
    assertEquals(Abort.run(findUser(users, "alice")).eval, Result.succeed("42"))
    assertEquals(Abort.run(findUser(users, "dave")).eval, Result.fail("unknown user: dave"))

  test("Ex 8: userAge reports both kinds of errors"):
    def run(name: String) = Abort.run[String | AgeError](userAge(users, name)).eval
    assertEquals(run("alice"), Result.succeed(42))
    assertEquals(run("bob"), Result.fail(AgeError.NotANumber("abc")))
    assertEquals(run("carol"), Result.fail(AgeError.Negative(-3)))
    assertEquals(run("dave"), Result.fail("unknown user: dave"))

  test("Ex 9: userAgeOrUnknown only handles unknown users"):
    assertEquals(result(userAgeOrUnknown(users, "alice")), Result.succeed(42))
    assertEquals(result(userAgeOrUnknown(users, "dave")), Result.succeed(-1))
    assertEquals(result(userAgeOrUnknown(users, "bob")), Result.fail(AgeError.NotANumber("abc")))
