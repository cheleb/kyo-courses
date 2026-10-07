package course.advanced.l08_capstone

import kyo.*
import kyo.kernel.*

// ----- The effect you implement in the capstone -----

/** The operations of a key-value store. Each operation is indexed by the type of its answer (a GADT). */
enum KVOp[A]:
  case Get(key: String)                extends KVOp[Maybe[String]]
  case Put(key: String, value: String) extends KVOp[Unit]
  case Delete(key: String)             extends KVOp[Boolean]

/** A key-value store effect. Input: `KVOp[A]`; output: `Id`, i.e. the answer has exactly the type `A` the operation
  * is indexed by.
  */
sealed trait KV extends ArrowEffect[KVOp, Id]

/** # A8 — Capstone: designing an effect
  *
  * The final capstone: design a complete effect, `KV`, with several interpreters, and use it in an application.
  *
  * ## Typed operations
  *
  * The effects so far had one operation (`Say`) or a fixed answer type (`MiniVar[V]` always answers `V`). A store has
  * operations answering *different* types: `get` a `Maybe[String]`, `put` a `Unit`, `delete` a `Boolean`. The
  * solution is an operation type indexed by its answer (`KVOp[A]`, a GADT) and `Id` as output:
  * `ArrowEffect.suspend[Maybe[String]](Tag[KV], KVOp.Get(key))` is a `Maybe[String] < KV`. In a handler, matching on
  * the operation tells the compiler what `C` is, but the answer usually needs `.asInstanceOf[C]`: Scala does not
  * always carry the GADT refinement through the polymorphic clause.
  *
  * ## Interpreters
  *
  * One program, many meanings: an in-memory store (state, `handleLoopState`), a read-only store (failures),
  * decorators that observe operations and *forward* them to the next handler.
  *
  * ## Forwarding
  *
  * A decorating handler re-performs each operation. Where it does so matters (A3):
  *   - in a `handleCont` clause, the kernel documents operations as *re-entrant*: they are answered by the same
  *     handler, which would intercept its own forwarded request again;
  *   - in a `handleLoop` / `handleLoopState` clause, the clause's own effects are answered *further out*. So the
  *     clause performs the operation and hands the answer back with `Loop.continue`.
  *
  * This lesson shows these techniques on a smaller effect, `Terminal`.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.advanced.l08_capstone.CapstoneLesson"
  * }}}
  */
object CapstoneLesson extends KyoApp:

  enum TermOp[A]:
    case ReadLine()           extends TermOp[String]
    case Print(text: String)  extends TermOp[Unit]

  sealed trait Terminal extends ArrowEffect[TermOp, Id]

  object Terminal:
    def readLine(using Frame): String < Terminal       = ArrowEffect.suspend[String](Tag[Terminal], TermOp.ReadLine())
    def print(text: String)(using Frame): Unit < Terminal = ArrowEffect.suspend[Unit](Tag[Terminal], TermOp.Print(text))

    /** A scripted terminal: input from a list, output collected. */
    def runScripted[A, S](input: List[String])(v: A < (Terminal & S))(using Frame): (List[String], A) < S =
      ArrowEffect.handleLoopState(Tag[Terminal], (input, List.empty[String]), v)(
        [C] =>
          (state, op) =>
            val (in, out) = state
            op match
              case TermOp.ReadLine()  => Loop.continue((in.drop(1), out), in.headOption.getOrElse("").asInstanceOf[C])
              case TermOp.Print(text) => Loop.continue((in, text :: out), ().asInstanceOf[C])
        ,
        done = (state, a) => (state._2.reverse, a)
      )

    /** A decorator: upper-cases everything printed, forwarding every operation to the next handler. */
    def shouting[A, S](v: A < (Terminal & S))(using Frame): A < (Terminal & S) =
      ArrowEffect.handleLoop(Tag[Terminal], v)(
        [C] =>
          op =>
            op match
              case TermOp.ReadLine()  => readLine.map(line => Loop.continue(line.asInstanceOf[C]))
              case TermOp.Print(text) => print(text.toUpperCase).map(_ => Loop.continue(().asInstanceOf[C]))
      )
  end Terminal

  val greet: String < Terminal =
    for
      _    <- Terminal.print("name?")
      name <- Terminal.readLine
      _    <- Terminal.print(s"hello $name")
    yield name

  run:
    for
      _ <- Console.printLine(s"scripted:          ${Terminal.runScripted(List("ada"))(greet).eval}")
      _ <- Console.printLine(s"shouting+scripted: ${Terminal.runScripted(List("ada"))(Terminal.shouting(greet)).eval}")
    yield ()

end CapstoneLesson
