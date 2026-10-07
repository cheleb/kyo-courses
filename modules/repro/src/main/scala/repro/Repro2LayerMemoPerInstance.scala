package repro

import java.util.concurrent.atomic.AtomicInteger
import kyo.*

/** Repro 2: `Layer.init` builds a shared dependency once per dependent when that dependency is a `def`.
  *
  * Run: sbt "repro/runMain repro.Repro2LayerMemoPerInstance"
  */
object Repro2LayerMemoPerInstance extends KyoApp:

  final class Db
  final class RepoA(val db: Db)
  final class RepoB(val db: Db)
  final class Service(val a: RepoA, val b: RepoB)

  val opened = AtomicInteger()

  def dbLayer: Layer[Db, Sync] = Layer(Sync.defer { opened.incrementAndGet(); Db() })

  val repoA   = Layer.from((db: Db) => RepoA(db))
  val repoB   = Layer.from((db: Db) => RepoB(db))
  val service = Layer.from[RepoA, RepoB, Service, Any]((a, b) => Service(a, b))

  def report(label: String, layer: Layer[Service, Sync]): Unit < Sync =
    for
      _   <- Sync.defer(opened.set(0))
      env <- Memo.run(layer.run)
      s    = env.get[Service]
      _   <- Console.printLine(s"$label: databases opened = ${opened.get}, repos share one = ${s.a.db eq s.b.db}")
    yield ()

  run:
    for
      _ <- report("def passed to Layer.init", Layer.init[Service](service, repoA, repoB, dbLayer))
      _ <- report("val passed to Layer.init", { val db = dbLayer; Layer.init[Service](service, repoA, repoB, db) })
      _ <- report("def, manual wiring      ", dbLayer to (repoA and repoB) to service)
    yield ()

end Repro2LayerMemoPerInstance
