package course.advanced.l03_handlers

import kyo.*
import kyo.kernel.*

// ----- Effects used by the exercises (operations included: you write the handlers) -----

/** Writer: records messages. */
sealed trait Tell extends ArrowEffect[Const[String], Const[Unit]]
object Tell:
  def apply(message: String)(using Frame): Unit < Tell = ArrowEffect.suspend[Any](Tag[Tell], message)

/** Generator of fresh numbers. */
sealed trait Fresh extends ArrowEffect[Const[Unit], Const[Int]]
object Fresh:
  def next(using Frame): Int < Fresh = ArrowEffect.suspend[Any](Tag[Fresh], ())

/** Early exit with a reason. Built like Kyo's own `Abort.fail`: the output is `Const[Unit]`, and `suspendWith` maps
  * the answer to `Nothing`, so a handler is not supposed to resume a `Stop`.
  */
sealed trait Stop extends ArrowEffect[Const[String], Const[Unit]]
object Stop:
  def apply(reason: String)(using Frame): Nothing < Stop =
    ArrowEffect.suspendWith[Any](Tag[Stop], reason)(_ => throw IllegalStateException("a Stop was resumed"))

/** Non-determinism: a boolean coin with both sides explored. */
sealed trait Flip extends ArrowEffect[Const[Unit], Const[Boolean]]
object Flip:
  def apply()(using Frame): Boolean < Flip = ArrowEffect.suspend[Any](Tag[Flip], ())

/** # A3 — Writing handlers
  *
  * A2 used `handleCont`, the most general handler. The kernel offers specialised families, each with optional extra
  * arms:
  *
  * | handler                          | the clause gets                | resumes            | typical use            |
  * |----------------------------------|--------------------------------|--------------------|------------------------|
  * | `handleCont(tag, v)(clause)`     | input + continuation           | at most once       | general, early exit    |
  * | `handleLoop(tag, v)(clause)`     | input only                     | `Loop.continue(a)` | simple, no state       |
  * | `handleLoopState(tag, s, v)(..)` | state + input                  | `Loop.continue(s, a)` | `Var`, `Emit`...   |
  * | `handleContRepeated(tag, v)(..)` | input + continuation           | any number of times | `Choice`              |
  *
  * Extra arms:
  *   - `done`: transforms the final result when the computation completes (for `handleLoopState`, it also receives
  *     the final state: that is how `Var.runTuple` returns it);
  *   - `recover`: offered any exception thrown by the handled computation, may replace it with a result.
  *
  * In loop-style handlers, `Loop.done(result)` ends the whole region with `result` (an early exit), *bypassing*
  * `done`. This is how a handler stops a program without holding the continuation.
  *
  * Resuming several times (exploring alternatives) requires `handleContRepeated`, which is exactly how `Choice.run` is
  * implemented: `v.map(Chunk(_))`, then for each request, resume once per alternative and concatenate.
  *
  * WARNING: `handleCont` supports at most one resumption. Its documentation says a second one is refused, but in
  * practice (RC7) calling the continuation twice does not fail cleanly: on a small `Flip` example the program ran away
  * and died with an `OutOfMemoryError` after more than a minute. If a handler hangs or exhausts memory, check that a
  * multi-shot clause uses `handleContRepeated`.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.advanced.l03_handlers.HandlersLesson"
  * }}}
  */
object HandlersLesson extends KyoApp:

  // An effect used by the demos only: request the next line of input.
  sealed trait ReadLine extends ArrowEffect[Const[Unit], Const[String]]
  def readLine(using Frame): String < ReadLine = ArrowEffect.suspend[Any](Tag[ReadLine], ())

  // handleLoopState: answer from a list of lines; stop with Absent when there are none left (Loop.done bypasses
  // `done`); otherwise wrap the result in Present and report how many lines were left unread.
  def runLines[A, S](lines: List[String])(v: A < (ReadLine & S)): (Maybe[A], Int) < S =
    ArrowEffect.handleLoopState(Tag[ReadLine], lines, v)(
      [C] =>
        (remaining, _) =>
          remaining match
            case head :: tail => Loop.continue(tail, head)
            case Nil          => Loop.done((Maybe.empty[A], 0)),
      done = (remaining, a) => (Maybe(a), remaining.size)
    )

  val readTwo: String < ReadLine =
    for
      a <- readLine
      b <- readLine
    yield s"$a+$b"

  // handleLoop: a stateless handler. Every line read is "kyo".
  def runConstant[A, S](v: A < (ReadLine & S)): A < S =
    ArrowEffect.handleLoop(Tag[ReadLine], v)([C] => _ => Loop.continue("kyo"))

  // handleContRepeated: every line read is *both* "a" and "b" — explore all combinations.
  def runBoth[A, S](v: A < (ReadLine & S)): Chunk[A] < S =
    ArrowEffect.handleContRepeated(Tag[ReadLine], v.map(Chunk(_)))(
      [C] => (_, cont) => cont("a").map(first => cont("b").map(second => first.concat(second)))
    )

  // recover: an exception thrown while running the computation is turned into a result.
  def runSafely(v: String < ReadLine): String < Any =
    ArrowEffect.handleCont(Tag[ReadLine], v)(
      [C] => (_, cont) => cont("x"),
      done = a => a,
      recover = ex => Maybe(s"recovered from ${ex.getMessage}")
    )

  def showLines(r: (Maybe[String], Int)): String = s"(${r._1.show}, ${r._2} unread)"

  run:
    for
      _ <- Console.printLine(s"runLines(3 lines): ${showLines(runLines(List("1", "2", "3"))(readTwo).eval)}")
      _ <- Console.printLine(s"runLines(1 line):  ${showLines(runLines(List("1"))(readTwo).eval)}")
      _ <- Console.printLine(s"runConstant:       ${runConstant(readTwo).eval}")
      _ <- Console.printLine(s"runBoth:           ${runBoth(readTwo).eval}")
      _ <- Console.printLine(s"runSafely:         ${runSafely(readLine.map(_ => throw new RuntimeException("bug"))).eval}")
    yield ()

end HandlersLesson
