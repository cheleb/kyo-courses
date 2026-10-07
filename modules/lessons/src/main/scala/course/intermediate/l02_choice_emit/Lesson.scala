package course.intermediate.l02_choice_emit

import kyo.*

/** # I2 — `Choice` and `Emit`
  *
  * Two effects that change the *shape* of a computation:
  *
  *   - `Choice` makes a computation *non-deterministic*: `Choice.eval(1, 2, 3)` continues the rest of the program
  *     once per value. `Choice.run` collects the results of every branch in a `Chunk`. Branches can be pruned with
  *     `Choice.dropIf(cond)` / `Choice.drop`. It is a for-comprehension over "all possibilities", ideal for search
  *     problems.
  *   - `Emit[V]` lets a computation *output* values while it runs, like a generator or a log. `Emit.run` collects
  *     them; `Emit.runFold`, `Emit.runForeach` and `Emit.runDiscard` consume them in other ways.
  *
  * Combined with state, the handler order matters again (see I1): a `Var` handled *inside* `Choice.run` is copied
  * into every branch, while a `Var` handled *outside* is shared by all branches, one after the other.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.intermediate.l02_choice_emit.ChoiceEmitLesson"
  * }}}
  */
object ChoiceEmitLesson extends KyoApp:

  // ----- Choice -----

  // Every combination of a size and a color.
  val variants: String < Choice =
    for
      size  <- Choice.eval("S", "M", "L")
      color <- Choice.eval("red", "blue")
    yield s"$size-$color"

  // Pruning: only the dice rolls that add up to 7.
  val sevens: (Int, Int) < Choice =
    for
      a <- Choice.evalSeq(1 to 6)
      b <- Choice.evalSeq(1 to 6)
      _ <- Choice.dropIf(a + b != 7)
    yield (a, b)

  // Choice mixes with other effects: here a branch can also fail.
  def safeDiv(n: Int): Int < (Choice & Abort[String]) =
    Choice.eval(2, 1, 0).map(d => if d == 0 then Abort.fail("division by zero") else n / d)

  // Var inside Choice.run: every branch starts from its own copy of the state.
  val varInside: Chunk[Int] =
    Choice.run(Var.run(0)(Choice.eval(1, 2, 3).map(n => Var.update[Int](_ + n)))).eval

  // Var outside Choice.run: one state threads through all branches in turn.
  val varOutside: (Int, Chunk[Int]) =
    Var.runTuple(0)(Choice.run(Choice.eval(1, 2, 3).map(n => Var.update[Int](_ + n)))).eval

  // ----- Emit -----

  // A generator: the squares below a limit. The result of the computation itself is the count.
  def squares(limit: Int): Int < Emit[Int] =
    Kyo.foreachDiscard(1 to limit)(n => Emit.value(n * n)).andThen(limit)

  // A computation that logs what it does.
  def fetch(id: Int): String < Emit[String] =
    for
      _ <- Emit.value(s"fetching $id")
      _ <- Emit.valueWhen(id > 100)(s"warning: large id $id")
    yield s"item-$id"

  run:
    for
      _ <- Console.printLine(s"variants: ${Choice.run(variants).eval}")
      _ <- Console.printLine(s"sevens: ${Choice.run(sevens).eval}")
      _ <- Console.printLine(s"each branch fails alone: ${Choice.run(Abort.run(safeDiv(10))).eval.map(_.show)}")
      _ <- Console.printLine(s"one failure fails all: ${Abort.run(Choice.run(safeDiv(10))).eval.show}")
      _ <- Console.printLine(s"Var inside Choice.run: $varInside")
      _ <- Console.printLine(s"Var outside Choice.run: $varOutside")
      _ <- Console.printLine(s"squares, collected: ${Emit.run(squares(5)).eval}")
      _ <- Console.printLine(s"squares, summed: ${Emit.runFold(0)((acc: Int, n: Int) => acc + n)(squares(5)).eval}")
      _ <- Emit.runForeach(fetch(101))(line => Console.printLine(s"  log: $line")).map(r => Console.printLine(s"fetched $r"))
    yield ()

end ChoiceEmitLesson
