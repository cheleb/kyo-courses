package course.beginner.l05_env

import kyo.*

/** B5 exercises — replace every `???`, then run `sbt "exercises/testOnly course.beginner.l05_env.*"`.
  *
  * The services `AppConfig`, `UserRepo` and `Logger` are defined in `Services.scala` in the lessons module.
  */
object Exercises:

  /** Ex 1: read the application name from the configuration. Hint: `Env.get` or `Env.use`. */
  def appName: String < Env[AppConfig] = ???

  /** Ex 2: build `"Welcome to <appName> (max <maxUsers> users)"`. */
  def banner: String < Env[AppConfig] = ???

  /** Ex 3: provide `config` to `banner` and return the plain `String`. Hint: `Env.run`, `.eval`. */
  def runBanner(config: AppConfig): String = ???

  /** Ex 4: find a user by id with the `UserRepo`; fail with `"user <id> not found"` if it does not exist. */
  def findUser(id: Int): String < (Env[UserRepo] & Sync & Abort[String]) = ???

  /** Ex 5: save the user, then log `"registered <name>"`. */
  def register(id: Int, name: String): Unit < (Env[UserRepo] & Env[Logger] & Sync) = ???

  /** Ex 6: like `register`, but first fail with `"too many users"` when the repository already holds
    * `maxUsers` users (or more). Nothing must be saved or logged in that case. Reuse `register`.
    */
  def registerChecked(id: Int, name: String): Unit < (Env[AppConfig] & Env[UserRepo] & Env[Logger] & Sync &
    Abort[String]) = ???

  /** Ex 7: run `registerChecked` with the given services. Only `Sync` and `Abort[String]` must remain. */
  def runRegisterChecked(config: AppConfig, repo: UserRepo, logger: Logger)(
      id: Int,
      name: String
  ): Unit < (Sync & Abort[String]) = ???

end Exercises
