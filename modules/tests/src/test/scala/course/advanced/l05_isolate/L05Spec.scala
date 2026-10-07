package course.advanced.l05_isolate

import course.KyoSuite
import kyo.*
import Exercises.*

class L05Spec extends KyoSuite:

  /** Adds `n` to the state after a delay: higher `n` finish first. */
  def add(n: Int): Unit < (Var[Int] & Async) =
    Async.sleep((50 - n * 10).millis).andThen(Var.updateDiscard[Int](_ + n))

  def say(n: Int): Unit < (Emit[String] & Async) =
    Async.sleep((50 - n * 10).millis).andThen(Emit.value(s"task $n"))

  def timed[A](v: => A < (Async & Scope & Abort[Any])): (A, Long) =
    val start  = System.nanoTime()
    val result = runKyo(v)
    (result, (System.nanoTime() - start) / 1_000_000)

  test("Ex 1: lastWins keeps the last task's state"):
    assertEquals(runKyo(Var.runTuple(100)(lastWins((1 to 3).map(add))))._1, 103)

  test("Ex 1: lastWins runs concurrently"):
    val (_, millis) = timed(Var.run(0)(lastWins(Seq.fill(5)(Async.sleep(100.millis).andThen(Var.setDiscard(1))))))
    assert(millis < 400, s"took $millis ms")

  test("Ex 2: sandboxed discards changes and returns results"):
    val tasks = (1 to 3).map(n => add(n).andThen(Var.get[Int]))
    val (state, results) = runKyo(Var.runTuple(100)(sandboxed(tasks)))
    assertEquals(state, 100)
    assertEquals(results.toList, List(101, 102, 103))

  test("Ex 3: sumAll keeps every addition"):
    assertEquals(runKyo(Var.runTuple(100)(sumAll((1 to 3).map(add))))._1, 106)
    assertEquals(runKyo(Var.runTuple(0)(sumAll((1 to 10).map(n => Var.updateDiscard[Int](_ + n)))))._1, 55)

  test("Ex 4: gatherMessages keeps every message in task order"):
    assertEquals(runKyo(gatherMessages((1 to 3).map(say))).toList, List("task 1", "task 2", "task 3"))

  test("Ex 5: both"):
    val tasks = (1 to 3).map(n => add(n).andThen(say(n)))
    val (state, messages) = runKyo(both(100)(tasks))
    assertEquals(state, 103)
    assertEquals(messages.toList, List("task 1", "task 2", "task 3"))

  test("Ex 6: inFiber brings the fiber's state back"):
    val v = Var.update[Int](_ * 2).map(n => s"doubled to $n")
    assertEquals(runKyo(Var.runTuple(21)(inFiber(v))), (42, "doubled to 42"))
