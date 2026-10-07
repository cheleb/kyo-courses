package course.intermediate.l03_stream

import kyo.*

/** I3 solutions. */
object Exercises:

  def fromList(xs: Seq[Int]): Stream[Int, Any] = Stream.init(xs)

  def evens(n: Int): Stream[Int, Any] = Stream.range(0, Int.MaxValue, step = 2).take(n)

  def fibonacci: Stream[Long, Any] =
    Stream.unfold((0L, 1L)) { case (a, b) => Maybe((a, (b, a + b))) }

  def wordCount(lines: Stream[String, Any]): Int < Any =
    lines.mapPure(_.split("\\s+").count(_.nonEmpty)).foldPure(0)(_ + _)

  private def parse(line: String): Int < Abort[String] =
    Abort.get(line.toIntOption.toRight(s"not a number: $line"))

  def parseAll(lines: Stream[String, Any]): Stream[Int, Abort[String]] = lines.map(parse)

  def parseFirst(n: Int)(lines: Seq[String]): Result[String, Seq[Int]] =
    Abort.run(Stream.init(lines).take(n).map(parse).run).eval

  def firstOver(threshold: Int)(s: Stream[Int, Any]): Maybe[Int] < Any = s.findPure(_ > threshold)

  def runningTotal(s: Stream[Int, Any]): Stream[Int, Any] =
    Stream:
      Var.run(0):
        s.foreach(n => Var.update[Int](_ + n).map(total => Emit.value(Chunk(total))))

end Exercises
