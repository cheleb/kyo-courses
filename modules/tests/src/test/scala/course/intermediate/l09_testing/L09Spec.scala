package course.intermediate.l09_testing

import course.KyoSuite
import kyo.*
import Exercises.*

class L09Spec extends KyoSuite:

  test("Ex 1: runSync"):
    assertEquals(runSync(Sync.defer(21 * 2)), 42)
    intercept[IllegalStateException](runSync(Sync.defer(throw new IllegalStateException("boom"))))

  test("Ex 2: runResult"):
    assertEquals(runResult(App.sqrt(16)), Result.succeed(4.0))
    assertEquals(runResult(App.sqrt(-1)), Result.fail("negative: -1.0"))
    assertEquals(runResult(Async.delay(10.millis)(Abort.fail[String]("late"))), Result.fail("late"))

  test("Ex 3: output"):
    assertEquals(output(Console.printLine("a").andThen(Console.print("b"))), "a\nb")

  test("Ex 4: withInput"):
    assertEquals(withInput(Seq("x", "y"))(Console.readLine.map(a => Console.readLine.map(b => a + b))), "xy")

  test("Ex 4: test App.greet end to end"):
    // Runners (`withInput`) run immediately: wrap the program (`Console.withOut`) *inside* them.
    assertEquals(withInput(Seq("Ada"))(Console.withOut(App.greet))._1.stdOut, "name?\nhello Ada\n")

  test("Ex 5: seeded makes randomness reproducible"):
    assertEquals(seeded(7)(App.roll(10)), seeded(7)(App.roll(10)))
    assertNotEquals(seeded(7)(App.roll(10)), seeded(8)(App.roll(10)))

  test("Ex 6: afterVirtualTime runs a one-hour sleep instantly"):
    val start = System.nanoTime()
    assertEquals(runKyo(afterVirtualTime(1.hour)(App.remind(1.hour, "stand up"))), "reminder: stand up")
    assert((System.nanoTime() - start) / 1_000_000 < 3000, "used real time?")

  test("Ex 7: doneWithin"):
    assert(!runKyo(doneWithin(59.minutes)(App.remind(1.hour, "x"))))
    assert(runKyo(doneWithin(1.hour)(App.remind(1.hour, "x"))))
