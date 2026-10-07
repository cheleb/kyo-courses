package course.beginner.l06_var

import kyo.*

/** B6 solutions. */
object Exercises:

  def next: Int < Var[Int] = Var.update[Int](_ + 1)

  def swap(value: Int): Int < Var[Int] = Var.set(value)

  def labels(names: Seq[String]): Seq[String] < Var[Int] =
    Kyo.foreach(names)(name => next.map(n => s"$n. $name"))

  def runLabels(names: Seq[String]): Seq[String] = Var.run(0)(labels(names)).eval

  def finalCount(start: Int, n: Int): Int =
    Var.runTuple(start)(Kyo.fill(n)(next)).eval._1

  def exec(instr: Instr): Unit < (Var[List[Int]] & Abort[String]) =
    def binary(op: (Int, Int) => Int): Unit < (Var[List[Int]] & Abort[String]) =
      Var.get[List[Int]].map:
        case a :: b :: rest => Var.setDiscard(op(b, a) :: rest)
        case _              => Abort.fail("stack underflow")

    instr match
      case Instr.Push(value) => Var.updateDiscard[List[Int]](value :: _)
      case Instr.Add         => binary(_ + _)
      case Instr.Mul         => binary(_ * _)
  end exec

  def evaluate(program: Seq[Instr]): Result[String, Int] =
    val run =
      for
        _     <- Kyo.foreachDiscard(program)(exec)
        stack <- Var.get[List[Int]]
        top   <- Abort.get(stack.headOption.toRight("empty stack"))
      yield top
    Abort.run(Var.run(List.empty[Int])(run)).eval
  end evaluate

end Exercises
