package course.beginner.l04_abort

import kyo.*

/** Errors used in this lesson and its exercises. With `strictEquality`, comparing values requires `CanEqual`. */
enum AgeError derives CanEqual:
  case NotANumber(input: String)
  case Negative(age: Int)
  case TooOld(age: Int)

/** # B4 — `Abort`: typed failures
  *
  * `Abort[E]` is the effect of a computation that may stop early with an error of type `E`. The error type appears
  * in the signature, so the compiler tracks which failures are still unhandled.
  *
  * Key ideas of this lesson:
  *
  *   1. Failing: `Abort.fail(e)`, `Abort.when(cond)(e)`, `Abort.get(either | option | result)`.
  *   2. Handling: `Abort.run` turns `A < Abort[E]` into a `Result[E, A]` and removes the effect.
  *      A `Result` is `Success(a)`, `Failure(e)` or `Panic(throwable)`.
  *   3. Several error types combine as a union, `Abort[E1 | E2]`, and can be handled one at a time.
  *   4. `Abort.recover` and `Abort.fold` handle errors without going through `Result`. They leave
  *      `Abort[Nothing]`: the channel of panics.
  *   5. Exceptions: `Abort.catching[Ex]` turns thrown exceptions into typed failures. Any *other* exception thrown
  *      in Kyo code becomes a `Panic`: an unexpected, untyped failure.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.beginner.l04_abort.AbortLesson"
  * }}}
  */
object AbortLesson extends KyoApp:

  // 1. Failing. `Abort.fail` has type `Nothing < Abort[E]`: it never produces a value.
  def withdraw(balance: Int, amount: Int): Int < Abort[String] =
    if amount > balance then Abort.fail(s"insufficient funds: $balance < $amount")
    else balance - amount

  // `Abort.when` fails when the condition holds, and otherwise continues with `()`.
  def deposit(balance: Int, amount: Int): Int < Abort[String] =
    for _ <- Abort.when(amount <= 0)(s"invalid amount: $amount")
    yield balance + amount

  // `Abort.get` lifts the usual "maybe a value" types: `Either`, `Option` (fails with `Absent`), `Result`, ...
  def lookup(accounts: Map[String, Int], name: String): Int < Abort[String] =
    Abort.get(accounts.get(name).toRight(s"no account for $name"))

  // 2. Handling. After `Abort.run`, no `Abort` is left: the result can be evaluated.
  val ok: Result[String, Int]     = Abort.run(withdraw(100, 30)).eval
  val failed: Result[String, Int] = Abort.run(withdraw(10, 30)).eval

  def show(r: Result[String, Int]): String = r match
    case Result.Success(v) => s"success: $v"
    case Result.Failure(e) => s"failure: $e"
    case Result.Panic(t)   => s"panic: ${t.getMessage}"

  // 3. Two error types: the union appears in the effect row.
  def parseAmount(input: String): Int < Abort[AgeError | String] =
    input.toIntOption match
      case None                => Abort.fail(AgeError.NotANumber(input))
      case Some(n) if n < 0    => Abort.fail(s"negative amount: $n")
      case Some(n)             => n

  // Handling only `String` errors leaves `Abort[AgeError]` in the type.
  val onlyAgeErrorsLeft: Result[String, Int] < Abort[AgeError] = Abort.run[String](parseAmount("-5"))

  // 4. Recovering with a fallback value, or folding both outcomes into one type.
  //    Typed failures are handled, but `Abort[Nothing]` remains: it is the channel for *panics*, which a
  //    handler for `String` errors does not touch. (`Sync` itself is a subtype of `Abort[Nothing]`.)
  val withFallback: Int < Abort[Nothing] = Abort.recover[String](_ => 0)(withdraw(10, 30))

  val folded: String < Abort[Nothing] =
    Abort.fold[String](balance => s"new balance: $balance", error => s"refused: $error")(withdraw(10, 30))

  // 5. Exceptions. `catching` selects the exception type to turn into a typed failure...
  def parseInt(input: String): Int < Abort[NumberFormatException] =
    Abort.catching[NumberFormatException](input.toInt)

  // ... optionally mapping it to a domain error.
  def parseIntOr(input: String): Int < Abort[AgeError] =
    Abort.catching[NumberFormatException](_ => AgeError.NotANumber(input))(input.toInt)

  // An exception nobody catches becomes a Panic.
  val boom: Int < Sync = Sync.defer(throw new IllegalStateException("boom"))

  run:
    for
      _ <- Console.printLine(show(ok))
      _ <- Console.printLine(show(failed))
      _ <- Console.printLine(show(Abort.run(deposit(10, -1)).eval))
      _ <- Console.printLine(show(Abort.run(lookup(Map("alice" -> 10), "bob")).eval))
      r <- Abort.run[AgeError](onlyAgeErrorsLeft)
      _ <- Console.printLine(s"nested results: $r")
      w <- withFallback
      f <- folded
      _ <- Console.printLine(s"recovered: $w, folded: $f")
      _ <- Console.printLine(s"parseInt: ${Abort.run(parseInt("x")).eval}")
      _ <- Console.printLine(s"parseIntOr: ${Abort.run(parseIntOr("x")).eval}")
      p <- Abort.run(boom)
      _ <- Console.printLine(show(p))
    yield ()

end AbortLesson
