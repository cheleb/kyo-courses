package course.beginner.l06_var

import kyo.*

/** B6 exercises — replace every `???`, then run `sbt "exercises/testOnly course.beginner.l06_var.*"`.
  *
  * The `Instr` enum is defined in the lesson.
  */
object Exercises:

  /** Ex 1: increment the counter and return its *new* value. */
  def next: Int < Var[Int] = ???

  /** Ex 2: replace the state with `value` and return the *previous* value. */
  def swap(value: Int): Int < Var[Int] = ???

  /** Ex 3: number each name using `next`: `Seq("alice", "bob")` gives `Seq("1. alice", "2. bob")` when the
    * counter starts at 0. Hint: `Kyo.foreach`.
    */
  def labels(names: Seq[String]): Seq[String] < Var[Int] = ???

  /** Ex 4: run `labels` with the counter starting at 0 and return the plain result. */
  def runLabels(names: Seq[String]): Seq[String] = ???

  /** Ex 5: call `next` `n` times, starting at `start`, and return the final state. Hint: `Var.runTuple`. */
  def finalCount(start: Int, n: Int): Int = ???

  /** Ex 6: execute one instruction on a stack kept in a `Var[List[Int]]` (head = top of the stack):
    *   - `Push(v)` pushes `v`;
    *   - `Add` / `Mul` pop two values and push their sum / product;
    *   - fail with `"stack underflow"` if there are not enough values.
    */
  def exec(instr: Instr): Unit < (Var[List[Int]] & Abort[String]) = ???

  /** Ex 7: execute every instruction on an empty stack and return the value on top of the stack at the end, or
    * fail with `"empty stack"` if there is none.
    */
  def evaluate(program: Seq[Instr]): Result[String, Int] = ???

end Exercises
