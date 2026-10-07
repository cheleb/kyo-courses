package course.beginner.l05_env

import course.KyoSuite
import kyo.*
import Exercises.*

class L05Spec extends KyoSuite:

  val config = AppConfig("kyo-courses", maxUsers = 2)

  def repoWith(users: (Int, String)*): UserRepo = runKyo(UserRepo.inMemory(users.toMap))

  test("Ex 1: appName reads the configuration"):
    assertEquals(Env.run(config)(appName).eval, "kyo-courses")

  test("Ex 2: banner uses both fields"):
    assertEquals(Env.run(config)(banner).eval, "Welcome to kyo-courses (max 2 users)")

  test("Ex 3: runBanner provides the configuration"):
    assertEquals(runBanner(AppConfig("demo", 5)), "Welcome to demo (max 5 users)")

  test("Ex 4: findUser finds existing users"):
    val repo = repoWith(1 -> "alice")
    assertEquals(runKyo(Env.run(repo)(findUser(1))), "alice")

  test("Ex 4: findUser fails for unknown users"):
    val repo = repoWith(1 -> "alice")
    assertEquals(runKyo(Abort.run[String](Env.run(repo)(findUser(2)))), Result.fail("user 2 not found"))

  test("Ex 5: register saves and logs"):
    val repo   = repoWith()
    val logger = RecordingLogger()
    runKyo(Env.run(repo)(Env.run(logger: Logger)(register(1, "alice"))))
    assertEquals(runKyo(repo.find(1)), Some("alice"))
    assertEquals(runKyo(logger.messages), List("registered alice"))

  test("Ex 6: registerChecked registers while there is room"):
    val repo   = repoWith(1 -> "alice")
    val logger = RecordingLogger()
    val result = Env.runAll(TypeMap(config, repo, logger: Logger))(registerChecked(2, "bob"))
    assertEquals(runKyo(Abort.run[String](result)), Result.succeed(()))
    assertEquals(runKyo(repo.count), 2)
    assertEquals(runKyo(logger.messages), List("registered bob"))

  test("Ex 6: registerChecked refuses when full, without side effects"):
    val repo   = repoWith(1 -> "alice", 2 -> "bob")
    val logger = RecordingLogger()
    val result = Env.runAll(TypeMap(config, repo, logger: Logger))(registerChecked(3, "carol"))
    assertEquals(runKyo(Abort.run[String](result)), Result.fail("too many users"))
    assertEquals(runKyo(repo.find(3)), None)
    assertEquals(runKyo(logger.messages), Nil)

  test("Ex 7: runRegisterChecked provides every service"):
    val repo   = repoWith()
    val logger = RecordingLogger()
    val run    = runRegisterChecked(config, repo, logger)
    runKyo(run(1, "alice").andThen(run(2, "bob")))
    assertEquals(runKyo(Abort.run[String](run(3, "carol"))), Result.fail("too many users"))
    assertEquals(runKyo(logger.messages), List("registered alice", "registered bob"))
