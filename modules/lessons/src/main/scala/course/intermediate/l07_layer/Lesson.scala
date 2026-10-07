package course.intermediate.l07_layer

import java.util.concurrent.atomic.AtomicInteger
import kyo.*

// ----- The application's components -----

final case class Config(url: String, poolSize: Int)

/** Each database gets a unique id, so that we can tell whether two components share the same instance. */
final class Database private (val url: String, val id: Int):
  def execute(sql: String): String < Sync = Sync.defer(s"[$url#$id] $sql")

object Database:
  private val ids = AtomicInteger()
  def open(config: Config): Database < Sync = Sync.defer(new Database(config.url, ids.incrementAndGet()))

final class UserRepo(val db: Database):
  def save(name: String): String < Sync = db.execute(s"insert $name")

final class Audit(val db: Database):
  def record(event: String): String < Sync = db.execute(s"audit $event")

final class UserService(val repo: UserRepo, val audit: Audit):
  def register(name: String): List[String] < Sync =
    for
      saved   <- repo.save(name)
      audited <- audit.record(s"registered $name")
    yield List(saved, audited)

/** # I7 — `Layer`: wiring an application
  *
  * B5 provided dependencies one by one with `Env.run`. A real application has a *graph* of components: the
  * service needs a repository and an audit trail, which both need the database, which needs the configuration.
  * A `Layer[Out, S]` is a recipe that builds an `Out`, possibly needing other components (`Env[In]` in `S`) and
  * other effects.
  *
  * Key ideas of this lesson:
  *
  *   1. Creating layers: `Layer(value)` from a computation; `Layer.from(f)` from a function whose parameters are the
  *      dependencies (`Layer.from((c: Config) => Database.open(c))` is a `Layer[Database, Env[Config] & Sync]`).
  *   2. Composing manually:
  *      - `a to b`: feed `a`'s output to `b`; the result produces only `b`'s output;
  *      - `a and b`: two independent layers, producing both outputs;
  *      - `a using b`: like `to`, but keeps `a`'s output too.
  *   3. Composing automatically: `Layer.init[Target](layers*)` resolves the graph at compile time, and reports
  *      missing dependencies as compile errors.
  *   4. Running: `layer.run` builds the components (a `TypeMap`) and `Env.runAll` provides them; or
  *      `Env.runLayer(layers*)(program)` does the wiring and the providing in one step.
  *   5. Memoization: a layer used by several components is built *once*, so they share the same instance. This is
  *      the job of the `Memo` effect that building layers adds: handle it with `Memo.run`.
  *   6. Pitfall: let the lambda drive `Layer.from`'s type inference (see `service` below).
  *   7. Pitfall: memoization is per layer *instance*. A layer defined with `def` creates a new instance at every
  *      call, so `Layer.init(..., databaseLayer, ...)` with a `def databaseLayer` may build the database once per
  *      dependent. Define layers as `val`s, or bind them to a `val` before wiring.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.intermediate.l07_layer.LayerLesson"
  * }}}
  */
object LayerLesson extends KyoApp:

  // 1. Individual layers.
  val config: Layer[Config, Any]                      = Layer(Config("jdbc:kyo", poolSize = 4))
  val database: Layer[Database, Env[Config] & Sync]   = Layer.from((c: Config) => Database.open(c))
  val repo: Layer[UserRepo, Env[Database]]            = Layer.from((db: Database) => UserRepo(db))
  val audit: Layer[Audit, Env[Database]]              = Layer.from((db: Database) => Audit(db))

  // Pitfall: do NOT ascribe the type of a multi-dependency `Layer.from`, e.g.
  //   val service: Layer[UserService, Env[UserRepo & Audit]] = Layer.from((r: UserRepo, a: Audit) => ...)
  // Scala then infers the type parameters from the expected `Env[A & B]`, picks `A = UserRepo & Audit`, and the
  // layer fails *at runtime* looking up that intersection. Let the lambda drive inference, or pass the type
  // parameters explicitly: `Layer.from[UserRepo, Audit, UserService, Any](...)`.
  val service = Layer.from((r: UserRepo, a: Audit) => UserService(r, a))

  // 2. Manual wiring: config -> database -> (repo and audit) -> service.
  val manual: Layer[UserService, Sync] = config to database to (repo and audit) to service

  // 3. Automatic wiring: the order of the arguments does not matter.
  val automatic = Layer.init[UserService](service, audit, repo, database, config)

  // The program only knows it needs a UserService.
  val program: (List[String], Boolean) < (Env[UserService] & Sync) =
    Env.use[UserService]: s =>
      s.register("alice").map(lines => (lines, s.repo.db eq s.audit.db))

  // 4. Running with the manually wired layer...
  val viaManual: (List[String], Boolean) < Sync = Memo.run(manual.run.map(env => Env.runAll(env)(program)))

  // ... or let Env.runLayer wire and run in one step.
  val viaRunLayer: (List[String], Boolean) < Sync =
    Memo.run(Env.runLayer(service, audit, repo, database, config)(program))

  run:
    for
      r1 <- viaManual
      _  <- Console.printLine(s"manual: ${r1._1.mkString(" | ")}")
      r2 <- viaRunLayer
      _  <- Console.printLine(s"runLayer: ${r2._1.mkString(" | ")}")
      // 5. Memoization: repo and audit share the same database.
      _ <- Console.printLine(s"repo and audit share one database: ${r2._2}")
    yield ()

end LayerLesson
