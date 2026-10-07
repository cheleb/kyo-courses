package repro

import kyo.*
import kyo.kernel.*

/** Repro 3: resuming a `handleCont` continuation twice is not refused; the evaluation runs away.
  *
  * Run: sbt "repro/runMain repro.Repro3HandleContMultiShot" (the module runs with -Xmx256m)
  */
object Repro3HandleContMultiShot:

  sealed trait Flip extends ArrowEffect[Const[Unit], Const[Boolean]]

  def flip(using Frame): Boolean < Flip = ArrowEffect.suspend[Any](Tag[Flip], ())

  /** Correct: multi-shot handler. */
  def runRepeated[A](v: A < Flip)(using Frame): List[A] < Any =
    ArrowEffect.handleContRepeated(Tag[Flip], v.map(List(_)))(
      [C] => (_, cont) => cont(true).map(t => cont(false).map(f => t ++ f))
    )

  /** Misuse: the same clause with the single-shot handler. */
  def runSingleShot[A](v: A < Flip)(using Frame): List[A] < Any =
    ArrowEffect.handleCont(Tag[Flip], v.map(List(_)))(
      [C] => (_, cont) => cont(true).map(t => cont(false).map(f => t ++ f))
    )

  val oneFlip: Boolean < Flip = flip
  val twoFlips: (Boolean, Boolean) < Flip =
    for
      a <- flip
      b <- flip
    yield (a, b)

  /** Evaluates `v` on a separate thread, reporting the outcome, a throwable, or a timeout. */
  def attempt(label: String, timeoutSeconds: Int)(v: => Any): Unit =
    val start  = System.nanoTime()
    val thread = Thread(() =>
      val outcome =
        try s"returned ${v}"
        catch case t: Throwable => s"threw ${t.getClass.getName}: ${t.getMessage}"
      println(f"$label%-28s $outcome%s after ${(System.nanoTime() - start) / 1_000_000}%d ms")
    )
    thread.setDaemon(true)
    thread.start()
    thread.join(timeoutSeconds * 1000L)
    if thread.isAlive then println(f"$label%-28s still running after $timeoutSeconds%d s")
  end attempt

  def main(args: Array[String]): Unit =
    attempt("handleContRepeated, 1 flip", 30)(runRepeated(oneFlip).eval)
    attempt("handleContRepeated, 2 flips", 30)(runRepeated(twoFlips).eval)
    attempt("handleCont, 1 flip", 30)(runSingleShot(oneFlip).eval)
    attempt("handleCont, 2 flips", 30)(runSingleShot(twoFlips).eval)
  end main

end Repro3HandleContMultiShot
