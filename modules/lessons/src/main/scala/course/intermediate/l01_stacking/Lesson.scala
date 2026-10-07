package course.intermediate.l01_stacking

import kyo.*

/** # I1 — Stacking effects and handler order
  *
  * A computation can use any number of effects: `A < (Var[Int] & Abort[String] & Env[Config] & Sync)`. Each handler
  * removes one effect from the row, and handlers can be applied in any order. Usually the order does not matter, but
  * sometimes it changes the *meaning* of the program.
  *
  * Key ideas of this lesson:
  *
  *   1. `Env` handlers commute: providing A then B or B then A gives the same program.
  *   2. `Var` + `Abort` do not commute:
  *      - `Abort.run(Var.runTuple(s)(v))`: the failure escapes the `Var` handler, so the state is *lost*:
  *        `Result[E, (S, A)]`.
  *      - `Var.runTuple(s)(Abort.run(v))`: the failure is caught *inside* the state, which is *kept* as it was when
  *        the failure happened: `(S, Result[E, A])`.
  *   3. Where you handle an effect matters too: `Abort.run` around each step of a loop lets the loop continue;
  *      around the whole loop, the first failure stops everything.
  *   4. Effect rows are types: `<` is contravariant in its effects, so `A < Abort[Nothing]` fits where
  *      `A < Abort[String]` is expected (and `Sync` is an `Abort[Nothing]`).
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.intermediate.l01_stacking.StackingLesson"
  * }}}
  */
object StackingLesson extends KyoApp:

  // A computation that changes the state, then fails.
  val program: Int < (Var[Int] & Abort[String]) =
    for
      _ <- Var.update[Int](_ + 1)
      _ <- Var.update[Int](_ + 1)
      _ <- Abort.fail("boom")
    yield 42

  // 2. Abort outside: the state is lost with the failure.
  val abortOutside: Result[String, (Int, Int)] = Abort.run(Var.runTuple(0)(program)).eval

  // 2. Abort inside: the state survives, frozen at the moment of the failure.
  val abortInside: (Int, Result[String, Int]) = Var.runTuple(0)(Abort.run(program)).eval

  // 1. Env handlers commute.
  final case class Prefix(value: String)
  final case class Suffix(value: String)

  val decorated: String < (Env[Prefix] & Env[Suffix]) =
    for
      p <- Env.get[Prefix]
      s <- Env.get[Suffix]
    yield s"${p.value}kyo${s.value}"

  val prefixFirst: String = Env.run(Prefix("<"))(Env.run(Suffix(">"))(decorated)).eval
  val suffixFirst: String = Env.run(Suffix(">"))(Env.run(Prefix("<"))(decorated)).eval

  // 3. Handling inside or outside a loop.
  def check(n: Int): Int < Abort[String] =
    if n < 0 then Abort.fail(s"negative: $n") else n

  val numbers = Seq(1, -2, 3, -4)

  // Around the whole loop: the first failure wins, nothing else is returned.
  val wholeLoop: Result[String, Seq[Int]] = Abort.run(Kyo.foreach(numbers)(check)).eval

  // Around each step: every element gets its own Result.
  val eachStep: Seq[Result[String, Int]] = Kyo.foreach(numbers)(n => Abort.run(check(n))).eval

  // 4. Fewer effects fit where more are allowed: `fallback` has no typed failure but is accepted as one.
  val fallback: Int < Abort[Nothing]       = 0
  val widened: Int < Abort[String]         = fallback
  val alsoWidened: Int < (Sync & Abort[String]) = Sync.defer(1)

  run:
    for
      _ <- Console.printLine(s"Abort outside Var: ${abortOutside.show}")
      _ <- Console.printLine(s"Abort inside Var:  (${abortInside._1}, ${abortInside._2.show})")
      _ <- Console.printLine(s"Env order: $prefixFirst == $suffixFirst")
      _ <- Console.printLine(s"whole loop: ${wholeLoop.show}")
      _ <- Console.printLine(s"each step:  ${eachStep.map(_.show)}")
    yield ()

end StackingLesson
