package course.intermediate.l09_testing

import java.io.IOException
import kyo.*

/** I9 solutions. */
object Exercises:
  import AllowUnsafe.embrace.danger

  def runSync[A](v: A < Sync): A = Sync.Unsafe.evalOrThrow(v)

  def runResult[E: ConcreteTag, A](v: A < (Async & Abort[E])): Result[E, A] =
    KyoApp.Unsafe.runAndBlock(5.seconds)(Abort.run[E](v)).getOrThrow

  def output(v: Any < Sync): String = runSync(Console.withOut(v))._1.stdOut

  def withInput[A](lines: Seq[String])(v: A < (Sync & Abort[IOException])): A =
    Sync.Unsafe.evalOrThrow(Console.withIn(lines)(v))

  def seeded[A](seed: Int)(v: A < Sync): A = runSync(Random.withSeed(seed)(v))

  /** Starts `v` in a fiber under virtual time, waits until it sleeps, then hands the fiber to `f`. */
  private def inVirtualTime[A, B](v: A < Async)(f: (Clock.TimeControl, Fiber[A, Any]) => B < Async): B < Async =
    Scope.run:
      Clock.withTimeControl: control =>
        for
          fiber  <- Fiber.init(v)
          _      <- control.awaitPendingSleepers(1)
          result <- f(control, fiber)
        yield result

  def afterVirtualTime[A](d: Duration)(v: A < Async): A < Async =
    inVirtualTime(v)((control, fiber) => control.advance(d).andThen(fiber.get))

  def doneWithin[A](d: Duration)(v: A < Async): Boolean < Async =
    inVirtualTime(v)((control, fiber) => control.advance(d).andThen(fiber.done))

end Exercises
