package course.advanced.l06_loop

import kyo.*
import kyo.kernel.*

/** A6 solutions. */
object Exercises:

  def sumOfSquares(n: Int): Long < Any =
    Loop(1, 0L)((i, acc) => if i > n then Loop.done(acc) else Loop.continue(i + 1, acc + i.toLong * i))

  def fib(n: Int): BigInt < Any =
    Loop(0, BigInt(0), BigInt(1))((i, a, b) => if i == n then Loop.done(a) else Loop.continue(i + 1, b, a + b))

  def collatzSteps(n: Long): Int < Any =
    Loop.indexed(n)((steps, current) =>
      if current == 1 then Loop.done(steps)
      else Loop.continue(if current % 2 == 0 then current / 2 else 3 * current + 1)
    )

  def drain: Unit < (Var[Int] & Emit[Int]) =
    Loop.whileTrue(Var.use[Int](_ > 0)):
      Var.use[Int](n => Emit.value(n).andThen(Var.setDiscard(n - 1)))

  def firstAbove(threshold: Int)(xs: Seq[Int < Sync]): Maybe[Int] < Sync =
    Kyo.findFirst(xs)(v => v.map(n => Maybe.when(n > threshold)(n)))

  def pipeline: Arrow[String, Int, Abort[String]] =
    val parse = Arrow[String](s => Abort.get(s.trim.toIntOption.toRight(s"not a number: ${s.trim}")))
    val even  = Arrow[Int](n => if n % 2 == 0 then n else Abort.fail(s"odd: $n"))
    parse.chain(even).chain(Arrow[Int](_ / 2))

  def factorial: Arrow[Int, BigInt, Any] =
    Arrow.recursive[Int, BigInt, Any]((self, n) => if n <= 1 then BigInt(1) else self(n - 1).map(_ * n))

end Exercises
