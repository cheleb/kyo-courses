package course.advanced.l02_arrow_effect

import kyo.*
import kyo.kernel.*

// ----- Effects used by the exercises -----

/** Outputs a message. Input: the message (`Const[String]`); output: nothing (`Const[Unit]`). */
sealed trait Say extends ArrowEffect[Const[String], Const[Unit]]

/** Asks a question and gets a textual answer. */
sealed trait Ask extends ArrowEffect[Const[String], Const[String]]

/** # A2 — `ArrowEffect`: your first effects
  *
  * Every effect seen so far (`Abort`, `Var`, `Emit`, `Choice`...) is built on two kernel primitives. This lesson
  * builds new ones from scratch.
  *
  * ## Declaring an effect
  *
  * An effect is a *type*, never instantiated: a `sealed trait` extending `ArrowEffect[Input, Output]`. `Input` and
  * `Output` are type constructors describing one operation: what a request carries, and what the handler answers.
  * Two shapes cover most cases (from `kyo.kernel`):
  *   - `Const[X]`: always `X`, whatever the operation's type parameter;
  *   - `Id`: the type parameter itself (for polymorphic operations, see `Fetch` below).
  *
  * ## Performing an operation
  *
  * `ArrowEffect.suspend[A](Tag[E], input)` creates a `Suspend` node (A1): a request of type `Output[A] < E`. Nothing
  * happens until a handler answers it. By convention operations live in the effect's companion object.
  *
  * ## Handling it
  *
  * `ArrowEffect.handleCont(Tag[E], computation)(clause)` removes `E` from the row. For every request, the clause
  * receives the request's input and the *continuation*, i.e. "the rest of the program", as an `Arrow`:
  *   - `cont(answer)` resumes the program with the answer;
  *   - never calling `cont` stops the program there (early exit: that is how `Abort` works);
  *   - the clause may use other effects (`Sync`, `Abort`, even other custom effects): they end up in the result's row.
  *
  * The clause is a *polymorphic function* `[C] => (input, cont) => ...` because each request may have its own type
  * parameter `C`.
  *
  * Type inference tip: when a clause uses an effect that is not in the method's declared result type (e.g. a local
  * helper emitting with `Emit`), annotate the expected type, e.g. `val handled: A < (S & Emit[String]) = ...`.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.advanced.l02_arrow_effect.ArrowEffectLesson"
  * }}}
  */
object ArrowEffectLesson extends KyoApp:

  // A polymorphic effect: fetch a value of any type by key. Input: the key (`Const[String]`); output: the requested
  // type itself (`Id`).
  sealed trait Fetch extends ArrowEffect[Const[String], Id]

  object Fetch:
    def apply[A](key: String)(using Frame): A < Fetch = ArrowEffect.suspend[A](Tag[Fetch], key)

  object SayOps:
    def apply(message: String)(using Frame): Unit < Say = ArrowEffect.suspend[Any](Tag[Say], message)

    /** Handler 1: print every message. The handler's own effect (`Sync`) joins the row. */
    def runPrint[A, S](v: A < (Say & S))(using Frame): A < (S & Sync) =
      ArrowEffect.handleCont(Tag[Say], v)([C] => (message, cont) => Console.printLine(s"  say: $message").andThen(cont(())))

    /** Handler 2: ignore every message. */
    def runSilent[A, S](v: A < (Say & S))(using Frame): A < S =
      ArrowEffect.handleCont(Tag[Say], v)([C] => (_, cont) => cont(()))
  end SayOps

  // A program using the custom effects: it does not know how they will be interpreted.
  val program: Int < (Say & Fetch) =
    for
      _     <- SayOps("starting")
      count <- Fetch[Int]("count")
      name  <- Fetch[String]("name")
      _     <- SayOps(s"$name has $count items")
    yield count * 2

  // A handler for Fetch: values come from a map; a missing key stops the program with an Abort.
  def runFetch[A, S](values: Map[String, Any])(v: A < (Fetch & S)): A < (S & Abort[String]) =
    ArrowEffect.handleCont(Tag[Fetch], v)(
      [C] =>
        (key, cont) =>
          values.get(key) match
            case Some(value) => cont(value.asInstanceOf[C])
            case None        => Abort.fail(s"missing key: $key") // cont is never called: early exit
    )

  run:
    for
      _ <- Console.printLine(s"the program, unhandled: $program")
      r <- Abort.run(SayOps.runPrint(runFetch(Map("count" -> 21, "name" -> "kyo"))(program)))
      _ <- Console.printLine(s"printing handler: ${r.show}")
      s <- Abort.run(SayOps.runSilent(runFetch(Map("count" -> 1))(program)))
      _ <- Console.printLine(s"silent handler, missing key: ${s.show}")
    yield ()

end ArrowEffectLesson
