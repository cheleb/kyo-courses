package course.intermediate.l01_stacking

import kyo.*

/** I1 solutions. */
object Exercises:

  def stateLostOnFailure[A](initial: Int)(v: A < (Var[Int] & Abort[String])): Result[String, (Int, A)] =
    Abort.run(Var.runTuple(initial)(v)).eval

  def stateKeptOnFailure[A](initial: Int)(v: A < (Var[Int] & Abort[String])): (Int, Result[String, A]) =
    Var.runTuple(initial)(Abort.run(v)).eval

  def step(n: Int): Unit < (Var[Int] & Abort[String]) =
    for
      _ <- Abort.when(n < 0)(s"negative: $n")
      _ <- Var.updateDiscard[Int](_ + n)
    yield ()

  def processAll(items: Seq[Int]): (Int, Seq[String]) =
    val errors = Kyo.foreach(items)(n => Abort.run(step(n)).map(_.failure))
    Var.runTuple(0)(errors).map((total, failures) => (total, failures.flatMap(_.toList))).eval

  def processUntilError(items: Seq[Int]): (Int, Maybe[String]) =
    Var.runTuple(0)(Abort.run(Kyo.foreachDiscard(items)(step))).map((total, r) => (total, r.failure)).eval

  def classify(v: Int < Abort[String | Int]): String =
    Abort.run[String](Abort.run[Int](v)).eval match
      case Result.Success(Result.Success(n)) => s"value $n"
      case Result.Success(Result.Failure(c)) => s"code $c"
      case Result.Failure(s)                 => s"message $s"
      case other                             => s"panic: $other"

  def transaction[A](v: A < (Var[Int] & Abort[String])): A < (Var[Int] & Abort[String]) =
    Var.get[Int].map: before =>
      Abort.fold[String](
        (a: A) => a,
        error => Var.setDiscard(before).andThen(Abort.fail(error))
      )(v)

end Exercises
