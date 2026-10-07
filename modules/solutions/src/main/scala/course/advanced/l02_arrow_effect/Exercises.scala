package course.advanced.l02_arrow_effect

import kyo.*
import kyo.kernel.*

/** A2 solutions. */
object Exercises:

  def say(message: String)(using Frame): Unit < Say = ArrowEffect.suspend[Any](Tag[Say], message)

  def ask(question: String)(using Frame): String < Ask = ArrowEffect.suspend[Any](Tag[Ask], question)

  def conversation(using Frame): String < (Say & Ask) =
    for
      _    <- say("hello")
      name <- ask("name?")
      _    <- say(s"nice to meet you $name")
    yield name

  def runSayConsole[A, S](v: A < (Say & S))(using Frame): A < (S & Sync) =
    ArrowEffect.handleCont(Tag[Say], v)([C] => (message, cont) => Console.printLine(s"> $message").andThen(cont(())))

  def runAskConstant[A, S](answer: String)(v: A < (Ask & S))(using Frame): A < S =
    ArrowEffect.handleCont(Tag[Ask], v)([C] => (_, cont) => cont(answer))

  def runAskScripted[A, S](answers: Map[String, String])(v: A < (Ask & S))(using Frame): A < (S & Abort[String]) =
    ArrowEffect.handleCont(Tag[Ask], v)(
      [C] =>
        (question, cont) =>
          answers.get(question) match
            case Some(answer) => cont(answer)
            case None         => Abort.fail(s"no answer for: $question")
    )

  def runAskViaSay[A, S](v: A < (Ask & S))(using Frame): A < (S & Say) =
    ArrowEffect.handleCont(Tag[Ask], v)([C] => (question, cont) => say(s"? $question").andThen(cont("unknown")))

end Exercises
