package course.beginner.l01_hello

import course.KyoSuite
import Exercises.*

class L01Spec extends KyoSuite:

  test("Ex 1: hello prints Hello, Kyo!"):
    assertEquals(stdOut(hello), "Hello, Kyo!\n")

  test("Ex 2: greet prints the name"):
    assertEquals(stdOut(greet("Alice")), "Hello, Alice!\n")

  test("Ex 3: twoLines prints two lines in order"):
    assertEquals(stdOut(twoLines), "Hello\nKyo\n")

  test("Ex 4: greetAll greets everyone in order"):
    assertEquals(stdOut(greetAll(Seq("Alice", "Bob"))), "Hello, Alice!\nHello, Bob!\n")

  test("Ex 4: greetAll prints nothing for no names"):
    assertEquals(stdOut(greetAll(Seq.empty)), "")
