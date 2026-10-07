package course.advanced.l01_pending_internals

import kyo.*

/** A1 exercises — replace every `???`, then run
  * `sbt "exercises/testOnly course.advanced.l01_pending_internals.*"`.
  */
object Exercises:

  /** Ex 1: are `A` and `B` the same type, according to their tags? */
  def sameType[A: Tag, B: Tag]: Boolean = ???

  /** Ex 2: is `A` a subtype of `B`, according to their tags? */
  def isSubtype[A: Tag, B: Tag]: Boolean = ???

  /** Ex 3: the caller's position, as `"<file name>:<line number>"`. */
  def callSite(using Frame): String = ???

  /** Ex 4: fail with `"<message> (at <file>:<line>)"` when `condition` is false, pointing at the *caller's* position.
    * Reuse `callSite`.
    */
  def check(condition: Boolean, message: String)(using Frame): Unit < Abort[String] = ???

  /** Ex 5: the sum `1 + 2 + ... + n`, written *recursively* (no loop, no formula): `sumTo(n - 1)` then add `n`.
    * It must not overflow the stack for `n = 1_000_000`: make every recursive step a deferred node.
    */
  def sumTo(n: Long): Long < Sync = ???

  /** Ex 6: remove every nesting level. */
  def flattenAll(v: Int < Sync < Sync < Sync): Int < Sync = ???

  /** Ex 7: handle every effect of `v` with a single `handle` pipeline: provide `env`, start the state at 0, and keep
    * the final state even when `v` fails. Return `(final state, result)`.
    */
  def runAll(env: Int)(v: Int < (Env[Int] & Abort[String] & Var[Int])): (Int, Result[String, Int]) = ???

end Exercises
