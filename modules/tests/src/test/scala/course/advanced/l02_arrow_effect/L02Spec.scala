package course.advanced.l02_arrow_effect

import course.KyoSuite
import kyo.*
import kyo.kernel.*
import Exercises.*

class L02Spec extends KyoSuite:

  /** Reference handlers, independent from the exercises, used to test the operations. */
  def collectSay[A, S](v: A < (Say & S)): (List[String], A) < S =
    val emitting: A < (S & Emit[String]) =
      ArrowEffect.handleCont(Tag[Say], v)([C] => (m, cont) => Emit.value(m).andThen(cont(())))
    Emit.run[String](emitting).map((said, a) => (said.toList, a))

  def echoAsk[A, S](v: A < (Ask & S)): A < S =
    ArrowEffect.handleCont(Tag[Ask], v)([C] => (q, cont) => cont(s"answer to $q"))

  test("Ex 1: say suspends a Say request"):
    assertEquals(collectSay(say("hi")).eval, (List("hi"), ()))

  test("Ex 2: ask suspends an Ask request"):
    assertEquals(echoAsk(ask("why?")).eval, "answer to why?")

  test("Ex 3: conversation"):
    assertEquals(
      collectSay(echoAsk(conversation)).eval,
      (List("hello", "nice to meet you answer to name?"), "answer to name?")
    )

  test("Ex 4: runSayConsole prints"):
    assertEquals(stdOut(runSayConsole(say("a").andThen(say("b")))), "> a\n> b\n")

  test("Ex 5: runAskConstant"):
    assertEquals(collectSay(runAskConstant("Ada")(conversation)).eval._2, "Ada")

  test("Ex 6: runAskScripted answers from the script"):
    val r = Abort.run(collectSay(runAskScripted(Map("name?" -> "Grace"))(conversation))).eval
    assertEquals(r, Result.succeed((List("hello", "nice to meet you Grace"), "Grace")))

  test("Ex 6: runAskScripted stops on a missing answer"):
    val r = Abort.run(collectSay(runAskScripted(Map.empty)(conversation))).eval
    assertEquals(r, Result.fail("no answer for: name?"))

  test("Ex 6: runAskScripted stops *before* the rest of the program"):
    // Abort handled inside the collector (see I1): the messages said before the failure survive.
    val (said, result) = collectSay(Abort.run(runAskScripted(Map.empty)(conversation))).eval
    assertEquals(said, List("hello"))
    assertEquals(result, Result.fail("no answer for: name?"))

  test("Ex 7: runAskViaSay translates questions into messages"):
    assertEquals(
      collectSay(runAskViaSay(conversation)).eval,
      (List("hello", "? name?", "nice to meet you unknown"), "unknown")
    )
