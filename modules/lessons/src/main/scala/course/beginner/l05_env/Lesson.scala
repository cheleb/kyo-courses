package course.beginner.l05_env

import kyo.*

/** # B5 — `Env`: dependency injection
  *
  * `Env[R]` is the effect of a computation that *needs* a value of type `R`: a configuration, a database client, a
  * logger... The dependency shows up in the type, and the compiler refuses to run the program until it is
  * provided.
  *
  * Key ideas of this lesson:
  *
  *   1. Reading: `Env.get[R]` returns the value; `Env.use[R](f)` applies a function to it.
  *   2. Several dependencies accumulate in the effect row: `Env[UserRepo] & Env[Logger]`
  *      (equivalently `Env[UserRepo & Logger]`).
  *   3. Providing: `Env.run(value)(program)` removes `Env[R]` from the row. Each `Env.run` removes one dependency;
  *      `Env.runAll(TypeMap(a, b))` provides several at once.
  *   4. The *same* program runs with production or test implementations: only the `Env.run` call changes.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.beginner.l05_env.EnvLesson"
  * }}}
  */
object EnvLesson extends KyoApp:

  // 1. Reading a dependency.
  val maxUsers: Int < Env[AppConfig] =
    Env.get[AppConfig].map(_.maxUsers)

  // `Env.use` is the same thing in one step.
  val appName: String < Env[AppConfig] =
    Env.use[AppConfig](_.appName)

  // 2. A program that needs two services. Kyo infers the row; here it is written out for clarity.
  def welcome(id: Int): Unit < (Env[UserRepo] & Env[Logger] & Sync) =
    for
      repo   <- Env.get[UserRepo]
      logger <- Env.get[Logger]
      user   <- repo.find(id)
      _      <- logger.log(s"welcome ${user.getOrElse("stranger")}")
    yield ()

  // 3. Providing dependencies, one at a time...
  val config = AppConfig("kyo-courses", maxUsers = 10)

  val name: String = Env.run(config)(appName).eval

  def oneByOne(repo: UserRepo): Unit < Sync =
    Env.run(repo):
      Env.run(Logger.console):
        welcome(1)

  // ... or all together.
  def allAtOnce(repo: UserRepo): Unit < Sync =
    Env.runAll(TypeMap(repo, Logger.console))(welcome(2))

  // 4. The same program with a test logger: nothing is printed, the messages are kept instead.
  def withRecording(repo: UserRepo): List[String] < Sync =
    val recorder = RecordingLogger()
    Env.run(repo)(Env.run(recorder: Logger)(welcome(1))).andThen(recorder.messages)

  run:
    for
      repo     <- UserRepo.inMemory(Map(1 -> "alice"))
      _        <- Console.printLine(s"app name: $name, max users: ${Env.run(config)(maxUsers).eval}")
      _        <- oneByOne(repo)
      _        <- allAtOnce(repo)
      recorded <- withRecording(repo)
      _        <- Console.printLine(s"recorded by the test logger: $recorded")
    yield ()

end EnvLesson
