package course.beginner.l07_data

import kyo.*

/** B7 solutions. */
object Exercises:

  def firstEven(xs: Seq[Int]): Maybe[Int] = Maybe.fromOption(xs.find(_ % 2 == 0))

  def nameLength(name: String): Int = Maybe(name).fold(0)(_.length)

  def parsePort(s: String): Result[String, Int] =
    Result
      .catching[NumberFormatException](s.toInt)
      .mapFailure(_ => s"not a number: $s")
      .flatMap: port =>
        if port >= 1 && port <= 65535 then Result.succeed(port)
        else Result.fail(s"out of range: $port")

  def firstValidPort(inputs: Seq[String]): Maybe[Int] =
    Maybe.fromOption(inputs.view.map(parsePort(_).toMaybe).find(_.isDefined)).flatten

  def rollingLog(log: Chunk[String], entry: String, max: Int): Chunk[String] =
    log.append(entry).takeRight(max)

  def parseTimeout(s: String): Duration = Duration.parse(s).getOrElse(30.seconds)

  def totalTime(parts: Seq[Duration]): Duration = parts.foldLeft(Duration.Zero)(_ + _)

  def isExpired(start: Instant, timeout: Duration, now: Instant): Boolean = now > start + timeout

  def requireEven(xs: Seq[Int]): Int < Abort[Absent] = Abort.get(firstEven(xs))

end Exercises
