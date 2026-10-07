package course.intermediate.l06_scope

import java.util.concurrent.ConcurrentLinkedQueue
import kyo.*
import scala.jdk.CollectionConverters.*

/** Records what happens to resources, so that tests can check the order of events. */
final class EventLog:
  private val entries                   = ConcurrentLinkedQueue[String]()
  def add(entry: String): Unit < Sync   = Sync.defer(entries.add(entry)).unit
  def events: List[String] < Sync       = Sync.defer(entries.asScala.toList)

/** A fake database connection. Opening and closing it are side effects that are logged. */
final class Connection private (val name: String, log: EventLog):
  def query(q: String): String < Sync = log.add(s"$name: $q").andThen(s"result of $q")
  def close: Unit < Sync              = log.add(s"close $name")

object Connection:
  def open(name: String, log: EventLog): Connection < Sync =
    log.add(s"open $name").andThen(new Connection(name, log))

/** # I6 — `Scope` and resources
  *
  * Files, sockets, connections, fibers, queues: some values must be *released* after use, whatever happens —
  * success, typed failure, panic or interruption. Kyo provides two levels:
  *
  *   1. Local finalizers: `Sync.ensure(finalizer)(v)` runs `finalizer` after `v`, in every case.
  *   2. The `Scope` effect: `Scope.acquireRelease(acquire)(release)` returns the resource and registers its release
  *      in the current scope. `Scope.run(v)` delimits the scope: when `v` ends, every registered release runs, in
  *      *reverse* order of acquisition. `Scope.acquire(autoCloseable)` does the same for Java `AutoCloseable`s.
  *
  * `Scope` appears in the type until `Scope.run` handles it, so the compiler tells you that a value needs
  * releasing. This is why `Fiber.init`, `Queue.init` or `Channel.init` require `Scope` (I4, I5).
  *
  * `Scope.ensure(f)` registers a finalizer that also learns *how* the scope ended: `Absent` on success,
  * `Present(error)` on failure or panic.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.intermediate.l06_scope.ScopeLesson"
  * }}}
  */
object ScopeLesson extends KyoApp:

  def connection(name: String, log: EventLog): Connection < (Scope & Sync) =
    Scope.acquireRelease(Connection.open(name, log))(_.close)

  // Two resources, released in reverse order when the scope ends.
  def twoResources(log: EventLog): String < Async =
    Scope.run:
      for
        users  <- connection("users", log)
        orders <- connection("orders", log)
        u      <- users.query("select user")
        o      <- orders.query("select order")
      yield s"$u + $o"

  // A failure: resources are still released.
  def failing(log: EventLog): String < (Async & Abort[String]) =
    Scope.run:
      for
        db <- connection("db", log)
        _  <- db.query("update")
        _  <- Abort.fail("constraint violated")
      yield "unreachable"

  // An interruption: a timeout stops the computation, resources are still released.
  def interrupted(log: EventLog): Unit < (Async & Abort[Timeout]) =
    Async.timeout(50.millis):
      Scope.run:
        connection("slow", log).map(_ => Async.sleep(5.seconds))

  // A local finalizer, without Scope.
  def withFinalizer(log: EventLog): Int < Sync =
    Sync.ensure(log.add("finalizer ran"))(Sync.defer(42))

  // Scope.ensure: the finalizer is told how the scope ended.
  def outcome(log: EventLog)(v: Int < Abort[String]): Int < (Async & Abort[String]) =
    Scope.run:
      Scope
        .ensure(error => log.add(error.fold("scope ended: success")(e => s"scope ended: ${e.show}")))
        .andThen(v)

  def show(title: String)(program: EventLog => Any < (Async & Abort[Any])): Unit < (Async & Abort[Any]) =
    val log = EventLog()
    for
      result <- Abort.run[Any](program(log))
      events <- log.events
      _      <- Console.printLine(s"$title -> ${result.show}")
      _      <- Kyo.foreachDiscard(events)(e => Console.printLine(s"    $e"))
    yield ()
  end show

  run:
    for
      _ <- show("two resources")(twoResources)
      _ <- show("failure")(failing)
      _ <- show("interruption")(interrupted)
      _ <- show("local finalizer")(withFinalizer)
      _ <- show("ensure, success")(outcome(_)(1))
      _ <- show("ensure, failure")(outcome(_)(Abort.fail("oops")))
    yield ()

end ScopeLesson
