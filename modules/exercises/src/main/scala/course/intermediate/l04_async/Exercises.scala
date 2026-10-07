package course.intermediate.l04_async

import kyo.*

/** I4 exercises — replace every `???`, then run `sbt "exercises/testOnly course.intermediate.l04_async.*"`. */
object Exercises:

  /** Ex 1: wait 50 milliseconds, then return `n * 2`. */
  def slowDouble(n: Int): Int < Async = ???

  /** Ex 2: apply `slowDouble` to every number *concurrently*; keep the order of the input. */
  def doubleAll(xs: Seq[Int]): Seq[Int] < Async = ???

  /** Ex 3: run both computations concurrently and return both results. */
  def both[A, B](a: A < Async, b: B < Async): (A, B) < Async = ???

  /** Ex 4: each mirror answers with its name after its latency: return the name of the fastest one. */
  def fastest(mirrors: Seq[(String, Duration)]): String < Async = ???

  /** Ex 5: return the first attempt that *succeeds*, ignoring faster failures. If every attempt fails, fail. */
  def firstSuccess(attempts: Seq[String < (Async & Abort[String])]): String < (Async & Abort[String]) = ???

  /** Ex 6: the result of `v` if it completes within `d`, `Absent` otherwise. Hint: `Async.timeout`, `Timeout`. */
  def withTimeout[A](d: Duration)(v: A < Async): Maybe[A] < Async = ???

  /** Ex 7: start a background fiber that increments `count` every 10 milliseconds, forever. Let it run for `d`, then
    * interrupt it before returning: `count` must not change afterwards.
    */
  def tickFor(d: Duration, count: AtomicInt): Unit < (Async & Scope) = ???

end Exercises
