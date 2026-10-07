package course.beginner.l05_env

import kyo.*

/** B5 solutions. */
object Exercises:

  def appName: String < Env[AppConfig] = Env.use[AppConfig](_.appName)

  def banner: String < Env[AppConfig] =
    Env.use[AppConfig](config => s"Welcome to ${config.appName} (max ${config.maxUsers} users)")

  def runBanner(config: AppConfig): String = Env.run(config)(banner).eval

  def findUser(id: Int): String < (Env[UserRepo] & Sync & Abort[String]) =
    for
      repo  <- Env.get[UserRepo]
      found <- repo.find(id)
      user  <- Abort.get(found.toRight(s"user $id not found"))
    yield user

  def register(id: Int, name: String): Unit < (Env[UserRepo] & Env[Logger] & Sync) =
    for
      repo   <- Env.get[UserRepo]
      logger <- Env.get[Logger]
      _      <- repo.save(id, name)
      _      <- logger.log(s"registered $name")
    yield ()

  def registerChecked(id: Int, name: String): Unit < (Env[AppConfig] & Env[UserRepo] & Env[Logger] & Sync &
    Abort[String]) =
    for
      config <- Env.get[AppConfig]
      repo   <- Env.get[UserRepo]
      count  <- repo.count
      _      <- Abort.when(count >= config.maxUsers)("too many users")
      _      <- register(id, name)
    yield ()

  def runRegisterChecked(config: AppConfig, repo: UserRepo, logger: Logger)(
      id: Int,
      name: String
  ): Unit < (Sync & Abort[String]) =
    Env.runAll(TypeMap(config, repo, logger))(registerChecked(id, name))

end Exercises
