package course.intermediate.l07_layer

import kyo.*

/** I7 exercises — replace every `???`, then run `sbt "exercises/testOnly course.intermediate.l07_layer.*"`.
  *
  * The components (`Config`, `Database`, `UserRepo`, `Audit`, `UserService`) are defined in the lesson.
  */
object Exercises:

  /** Ex 1: a layer producing `Config(url, poolSize = 4)`. */
  def configLayer(url: String): Layer[Config, Any] = ???

  /** Ex 2: a layer opening a `Database` from the `Config`. */
  def databaseLayer: Layer[Database, Env[Config] & Sync] = ???

  /** Ex 3: a layer building a `UserRepo` from the `Database`. */
  def repoLayer: Layer[UserRepo, Env[Database]] = ???

  /** Ex 4: a layer building an `Audit` from the `Database`. */
  def auditLayer: Layer[Audit, Env[Database]] = ???

  /** Ex 5: a layer building a `UserService` from a `UserRepo` and an `Audit`.
    *
    * Careful: the return type is ascribed here, so pass `Layer.from`'s type parameters explicitly (see the pitfall in
    * the lesson).
    */
  def serviceLayer: Layer[UserService, Env[UserRepo & Audit]] = ???

  /** Ex 6: wire the whole application *manually*, with `to` and `and`. */
  def appLayer(url: String): Layer[UserService, Sync] = ???

  /** Ex 7: wire the whole application *automatically*, with `Layer.init`. Repo and audit must share one database:
    * remember that memoization is per layer instance, and the layers above are `def`s.
    */
  def autoLayer(url: String): Layer[UserService, Sync] = ???

  /** Ex 8: build the components of `layer` and run `program` with them. Hint: `layer.run`, `Memo.run`, `Env.runAll`.
    */
  def runApp[A](layer: Layer[UserService, Sync])(program: A < (Env[UserService] & Sync)): A < Sync = ???

end Exercises
