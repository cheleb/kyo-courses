package course.intermediate.l02_choice_emit

import kyo.*

/** I2 solutions. */
object Exercises:

  def coin: String < Choice = Choice.eval("heads", "tails")

  def pairs(n: Int): (Int, Int) < Choice =
    for
      a <- Choice.evalSeq(1 to n)
      b <- Choice.evalSeq(a + 1 to n)
    yield (a, b)

  def pythagorean(n: Int): (Int, Int, Int) < Choice =
    for
      a <- Choice.evalSeq(1 to n)
      b <- Choice.evalSeq(a to n)
      c <- Choice.evalSeq(b to n)
      _ <- Choice.dropIf(a * a + b * b != c * c)
    yield (a, b, c)

  def permutations(xs: List[Int]): List[Int] < Choice =
    if xs.isEmpty then Nil
    else
      for
        head <- Choice.evalSeq(xs)
        tail <- permutations(xs.filter(_ != head))
      yield head :: tail

  def queens(n: Int): List[Int] < Choice =
    def safe(placed: List[Int], col: Int): Boolean =
      val row = placed.length
      placed.zipWithIndex.forall((c, r) => c != col && (row - r) != (col - c).abs)

    def place(placed: List[Int]): List[Int] < Choice =
      if placed.length == n then placed
      else
        for
          col  <- Choice.evalSeq(0 until n)
          _    <- Choice.dropIf(!safe(placed, col))
          done <- place(placed :+ col)
        yield done

    place(Nil)
  end queens

  def countdown(n: Int): Unit < Emit[Int] =
    Kyo.foreachDiscard(n to 1 by -1)(Emit.value(_))

  def collatz(n: Int): Int < Emit[Int] =
    Emit.value(n).andThen:
      if n == 1 then 0
      else collatz(if n % 2 == 0 then n / 2 else 3 * n + 1).map(_ + 1)

  def sumEmitted(v: Unit < Emit[Int]): Int =
    Emit.runFold(0)((acc: Int, n: Int) => acc + n)(v).eval._1

  def filterEmitted(threshold: Int)(v: Unit < Emit[Int]): Unit < Emit[Int] =
    Emit.runForeach(v)(n => Emit.valueWhen(n > threshold)(n))

end Exercises
