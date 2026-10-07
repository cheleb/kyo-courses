package course.beginner.l03_sync

import course.KyoSuite
import java.util.concurrent.atomic.AtomicInteger
import kyo.*
import Exercises.*

class L03Spec extends KyoSuite:

  test("Ex 1: increment does nothing until the program runs"):
    val counter = new AtomicInteger
    val program = increment(counter)
    assertEquals(counter.get, 0)
    assertEquals(runKyo(program), 1)

  test("Ex 1: increment runs again every time it is sequenced"):
    val counter = new AtomicInteger
    val program = increment(counter)
    assertEquals(runKyo(program.flatMap(_ => program)), 2)
    assertEquals(counter.get, 2)

  test("Ex 2: rollDie stays between 1 and 6 and hits every face"):
    val rolls = runKyo(Kyo.fill(600)(rollDie)).toSet
    assertEquals(rolls, (1 to 6).toSet)

  test("Ex 2: rollDie is reproducible with a seed"):
    val rolls = Random.withSeed(7)(Kyo.fill(20)(rollDie)).map(_.toList)
    assertEquals(runKyo(rolls), runKyo(rolls))

  test("Ex 3: pickWinner picks one of the names"):
    val names   = Seq("Alice", "Bob", "Carol")
    val winners = runKyo(Kyo.fill(100)(pickWinner(names))).toSet
    assertEquals(winners, names.toSet)

  test("Ex 4: timestamped uses the clock"):
    val program = Clock.withTimeControl: control =>
      for
        first  <- timestamped("hello")
        _      <- control.advance(90.seconds)
        second <- timestamped("bye")
      yield (first, second)
    assertEquals(runKyo(program), ("[1970-01-01T00:00:00Z] hello", "[1970-01-01T00:01:30Z] bye"))

  test("Ex 5: timed returns the result and the elapsed time"):
    val program = Clock.withTimeControl: control =>
      timed(control.advance(3.seconds).andThen("done"))
    val (result, elapsed) = runKyo(program)
    assertEquals(result, "done")
    assertEquals(elapsed.toMillis, 3000L)

  test("Ex 5: timed of an instant computation is zero"):
    val program = Clock.withTimeControl(_ => timed(42))
    val (result, elapsed) = runKyo(program)
    assertEquals(result, 42)
    assertEquals(elapsed.toMillis, 0L)
