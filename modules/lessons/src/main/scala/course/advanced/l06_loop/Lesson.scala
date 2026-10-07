package course.advanced.l06_loop

import kyo.*
import kyo.kernel.*

/** # A6 — Stack safety and performance: `Loop`, `Kyo` operations, `Arrow`
  *
  * A1 showed that recursion through `map` is stack-safe: every step is a node the evaluator unfolds. Safe is not the
  * same as cheap, though: each step allocates nodes. The kernel offers cheaper building blocks.
  *
  * ## `Loop`
  *
  * `Loop(state)(body)` runs `body` round after round. The body answers `Loop.continue(nextState)` or
  * `Loop.done(result)`, and may perform effects between rounds. Up to four state values can be carried
  * (`Loop(a, b)((a, b) => ...)`) without allocating a tuple per round. Variants: `Loop.indexed` (a round counter),
  * `Loop.repeat(n)(body)`, `Loop.whileTrue(cond)(body)`, `Loop.forever(body)`, `Loop.foreach(body)`.
  *
  * ## `Kyo.*` sequential operations
  *
  * `Kyo.foreach`, `foldLeft`, `collect`, `filter`, `findFirst`, `takeWhile`, `partition`, `scanLeft`, `fill`... are the
  * effectful versions of the collection methods, run sequentially and implemented with `Loop`. Those that can stop
  * early (`findFirst`, `takeWhile`) do *not* run the effects of the remaining elements.
  *
  * ## `Arrow`
  *
  * An `Arrow[A, B, S]` is a reified function `A => B < S` (A2 met it as the continuation of a handler). Build one with
  * `Arrow[A](f)`, compose with `chain`, apply with `arrow(a)`. `Arrow.recursive((self, a) => ...)` defines a recursive
  * arrow allocated *once*, instead of re-creating closures at every level. Composed arrows are fused by the JIT into
  * straight-line code, which makes them the cheapest way to express a reusable effectful pipeline.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.advanced.l06_loop.LoopLesson"
  * }}}
  */
object LoopLesson extends KyoApp:

  // The same sum, three ways.
  def recursiveSum(n: Int): Long < Sync =
    if n == 0 then 0L else Sync.defer(recursiveSum(n - 1)).map(_ + n)

  def loopSum(n: Int): Long < Any =
    Loop(1, 0L)((i, acc) => if i > n then Loop.done(acc) else Loop.continue(i + 1, acc + i))

  def foldSum(n: Int): Long < Any = Kyo.foldLeft(1 to n)(0L)((acc, i) => acc + i)

  // A loop performing effects between rounds: guess a random number.
  def guess(secret: Int): Int < Sync =
    Loop.indexed: attempt =>
      Random.nextInt(10).map(n => if n == secret then Loop.done(attempt + 1) else Loop.continue)

  // Early exit in Kyo operations: the effects of the remaining elements never run.
  def firstOver(threshold: Int)(xs: Seq[Int]): Maybe[Int] < Sync =
    Kyo.findFirst(xs)(n => Console.printLine(s"    checking $n").andThen(Maybe.when(n > threshold)(n)))

  // Arrows: a reusable, composable pipeline.
  val parse: Arrow[String, Int, Abort[String]] =
    Arrow[String](s => Abort.get(s.toIntOption.toRight(s"not a number: $s")))
  val positive: Arrow[Int, Int, Abort[String]] =
    Arrow[Int](n => if n > 0 then n else Abort.fail(s"not positive: $n"))
  val pipeline: Arrow[String, Int, Abort[String]] = parse.chain(positive).chain(Arrow[Int](_ * 10))

  // A recursive arrow, allocated once.
  val countDigits: Arrow[Long, Int, Any] =
    Arrow.recursive[Long, Int, Any]((self, n) => if n < 10 then 1 else self(n / 10).map(_ + 1))

  def millis[A](label: String)(v: => A < Sync): Unit < Sync =
    for
      start <- Clock.nowMonotonic
      a     <- v
      end   <- Clock.nowMonotonic
      _     <- Console.printLine(s"  $label = $a in ${end.minusOrZero(start).toMillis} ms")
    yield ()

  run:
    for
      _ <- Console.printLine("sum 1..5_000_000:")
      _ <- millis("recursion")(recursiveSum(5_000_000))
      _ <- millis("Loop     ")(loopSum(5_000_000))
      _ <- millis("foldLeft ")(foldSum(5_000_000))
      g <- Random.withSeed(1)(guess(7))
      _ <- Console.printLine(s"guessed 7 after $g attempts")
      _ <- Console.printLine("findFirst stops at the first match:")
      f <- firstOver(2)(Seq(1, 2, 3, 4, 5))
      _ <- Console.printLine(s"  found ${f.show}")
      _ <- Console.printLine(s"pipeline: ${List("12", "x", "-1").map(s => Abort.run(pipeline(s)).eval.show)}")
      _ <- Console.printLine(s"digits of 9_876_543_210: ${countDigits(9_876_543_210L).eval}")
    yield ()

end LoopLesson
