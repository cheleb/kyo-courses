package course.intermediate.l07_layer

import course.KyoSuite
import kyo.*
import Exercises.*

class L07Spec extends KyoSuite:

  /** Builds the components of a layer whose dependencies have all been provided. */
  def build[A: Tag](layer: Layer[A, Sync]): A = runKyo(Memo.run(layer.run.map(_.get[A])))

  def provided[A: Tag](layer: Layer[A, Env[Config] & Sync]): A =
    build(configLayer("jdbc:test") to layer)

  test("Ex 1: configLayer"):
    assertEquals(build(configLayer("jdbc:x")), Config("jdbc:x", 4))

  test("Ex 2: databaseLayer uses the configuration"):
    assertEquals(provided(databaseLayer).url, "jdbc:test")

  test("Ex 3: repoLayer uses the database"):
    val repo = provided(databaseLayer to repoLayer)
    assertEquals(runKyo(repo.save("alice")).takeWhile(_ != '#'), "[jdbc:test")

  test("Ex 4: auditLayer uses the database"):
    val audit = provided(databaseLayer to auditLayer)
    assert(runKyo(audit.record("x")).endsWith("audit x"))

  test("Ex 5: serviceLayer combines repo and audit"):
    val service = provided(databaseLayer to (repoLayer and auditLayer) to serviceLayer)
    assertEquals(runKyo(service.register("bob")).size, 2)

  def checkApp(layer: Layer[UserService, Sync]): Unit =
    val service = build(layer)
    val lines   = runKyo(service.register("carol"))
    assert(lines.forall(_.startsWith("[jdbc:app#")), lines.toString)
    assert(service.repo.db eq service.audit.db, "repo and audit must share one database")

  test("Ex 6: appLayer wires everything, sharing the database"):
    checkApp(appLayer("jdbc:app"))

  test("Ex 7: autoLayer wires everything, sharing the database"):
    checkApp(autoLayer("jdbc:app"))

  test("Ex 8: runApp provides the service"):
    val program = Env.use[UserService](s => s.register("dave").map(_.last))
    assert(runKyo(runApp(appLayer("jdbc:run"))(program)).endsWith("audit registered dave"))
