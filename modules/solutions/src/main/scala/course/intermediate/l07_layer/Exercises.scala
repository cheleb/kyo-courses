package course.intermediate.l07_layer

import kyo.*

/** I7 solutions. */
object Exercises:

  def configLayer(url: String): Layer[Config, Any] = Layer(Config(url, poolSize = 4))

  def databaseLayer: Layer[Database, Env[Config] & Sync] = Layer.from((c: Config) => Database.open(c))

  def repoLayer: Layer[UserRepo, Env[Database]] = Layer.from((db: Database) => UserRepo(db))

  def auditLayer: Layer[Audit, Env[Database]] = Layer.from((db: Database) => Audit(db))

  def serviceLayer: Layer[UserService, Env[UserRepo & Audit]] =
    Layer.from[UserRepo, Audit, UserService, Any]((repo, audit) => UserService(repo, audit))

  def appLayer(url: String): Layer[UserService, Sync] =
    configLayer(url) to databaseLayer to (repoLayer and auditLayer) to serviceLayer

  def autoLayer(url: String): Layer[UserService, Sync] =
    // Memoization is per layer *instance*: `databaseLayer` is a `def`, so bind it once to share one database.
    val database = databaseLayer
    Layer.init[UserService](serviceLayer, auditLayer, repoLayer, database, configLayer(url))

  def runApp[A](layer: Layer[UserService, Sync])(program: A < (Env[UserService] & Sync)): A < Sync =
    Memo.run(layer.run.map(env => Env.runAll(env)(program)))

end Exercises
