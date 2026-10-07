package course.intermediate.l03_stream

import kyo.*

/** # I3 — `Stream`
  *
  * A `Stream[V, S]` is a *description* of a sequence of values of type `V`, produced with the effects `S`. Like any
  * Kyo computation it is lazy and can be run many times; it can be infinite; and every effect it uses shows up in
  * `S`.
  *
  * Under the hood a stream is just a computation that *emits chunks*: `Unit < (Emit[Chunk[V]] & S)` (see I2).
  * Values travel in `Chunk`s for performance, which is mostly invisible but explains why some operators come in a
  * `...Chunk` variant.
  *
  * Key ideas of this lesson:
  *
  *   1. Building: `Stream.init(seq)`, `Stream.range`, `Stream.unfold`, or `Stream(emitting computation)`.
  *   2. Transforming: `map`, `filter`, `take`, `drop`, `takeWhile`, `flatMap`, `concat`, `changes`, `tap`...
  *      Operators taking a function accept an *effectful* function: `map(n => Sync.defer(...))` adds `Sync` to `S`.
  *      The `...Pure` variants (`mapPure`, `filterPure`) take plain functions.
  *   3. Consuming: `run` (collect into a `Chunk`), `fold`, `foreach`, `find`, `discard`.
  *   4. Laziness: `take` / `find` stop the stream early, so infinite streams are fine.
  *   5. Pitfall: laziness works *per chunk*. An effectful `map` processes a whole chunk before `take` sees it, so
  *      `stream.map(f).take(2)` may run `f` on more than 2 elements (and fail on one you did not ask for).
  *      Use small chunks (`chunkSize = 1`, `rechunk(1)`) or put `take` before the effectful step.
  *   6. Generic code over `Stream[V, S]` needs a `Tag[V]` (streams emit `Chunk[V]` through a tagged effect):
  *      write `def f[V: Tag](s: Stream[V, Any])`.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.intermediate.l03_stream.StreamLesson"
  * }}}
  */
object StreamLesson extends KyoApp:

  // 1. Building streams.
  val fromSeq: Stream[Int, Any]   = Stream.init(Seq(1, 2, 3))
  val numbers: Stream[Int, Any]   = Stream.range(0, 10)                    // 0 until 10
  val naturals: Stream[Int, Any]  = Stream.range(0, Int.MaxValue)          // as good as infinite
  val powersOf2: Stream[Int, Any] =                                       // infinite: never returns Absent
    Stream.unfold(1)(n => Maybe((n, n * 2)))
  val fromEmit: Stream[String, Any] =                                     // a stream *is* an emitting computation
    Stream(Emit.value(Chunk("a", "b")).andThen(Emit.value(Chunk("c"))))

  // 2. Transforming. Nothing runs yet: these are still descriptions.
  val evenSquares: Stream[Int, Any] = numbers.filterPure(_ % 2 == 0).mapPure(n => n * n)

  // An effectful transformation: the effect appears in the stream type.
  val logged: Stream[Int, Sync] = fromSeq.map(n => Console.printLine(s"  processing $n").andThen(n * 10))

  // A step that may fail.
  def parse(s: String): Int < Abort[String] = Abort.get(s.toIntOption.toRight(s"not a number: $s"))

  val parsed: Stream[Int, Abort[String]] = Stream.init(Seq("1", "2", "x", "4")).map(parse)

  // 5. The same stream with one value per chunk: `take(2)` stops before `parse("x")` runs.
  val parsedOneByOne: Stream[Int, Abort[String]] = Stream.init(Seq("1", "2", "x", "4"), chunkSize = 1).map(parse)

  // flatMap: each value becomes a stream of its own.
  val repeated: Stream[Int, Any] = fromSeq.flatMap(n => Stream.init(Seq.fill(n)(n)))

  run:
    for
      _ <- Console.printLine(s"fromSeq: ${fromSeq.run.eval}")
      _ <- Console.printLine(s"fromEmit: ${fromEmit.run.eval}")
      _ <- Console.printLine(s"even squares: ${evenSquares.run.eval}")
      // 4. Laziness: only the first 5 values are ever produced.
      _ <- Console.printLine(s"powers of 2: ${powersOf2.take(5).run.eval}")
      _ <- Console.printLine(s"first square over 1000: ${naturals.mapPure(n => n * n).findPure(_ > 1000).eval.show}")
      // 3. Consuming in different ways.
      _ <- Console.printLine(s"sum 0..9 = ${numbers.foldPure(0)(_ + _).eval}")
      _ <- Console.printLine("effectful map:")
      r <- logged.run
      _ <- Console.printLine(s"  result: $r")
      _ <- Console.printLine(s"parsed: ${Abort.run(parsed.run).eval.show}")
      _ <- Console.printLine(s"parsed.take(2): ${Abort.run(parsed.take(2).run).eval.show}  <- the whole chunk was parsed")
      _ <- Console.printLine(s"parsedOneByOne.take(2): ${Abort.run(parsedOneByOne.take(2).run).eval.map(_.mkString("[", ", ", "]")).show}")
      _ <- Console.printLine(s"repeated: ${repeated.run.eval}")
      _ <- Console.printLine(s"changes: ${Stream.init(Seq(1, 1, 2, 2, 2, 1)).changes.run.eval}")
      _ <- Console.printLine(s"concat: ${fromSeq.concat(Stream.init(Seq(4, 5))).run.eval}")
    yield ()

end StreamLesson
