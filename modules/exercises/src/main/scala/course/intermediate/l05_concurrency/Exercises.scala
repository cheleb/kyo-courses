package course.intermediate.l05_concurrency

import kyo.*

/** I5 exercises — replace every `???`, then run `sbt "exercises/testOnly course.intermediate.l05_concurrency.*"`.
  *
  * The `Account` class is defined in the lesson.
  */
object Exercises:

  /** Ex 1: start `n` concurrent tasks that each increment a shared counter once; return the final count. */
  def countConcurrently(n: Int): Int < Async = ???

  /** Ex 2: offer every item, in order, to a queue of the given capacity (nobody takes from it). Return the accepted
    * and the rejected items. Hint: `Queue.use`, `offer`. (Remember: the capacity is rounded up to a power of two.)
    */
  def offerAll(capacity: Int, items: Seq[Int]): (Seq[Int], Seq[Int]) < (Sync & Abort[Closed]) = ???

  /** Ex 3: a producer fiber puts every item into a channel of capacity 4, then closes it *without losing* buffered
    * items; meanwhile the consumer sums everything it receives. Return the sum.
    */
  def sumThroughChannel(items: Seq[Int]): Int < (Async & Scope & Abort[Closed]) = ???

  /** Ex 4: run every task concurrently, but never more than `max` at the same time. Keep the order of the results.
    * Hint: `Meter.useSemaphore`.
    */
  def runLimited[A](max: Int)(tasks: Seq[A < Async]): Seq[A] < (Async & Abort[Closed]) = ???

  /** Ex 5: deposit all amounts into `account` *concurrently*, using `account.unsafeDeposit`, without losing any.
    * Hint: a mutex.
    */
  def depositAll(account: Account, amounts: Seq[Long]): Unit < (Async & Abort[Closed]) = ???

  /** Ex 6: start `n` worker fibers; worker `i` (from 1 to `n`) sleeps `(n - i) * 5` milliseconds, then records `i`
    * in a shared list. Wait until *all* workers are done (hint: `Latch`) and return the recorded ids, in the order
    * they were recorded.
    */
  def waitForWorkers(n: Int): List[Int] < (Async & Scope) = ???

  /** Ex 7: publish every message on a hub to `listeners` subscribers; return what each subscriber received. */
  def broadcast(messages: Seq[String], listeners: Int): Seq[Seq[String]] < (Async & Scope & Abort[Closed]) = ???

end Exercises
