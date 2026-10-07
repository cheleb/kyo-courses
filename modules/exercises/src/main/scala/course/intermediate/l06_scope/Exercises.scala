package course.intermediate.l06_scope

import kyo.*

/** I6 exercises — replace every `???`, then run `sbt "exercises/testOnly course.intermediate.l06_scope.*"`.
  *
  * `EventLog` and `Connection` are defined in the lesson.
  */
object Exercises:

  /** Ex 1: open a connection whose `close` is registered in the current scope. */
  def connection(name: String, log: EventLog): Connection < (Scope & Sync) = ???

  /** Ex 2: open a connection named `"db"`, run query `q` on it and return its result. The connection must be
    * closed before this computation ends: no `Scope` may remain in the type.
    */
  def queryOnce(log: EventLog)(q: String): String < Async = ???

  /** Ex 3: open `"source"` then `"target"`, run `"read"` on the source then `"write"` on the target, and return
    * the result of the write. Both connections must be closed, in reverse order of opening.
    */
  def copy(log: EventLog): String < Async = ???

  /** Ex 4: open `"db"`, run `"update"`, then fail with `"boom"`. The connection must still be closed. */
  def queryThenFail(log: EventLog): String < (Async & Abort[String]) = ???

  /** Ex 5: open `"slow"`, wait one second, then run `"select"` and return its result — but give up after 50
    * milliseconds, returning `Absent`. The connection must still be closed, and the query must never run.
    */
  def slowQuery(log: EventLog): Maybe[String] < Async = ???

  /** Ex 6: run `v` in its own scope and log how it ended, once it has ended:
    *   - `"outcome: success"`,
    *   - `"outcome: failure <error>"` for a typed failure,
    *   - `"outcome: panic <exception message>"` for a panic.
    *
    * Hint: `Scope.ensure` with a function, then a `match` on `Result.Failure` / `Result.Panic`.
    */
  def tracked[A](log: EventLog)(v: A < (Async & Abort[String])): A < (Async & Abort[String]) = ???

  /** Ex 7: open a connection for every name, in order, all registered in the current scope. */
  def openAll(names: Seq[String], log: EventLog): Seq[Connection] < (Scope & Sync) = ???

end Exercises
