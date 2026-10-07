package course.advanced.l05_isolate

import kyo.*

/** A5 exercises — replace every `???`, then run `sbt "exercises/testOnly course.advanced.l05_isolate.*"`. */
object Exercises:

  /** Ex 1: run every task concurrently; afterwards, the state is the final state of the *last* task (in input
    * order).
    */
  def lastWins(tasks: Seq[Unit < (Var[Int] & Async)]): Unit < (Var[Int] & Async) = ???

  /** Ex 2: run every task concurrently, each on its own copy of the state, then throw their changes away. Return the
    * tasks' results, in order.
    */
  def sandboxed[A](tasks: Seq[A < (Var[Int] & Async)]): Seq[A] < (Var[Int] & Async) = ???

  /** Ex 3: each task adds something to the state; run them concurrently and keep *every* addition. The result must
    * be the state before the call plus the sum of all additions. Hint: merge deltas, not final values.
    */
  def sumAll(tasks: Seq[Unit < (Var[Int] & Async)]): Unit < (Var[Int] & Async) = ???

  /** Ex 4: run every task concurrently and collect everything they emit (in input order of the tasks). */
  def gatherMessages(tasks: Seq[Unit < (Emit[String] & Async)]): Chunk[String] < Async = ???

  /** Ex 5: tasks both update the state *and* emit messages. Run them concurrently, keeping the last task's state
    * (like Ex 1) and every message. Return the final state and the messages. Hint: compose isolates with `andThen`.
    */
  def both(start: Int)(tasks: Seq[Unit < (Var[Int] & Emit[String] & Async)]): (Int, Chunk[String]) < Async = ???

  /** Ex 6: run `v` in a background fiber; when it completes, the caller's state is the fiber's final state. Return
    * `v`'s result.
    */
  def inFiber[A](v: A < (Var[Int] & Async)): A < (Var[Int] & Async & Scope) = ???

end Exercises
