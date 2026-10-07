package course.advanced.l04_context

import kyo.*
import kyo.kernel.*

// ----- Effects used by the exercises -----

/** The current user: a *required* context value. */
sealed trait User extends ContextEffect[String]

/** The current nesting depth: read with a default (0). */
sealed trait Depth extends ContextEffect[Int]

/** The current tracing path (outermost span first): read with a default (empty).
  *
  * Not named `Span`: `kyo.Span` already exists, and the `import kyo.*` wildcard takes precedence over a type defined
  * in another file of the same package. Watch out for such clashes when naming your own effects.
  */
sealed trait Spans extends ContextEffect[List[String]]

/** A tree to render, for the exercises. */
final case class Tree(name: String, children: List[Tree] = Nil)

/** # A4 — `ContextEffect`
  *
  * The second kind of kernel effect. Where an `ArrowEffect` declares *operations*, a `ContextEffect[A]` declares a
  * *value* bound around a computation, which the computation can only read. `Env` is built on it (and so are `Local`,
  * `Log`'s current logger, `Scope`'s finalizers...).
  *
  *   1. Reading: `ContextEffect.suspend(Tag[E])` is a *required* read: `E` enters the row, so the program cannot run
  *      until a handler binds a value. `ContextEffect.suspend(Tag[E], default)` is a *defaulted* read: it adds nothing
  *      to the row and uses `default` when no handler is bound.
  *   2. Binding: `ContextEffect.handleInheritable(Tag[E], value)(v)` binds `value` for `v`. The overload with
  *      `(ifUndefined, ifDefined)` derives the value from an outer binding: this is how nested scopes accumulate
  *      (indentation, tracing spans, request metadata).
  *   3. Binding is *dynamic scoping*: the value is visible to everything that runs inside the handler, however deep,
  *      without being passed as a parameter, and the innermost handler wins.
  *   4. Fibers: an *inheritable* binding is handed to fibers forked inside it as it stands. A *non-inheritable* one
  *      (`handleNonInheritable`) makes a fork *start the region over*: the fork gets the value the handler would
  *      derive with no outer binding. For a constant binding that is the same value; the difference shows with a
  *      derived one (`ifUndefined`, `ifDefined`): inside `withVerbosity(3)`, a non-inheritable `louder` gives 4 to
  *      the current fiber but 2 (its `ifUndefined`) to a fork. Kyo's `Scope` uses this so that a child never
  *      inherits, and releases, its parent's resources.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.advanced.l04_context.ContextLesson"
  * }}}
  */
object ContextLesson extends KyoApp:

  // A context value of our own: the log level, with a default.
  sealed trait Verbosity extends ContextEffect[Int]

  val verbosity: Int < Any = ContextEffect.suspend(Tag[Verbosity], 1) // defaulted: no effect in the row

  def withVerbosity[A, S](level: Int)(v: A < S): A < S =
    ContextEffect.handleInheritable(Tag[Verbosity], level)(v)

  // Nested binding: each `louder` adds one to the outer value (or starts at 2 if none).
  def louder[A, S](v: A < S): A < S =
    ContextEffect.handleInheritable(Tag[Verbosity], 2, (outer: Int) => outer + 1)(v)

  def log(message: String): Unit < Sync =
    verbosity.map(level => Console.printLine(s"  [verbosity $level] $message"))

  // A required context value: the request id.
  sealed trait RequestId extends ContextEffect[String]
  val requestId: String < RequestId = ContextEffect.suspend(Tag[RequestId])

  def handleRequest(id: String)(v: String < (RequestId & Sync)): String < Sync =
    ContextEffect.handleInheritable(Tag[RequestId], id)(v)

  // Deep code reads the request id without receiving it as a parameter.
  val deepInTheCode: String < (RequestId & Sync) = requestId.map(id => s"processing request $id")

  // Inheritable vs non-inheritable bindings, seen from a forked fiber.
  def fromFork(v: Int < Any): Int < (Async & Scope) = Fiber.init(v).map(_.get)

  def louderLocal[A, S](v: A < S): A < S =
    ContextEffect.handleNonInheritable(Tag[Verbosity], 2, (outer: Int) => outer + 1)(v)

  val inheritedInFork: (Int, Int) < (Async & Scope) =
    withVerbosity(3)(louder(verbosity.map(here => fromFork(verbosity).map(fork => (here, fork)))))
  val notInheritedInFork: (Int, Int) < (Async & Scope) =
    withVerbosity(3)(louderLocal(verbosity.map(here => fromFork(verbosity).map(fork => (here, fork)))))

  run:
    for
      _ <- log("default")
      _ <- withVerbosity(3)(log("bound to 3"))
      _ <- louder(log("louder, no outer binding"))
      _ <- withVerbosity(3)(louder(louder(log("3, then louder twice"))))
      r <- handleRequest("req-42")(deepInTheCode)
      _ <- Console.printLine(s"  $r")
      i <- Scope.run(inheritedInFork)
      n <- Scope.run(notInheritedInFork)
      _ <- Console.printLine(s"  inheritable louder in verbosity 3:     here ${i._1}, in a fork ${i._2}")
      _ <- Console.printLine(s"  non-inheritable louder in verbosity 3: here ${n._1}, in a fork ${n._2}")
    yield ()

end ContextLesson
