package course.beginner.l02_pending

import course.KyoSuite
import java.util.concurrent.atomic.AtomicInteger
import kyo.*
import Exercises.*

class L02Spec extends KyoSuite:

  /** A computation that counts how many times it has been run. */
  def counting(counter: AtomicInteger): Int < Sync = Sync.defer(counter.incrementAndGet())

  test("Ex 1: answer is 42"):
    assertEquals(answer.eval, 42)

  test("Ex 2: double doubles"):
    assertEquals(double(21).eval, 42)

  test("Ex 3: sum adds both results"):
    assertEquals(runKyo(sum(Sync.defer(1), Sync.defer(2))), 3)

  test("Ex 3: sum runs a before b"):
    val counter = new AtomicInteger
    assertEquals(runKyo(sum(counting(counter).map(_ * 10), counting(counter))), 12)

  test("Ex 4: evaluate extracts the value"):
    assertEquals(evaluate((20: Int < Any).map(_ + 22)), 42)

  test("Ex 5: widen keeps the value"):
    assertEquals(runKyo(widen(Sync.defer(7))), 7)

  test("Ex 6: twice runs the computation twice, in order"):
    val counter = new AtomicInteger
    assertEquals(runKyo(twice(counting(counter))), (1, 2))
    assertEquals(counter.get, 2)

  test("Ex 7: sumAll adds everything"):
    assertEquals(runKyo(sumAll(Seq(Sync.defer(1), Sync.defer(2), Sync.defer(3)))), 6)

  test("Ex 7: sumAll runs each computation exactly once"):
    val counter = new AtomicInteger
    assertEquals(runKyo(sumAll(Seq.fill(3)(counting(counter)))), 1 + 2 + 3)
    assertEquals(counter.get, 3)

  test("Ex 7: sumAll of nothing is 0"):
    assertEquals(runKyo(sumAll(Seq.empty)), 0)
