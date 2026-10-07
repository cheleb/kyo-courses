package course.beginner.l06_var

import kyo.*

/** Instructions of the stack machine used in the exercises. */
enum Instr derives CanEqual:
  case Push(value: Int)
  case Add
  case Mul

/** # B6 — `Var`: local state
  *
  * `Var[V]` is the effect of a computation that reads and writes a value of type `V`. No mutable variable is
  * involved: the handler threads the state from one step to the next, so the same program can run many times, each
  * time from a fresh initial state.
  *
  * Key ideas of this lesson:
  *
  *   1. Reading: `Var.get[V]`, `Var.use[V](f)`.
  *   2. Writing: `Var.set(v)` returns the *previous* value, `Var.update(f)` returns the *new* one; the `Discard`
  *      variants (`setDiscard`, `updateDiscard`) return `Unit`.
  *   3. Running: `Var.run(initial)(program)` returns the result; `Var.runTuple` also returns the final state.
  *   4. `Var` combines with other effects, e.g. `Abort` to stop a stateful computation.
  *   5. Several `Var`s can coexist as long as their types differ: the type identifies the state.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.beginner.l06_var.VarLesson"
  * }}}
  */
object VarLesson extends KyoApp:

  // 1 & 2. A bank balance kept in a `Var[Int]`.
  def deposit(amount: Int): Int < Var[Int] = Var.update[Int](_ + amount)

  def withdraw(amount: Int): Int < (Var[Int] & Abort[String]) =
    for
      balance <- Var.get[Int]
      _       <- Abort.when(amount > balance)(s"insufficient funds: $balance < $amount")
      updated <- Var.update[Int](_ - amount)
    yield updated

  val session: Int < (Var[Int] & Abort[String]) =
    for
      _ <- deposit(100)
      _ <- withdraw(30)
      b <- withdraw(20)
    yield b

  // `set` returns the previous value: handy to "reset and report".
  val reset: Int < Var[Int] = Var.set(0)

  // 3. Running: the same program from two different initial states.
  val fromZero: Result[String, (Int, Int)] = Abort.run(Var.runTuple(0)(session)).eval
  val fromTen: Result[String, (Int, Int)]  = Abort.run(Var.runTuple(10)(session)).eval

  // 4. `Abort` stops the computation: the remaining steps never run.
  val overdraft: Result[String, Int] = Abort.run(Var.run(10)(withdraw(50))).eval

  // 5. Two independent states, distinguished by their types.
  def record(word: String): Unit < (Var[Int] & Var[List[String]]) =
    for
      _ <- Var.updateDiscard[Int](_ + 1)
      _ <- Var.updateDiscard[List[String]](word :: _)
    yield ()

  val recorded: (List[String], (Int, Unit)) =
    Var.runTuple(List.empty[String])(Var.runTuple(0)(Kyo.foreachDiscard(Seq("a", "b", "c"))(record))).eval

  run:
    for
      _ <- Console.printLine(s"from 0: $fromZero")
      _ <- Console.printLine(s"from 10: $fromTen")
      _ <- Console.printLine(s"overdraft: $overdraft")
      _ <- Console.printLine(s"reset returned ${Var.run(42)(reset).eval}")
      _ <- Console.printLine(s"recorded: $recorded")
    yield ()

end VarLesson
