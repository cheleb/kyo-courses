package course.intermediate.l09_testing

import java.io.IOException
import kyo.*

/** I9 exercises — replace every `???`, then run `sbt "exercises/testOnly course.intermediate.l09_testing.*"`.
  *
  * You write your own testing toolkit. The tests use it on the code in `App` (see the lesson).
  */
object Exercises:

  /** Ex 1: run a `Sync` program and return its result (rethrowing any exception). Hint:
    * `Sync.Unsafe.evalOrThrow`, and `import AllowUnsafe.embrace.danger` to allow it.
    */
  def runSync[A](v: A < Sync): A = ???

  /** Ex 2: run an asynchronous program that may fail; return its outcome as a `Result`. Give up after 5 seconds.
    * Hint: `Abort.run`, then `KyoApp.Unsafe.runAndBlock`.
    */
  def runResult[E: ConcreteTag, A](v: A < (Async & Abort[E])): Result[E, A] = ???

  /** Ex 3: run `v` and return what it printed on the standard output. */
  def output(v: Any < Sync): String = ???

  /** Ex 4: run `v`, feeding it the given lines as console input. */
  def withInput[A](lines: Seq[String])(v: A < (Sync & Abort[IOException])): A = ???

  /** Ex 5: run `v` with a random generator seeded with `seed`. */
  def seeded[A](seed: Int)(v: A < Sync): A = ???

  /** Ex 6: `v` sleeps once, for at most `d`. Run it in *virtual* time: start it in a fiber, wait until it sleeps,
    * advance the clock by `d`, and return its result. Must take far less than `d` of real time.
    */
  def afterVirtualTime[A](d: Duration)(v: A < Async): A < Async = ???

  /** Ex 7: `v` sleeps once. Is it done once virtual time has advanced by `d`? Hint: `fiber.done`. */
  def doneWithin[A](d: Duration)(v: A < Async): Boolean < Async = ???

end Exercises
