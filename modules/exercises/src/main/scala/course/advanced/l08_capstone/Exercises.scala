package course.advanced.l08_capstone

import kyo.*
import kyo.kernel.*

/** A8 capstone — replace every `???`, then run `sbt "exercises/testOnly course.advanced.l08_capstone.*"`.
  *
  * The effect `KV` and its operations `KVOp` are declared in the lesson.
  */
object Exercises:

  // ----- Operations -----

  /** Ex 1: the value of `key`, if any. */
  def get(key: String)(using Frame): Maybe[String] < KV = ???

  /** Ex 2: store `value` under `key`. */
  def put(key: String, value: String)(using Frame): Unit < KV = ???

  /** Ex 3: remove `key`; `true` if it existed. */
  def delete(key: String)(using Frame): Boolean < KV = ???

  /** Ex 4: a derived operation: increment the integer stored under `key` (a missing key counts as 0), store it, and
    * return the new value.
    */
  def increment(key: String)(using Frame): Int < KV = ???

  // ----- Interpreters -----

  /** Ex 5: an in-memory store starting from `initial`; return the final contents and the result. */
  def runInMemory[A, S](initial: Map[String, String])(v: A < (KV & S))(using Frame): (Map[String, String], A) < S =
    ???

  /** Ex 6: a read-only store over `data`: `get` works, `put` / `delete` fail with `"read-only: put <key>"` /
    * `"read-only: delete <key>"`.
    */
  def runReadOnly[A, S](data: Map[String, String])(v: A < (KV & S))(using Frame): A < (S & Abort[String]) = ???

  /** Ex 7: a decorator: emit `"get <key>"`, `"put <key>=<value>"` or `"delete <key>"` for every operation, and
    * forward it to the next `KV` handler.
    */
  def runLogged[A, S](v: A < (KV & S))(using Frame): A < (S & KV & Emit[String]) = ???

  /** Ex 8: a decorator counting the operations, forwarding them to the next `KV` handler. */
  def runCounted[A, S](v: A < (KV & S))(using Frame): (Int, A) < (S & KV) = ???

  // ----- Application -----

  /** Ex 9: record a visit for every user (key `"visits:<user>"`), then return the visit count of every distinct
    * user.
    */
  def visitAll(users: Seq[String])(using Frame): Map[String, Int] < KV = ???

end Exercises
