package course.advanced.l02_arrow_effect

import kyo.*
import kyo.kernel.*

/** A2 exercises — replace every `???`, then run `sbt "exercises/testOnly course.advanced.l02_arrow_effect.*"`.
  *
  * The effects `Say` and `Ask` are declared in the lesson.
  */
object Exercises:

  /** Ex 1: the `Say` operation. Hint: `ArrowEffect.suspend[Any](Tag[Say], ...)`. */
  def say(message: String)(using Frame): Unit < Say = ???

  /** Ex 2: the `Ask` operation. */
  def ask(question: String)(using Frame): String < Ask = ???

  /** Ex 3: a program using both effects: say `"hello"`, ask `"name?"`, then say `"nice to meet you <name>"`, and
    * return the name.
    */
  def conversation(using Frame): String < (Say & Ask) = ???

  /** Ex 4: handle `Say` by printing each message on the console as `"> <message>"`. */
  def runSayConsole[A, S](v: A < (Say & S))(using Frame): A < (S & Sync) = ???

  /** Ex 5: handle `Ask` by answering every question with `answer`. */
  def runAskConstant[A, S](answer: String)(v: A < (Ask & S))(using Frame): A < S = ???

  /** Ex 6: handle `Ask` with scripted answers. A question without an answer stops the program with
    * `"no answer for: <question>"`.
    */
  def runAskScripted[A, S](answers: Map[String, String])(v: A < (Ask & S))(using Frame): A < (S & Abort[String]) =
    ???

  /** Ex 7: a handler *translating* one effect into another: each question is said (with `say`) as
    * `"? <question>"`, and answered with `"unknown"`. `Say` remains to be handled.
    */
  def runAskViaSay[A, S](v: A < (Ask & S))(using Frame): A < (S & Say) = ???

end Exercises
