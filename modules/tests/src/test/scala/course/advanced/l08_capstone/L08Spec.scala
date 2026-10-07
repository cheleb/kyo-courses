package course.advanced.l08_capstone

import course.KyoSuite
import kyo.*
import Exercises.*

class L08Spec extends KyoSuite:

  def program: (Maybe[String], Boolean, Boolean) < KV =
    for
      _       <- put("a", "1")
      a       <- get("a")
      deleted <- delete("a")
      again   <- delete("a")
    yield (a, deleted, again)

  test("Ex 1-3, 5: operations against the in-memory store"):
    assertEquals(runInMemory(Map.empty)(program).eval, (Map.empty[String, String], (Maybe("1"), true, false)))

  test("Ex 5: runInMemory starts from the initial contents"):
    assertEquals(runInMemory(Map("x" -> "7"))(get("x")).eval, (Map("x" -> "7"), Maybe("7")))
    assertEquals(runInMemory(Map.empty)(get("missing")).eval._2, Maybe.empty[String])

  test("Ex 4: increment"):
    val three = increment("n").andThen(increment("n")).andThen(increment("n"))
    assertEquals(runInMemory(Map.empty)(three).eval, (Map("n" -> "3"), 3))
    assertEquals(runInMemory(Map("n" -> "41"))(increment("n")).eval, (Map("n" -> "42"), 42))

  test("Ex 6: runReadOnly reads"):
    assertEquals(Abort.run(runReadOnly(Map("k" -> "v"))(get("k"))).eval, Result.succeed(Maybe("v")))

  test("Ex 6: runReadOnly refuses writes and stops the program"):
    assertEquals(Abort.run(runReadOnly(Map.empty)(program)).eval, Result.fail("read-only: put a"))
    assertEquals(Abort.run(runReadOnly(Map("a" -> "1"))(delete("a"))).eval, Result.fail("read-only: delete a"))

  test("Ex 7: runLogged logs and forwards"):
    val (log, (store, result)) = Emit.run[String](runInMemory(Map.empty)(runLogged(program))).eval
    assertEquals(log.toList, List("put a=1", "get a", "delete a", "delete a"))
    assertEquals(store, Map.empty[String, String])
    assertEquals(result, (Maybe("1"), true, false))

  test("Ex 7: runLogged forwards to a read-only store too"):
    val (log, result) = Emit.run[String](Abort.run(runReadOnly(Map("k" -> "v"))(runLogged(get("k"))))).eval
    assertEquals(log.toList, List("get k"))
    assertEquals(result, Result.succeed(Maybe("v")))

  test("Ex 8: runCounted counts and forwards"):
    val (store, (count, result)) = runInMemory(Map.empty)(runCounted(program)).eval
    assertEquals(count, 4)
    assertEquals(result, (Maybe("1"), true, false))
    assertEquals(store, Map.empty[String, String])

  test("Ex 7 + 8: decorators stack"):
    val (log, (_, (count, _))) = Emit.run[String](runInMemory(Map.empty)(runLogged(runCounted(increment("x"))))).eval
    assertEquals(count, 2)
    assertEquals(log.toList, List("get x", "put x=1"))

  test("Ex 9: visitAll"):
    val (store, counts) = runInMemory(Map("visits:ada" -> "10"))(visitAll(Seq("ada", "bob", "ada"))).eval
    assertEquals(counts, Map("ada" -> 12, "bob" -> 1))
    assertEquals(store, Map("visits:ada" -> "12", "visits:bob" -> "1"))
