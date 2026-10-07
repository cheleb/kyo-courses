package course.advanced.l01_pending_internals

import kyo.*

/** A1 solutions. */
object Exercises:

  def sameType[A: Tag, B: Tag]: Boolean = Tag[A] =:= Tag[B]

  def isSubtype[A: Tag, B: Tag]: Boolean = Tag[A] <:< Tag[B]

  def callSite(using frame: Frame): String =
    s"${frame.position.fileName}:${frame.position.lineNumber}"

  def check(condition: Boolean, message: String)(using Frame): Unit < Abort[String] =
    Abort.when(!condition)(s"$message (at $callSite)")

  def sumTo(n: Long): Long < Sync =
    if n == 0 then 0L else Sync.defer(sumTo(n - 1)).map(_ + n)

  def flattenAll(v: Int < Sync < Sync < Sync): Int < Sync = v.flatten.flatten

  def runAll(env: Int)(v: Int < (Env[Int] & Abort[String] & Var[Int])): (Int, Result[String, Int]) =
    v.handle(Env.run(env), Abort.run, Var.runTuple(0), _.eval)

end Exercises
