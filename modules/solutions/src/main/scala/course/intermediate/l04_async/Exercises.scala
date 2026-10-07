package course.intermediate.l04_async

import kyo.*

/** I4 solutions. */
object Exercises:

  def slowDouble(n: Int): Int < Async = Async.sleep(50.millis).andThen(n * 2)

  def doubleAll(xs: Seq[Int]): Seq[Int] < Async = Async.foreach(xs)(slowDouble)

  def both[A, B](a: A < Async, b: B < Async): (A, B) < Async = Async.zip(a, b)

  def fastest(mirrors: Seq[(String, Duration)]): String < Async =
    Async.race(mirrors.map((name, latency) => Async.sleep(latency).andThen(name)))

  def firstSuccess(attempts: Seq[String < (Async & Abort[String])]): String < (Async & Abort[String]) =
    Async.race(attempts)

  def withTimeout[A](d: Duration)(v: A < Async): Maybe[A] < Async =
    Abort.run[Timeout](Async.timeout(d)(v)).map(_.toMaybe)

  def tickFor(d: Duration, count: AtomicInt): Unit < (Async & Scope) =
    def tick: Unit < Async = count.incrementAndGet.andThen(Async.sleep(10.millis)).andThen(tick)
    for
      fiber <- Fiber.init(tick)
      _     <- Async.sleep(d)
      _     <- fiber.interrupt
    yield ()
  end tickFor

end Exercises
