package course.advanced.l05_isolate

import kyo.*

/** A5 solutions. */
object Exercises:

  def lastWins(tasks: Seq[Unit < (Var[Int] & Async)]): Unit < (Var[Int] & Async) =
    Var.isolate.update[Int].use(Async.foreachDiscard(tasks)(identity))

  def sandboxed[A](tasks: Seq[A < (Var[Int] & Async)]): Seq[A] < (Var[Int] & Async) =
    Var.isolate.discard[Int].use(Async.foreach(tasks)(identity))

  def sumAll(tasks: Seq[Unit < (Var[Int] & Async)]): Unit < (Var[Int] & Async) =
    Var.get[Int].map: start =>
      Var.isolate.merge[Int]((current, fork) => current + (fork - start)).use(Async.foreachDiscard(tasks)(identity))

  def gatherMessages(tasks: Seq[Unit < (Emit[String] & Async)]): Chunk[String] < Async =
    Emit.run[String](Emit.isolate.merge[String].use(Async.foreachDiscard(tasks)(identity))).map(_._1)

  def both(start: Int)(tasks: Seq[Unit < (Var[Int] & Emit[String] & Async)]): (Int, Chunk[String]) < Async =
    val isolate = Var.isolate.update[Int].andThen(Emit.isolate.merge[String])
    val program = isolate.use(Async.foreachDiscard(tasks)(identity))
    Emit.run[String](Var.runTuple(start)(program)).map((messages, result) => (result._1, messages))

  def inFiber[A](v: A < (Var[Int] & Async)): A < (Var[Int] & Async & Scope) =
    Var.isolate.update[Int].use(Fiber.init(v).map(_.get))

end Exercises
