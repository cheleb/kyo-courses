package course.intermediate.l06_scope

import kyo.*

/** I6 solutions. */
object Exercises:

  def connection(name: String, log: EventLog): Connection < (Scope & Sync) =
    Scope.acquireRelease(Connection.open(name, log))(_.close)

  def queryOnce(log: EventLog)(q: String): String < Async =
    Scope.run(connection("db", log).map(_.query(q)))

  def copy(log: EventLog): String < Async =
    Scope.run:
      for
        source <- connection("source", log)
        target <- connection("target", log)
        _      <- source.query("read")
        result <- target.query("write")
      yield result

  def queryThenFail(log: EventLog): String < (Async & Abort[String]) =
    Scope.run:
      for
        db <- connection("db", log)
        _  <- db.query("update")
        _  <- Abort.fail("boom")
      yield "unreachable"

  def slowQuery(log: EventLog): Maybe[String] < Async =
    val slow: String < Async =
      Scope.run:
        for
          db     <- connection("slow", log)
          _      <- Async.sleep(1.second)
          result <- db.query("select")
        yield result
    Abort.run[Timeout](Async.timeout(50.millis)(slow)).map(_.toMaybe)
  end slowQuery

  def tracked[A](log: EventLog)(v: A < (Async & Abort[String])): A < (Async & Abort[String]) =
    Scope.run:
      Scope
        .ensure:
          case Absent                         => log.add("outcome: success")
          case Present(Result.Failure(error)) => log.add(s"outcome: failure $error")
          case Present(Result.Panic(ex))      => log.add(s"outcome: panic ${ex.getMessage}")
        .andThen(v)

  def openAll(names: Seq[String], log: EventLog): Seq[Connection] < (Scope & Sync) =
    Kyo.foreach(names)(connection(_, log))

end Exercises
