package course.beginner.l07_data

import kyo.*

/** # B7 — The kyo-data toolbox
  *
  * Kyo ships its own small data types. They are used throughout its APIs and are designed to be cheap: most of
  * them are opaque types that avoid allocations.
  *
  *   - `Maybe[A]`: an optional value, `Present(a)` or `Absent`. Like `Option`, but allocation-free, and
  *     `Maybe(null)` is `Absent`.
  *   - `Result[E, A]`: `Success(a)`, `Failure(e)` or `Panic(throwable)`. It is what `Abort.run` returns.
  *   - `Chunk[A]`: an immutable `Seq` with cheap `append`, `take`, `drop` and slicing.
  *   - `Duration` and `Instant`: time spans and points in time, with readable syntax (`5.seconds`).
  *
  * All of them convert to and from the standard library types, and `Abort.get` lifts `Maybe` and `Result` into
  * the `Abort` effect.
  *
  * Pitfall: because `Maybe`, `Result` and `Duration` are opaque types, `toString` shows the raw underlying value
  * (`Present(2).toString` is `"2"`, a `Duration` prints as nanoseconds). Use `.show` to print them.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.beginner.l07_data.DataLesson"
  * }}}
  */
object DataLesson extends KyoApp:

  // ----- Maybe -----
  val some: Maybe[Int] = Present(1)
  val none: Maybe[Int] = Absent
  val fromNull: Maybe[String] = Maybe(System.getenv("SURELY_NOT_DEFINED_VARIABLE")) // Absent

  val maybeDemo: List[String] = List(
    some.map(_ + 1).show,                // Present(2)
    none.getOrElse(0).toString,          // 0
    some.fold(0)(_ * 10).toString,       // 10: fold(ifAbsent)(ifPresent)
    Maybe.fromOption(Some("x")).show,    // Present(x)
    some.toOption.toString,              // Some(1)
    Maybe.when(1 > 2)("never").show      // Absent
  )

  // ----- Result -----
  val ok: Result[String, Int]     = Result.succeed(1)
  val ko: Result[String, Int]     = Result.fail("boom")
  val caught: Result[NumberFormatException, Int] = Result.catching[NumberFormatException]("x".toInt)

  val resultDemo: List[String] = List(
    ok.map(_ + 1).show,                                   // Success(2)
    ko.getOrElse(0).toString,                             // 0
    caught.mapFailure(_.getMessage).show,                 // Failure(For input string: "x")
    ok.flatMap(n => if n > 0 then Result.succeed(n) else Result.fail("negative")).show,
    ko.fold(n => s"got $n", e => s"failed with $e", t => s"panicked with $t"),
    ok.toMaybe.show,                                      // Present(1)
    Result.fromEither(Left("left")).show,                 // Failure(left)
    Result(throw new Exception("oops")).toString          // Panic: `Result(...)` catches every exception
  )

  // ----- Chunk -----
  val chunk: Chunk[Int] = Chunk(1, 2, 3)

  val chunkDemo = List(
    chunk.append(4),                 // Chunk(1, 2, 3, 4)
    chunk.concat(Chunk(4, 5)),       // Chunk(1, 2, 3, 4, 5)
    chunk.takeRight(2),              // Chunk(2, 3)
    chunk.map(_ * 2),                // a Chunk is a Seq: the usual collection methods work
    chunk.headMaybe.show,            // Present(1)
    Chunk.from(List(7, 8))           // Chunk(7, 8)
  )

  // ----- Duration -----
  val timeout: Duration = 1.minute + 30.seconds

  val durationDemo: List[String] = List(
    timeout.show,                    // 90.seconds
    timeout.toMillis.toString,       // 90000
    (2.seconds * 3).show,            // 6.seconds
    (timeout > 1.minute).toString,   // true
    Duration.parse("250 millis").map(_.show).show, // Success(250.millis)
    Duration.parse("soon").isFailure.toString      // true
  )

  // ----- Instant -----
  val start: Instant = Instant.Epoch
  val later: Instant = start + 2.hours

  val instantDemo: List[String] = List(
    later.show,                      // 1970-01-01T02:00:00Z
    (later > start).toString,        // true
    later.minusOrZero(start).show,   // 2.hours
    start.minusOrZero(later).show    // 0: never negative
  )

  // ----- Bridging with Abort -----
  val fromMaybe: Int < Abort[Absent]  = Abort.get(some)
  val fromResult: Int < Abort[String] = Abort.get(ko)

  run:
    for
      _ <- Console.printLine(s"Maybe:    $maybeDemo")
      _ <- Console.printLine(s"Result:   $resultDemo")
      _ <- Console.printLine(s"Chunk:    $chunkDemo")
      _ <- Console.printLine(s"Duration: $durationDemo")
      _ <- Console.printLine(s"Instant:  $instantDemo")
      _ <- Console.printLine(s"Abort.get: ${Abort.run(fromMaybe).eval.show}, ${Abort.run(fromResult).eval.show}")
    yield ()

end DataLesson
