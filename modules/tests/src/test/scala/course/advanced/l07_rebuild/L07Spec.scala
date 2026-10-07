package course.advanced.l07_rebuild

import course.KyoSuite
import kyo.*
import Exercises.*

class L07Spec extends KyoSuite:

  test("Ex 1 + 2: abortRun"):
    assertEquals(abortRun(abortFail("boom")).eval, Left("boom"))
    assertEquals(abortRun[String, Int, Any](42).eval, Right(42))

  test("Ex 2: abortRun stops the rest of the program"):
    val program = MiniEmit.value("before").andThen(abortFail("boom")).andThen(MiniEmit.value("after"))
    assertEquals(MiniEmit.run(abortRun(program)).eval, (Chunk("before"), Left("boom")))

  test("Ex 2: two error types, handled one at a time"):
    val program: Int < (MiniAbort[String] & MiniAbort[Int]) = abortFail(404)
    assertEquals(abortRun[String, Either[Int, Int], Any](abortRun[Int, Int, MiniAbort[String]](program)).eval, Right(Left(404)))

  test("Ex 3: abortRecover"):
    assertEquals(abortRecover((e: String) => e.length)(abortFail("four")).eval, 4)
    assertEquals(abortRecover((e: String) => e.length)(7).eval, 7)

  test("Ex 4 + 7: varGet"):
    assertEquals(varRunTuple(5)(varGet[Int]).eval, (5, 5))

  test("Ex 5: varSet returns the previous value"):
    assertEquals(varRunTuple(1)(varSet(9)).eval, (9, 1))

  test("Ex 6: varUpdate returns the new value"):
    assertEquals(varRunTuple(1)(varUpdate[Int](_ + 10)).eval, (11, 11))

  test("Ex 7: a stack machine-like sequence, like the real Var"):
    val mini =
      for
        a <- varUpdate[Int](_ + 1)
        b <- varSet(100)
        c <- varGet[Int]
      yield (a, b, c)
    val real =
      for
        a <- Var.update[Int](_ + 1)
        b <- Var.set(100)
        c <- Var.get[Int]
      yield (a, b, c)
    assertEquals(varRunTuple(0)(mini).eval, Var.runTuple(0)(real).eval)

  test("Ex 7: two MiniVars of different types do not mix"):
    val program =
      for
        _ <- varUpdate[Int](_ + 1)
        _ <- varUpdate[String](_ + "!")
      yield ()
    assertEquals(varRunTuple("hi")(varRunTuple(1)(program)).eval, ("hi!", (2, ())))

  test("handler order with the rebuilt effects matches I1"):
    val program = varUpdate[Int](_ + 1).andThen(abortFail("boom"))
    // MiniAbort outside: the state is lost.
    assertEquals(abortRun(varRunTuple(0)(program)).eval, Left("boom"))
    // MiniAbort inside: the state is kept.
    assertEquals(varRunTuple(0)(abortRun(program)).eval, (1, Left("boom")))
