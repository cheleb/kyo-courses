package course

import kyo.*

/** Base class for every course spec: runs Kyo programs synchronously so munit can check their results. */
abstract class KyoSuite extends munit.FunSuite:

  /** Runs a program with any of the common effects and returns its value, rethrowing failures. */
  def runKyo[A](v: => A < (Async & Scope & Abort[Any]))(using Frame): A =
    import AllowUnsafe.embrace.danger
    KyoApp.Unsafe.runAndBlock(5.seconds)(v).getOrThrow

  /** Runs a program and returns what it printed on the standard output. */
  def stdOut(v: => Any < (Async & Scope & Abort[Any]))(using Frame): String =
    runKyo(Console.withOut(v))._1.stdOut
