package repro

import kyo.*

/** Repro 1: a two-dependency `Layer.from` whose type is ascribed compiles, then fails at runtime.
  *
  * Run: sbt "repro/runMain repro.Repro1LayerFromAscription"
  */
object Repro1LayerFromAscription extends KyoApp:

  final class A
  final class B
  final class C(val a: A, val b: B)

  val deps: Layer[A & B, Any] = Layer(A()) and Layer(B())

  // Type parameters inferred from the lambda: works.
  val inferred = Layer.from((a: A, b: B) => C(a, b))

  // Same expression, with the expected type written out: compiles, fails at runtime.
  val ascribed: Layer[C, Env[A & B]] = Layer.from((a: A, b: B) => C(a, b))

  // Same with the expected type coming from a method's return type.
  def returned: Layer[C, Env[A & B]] = Layer.from((a: A, b: B) => C(a, b))

  // Workaround: explicit type arguments.
  val explicit: Layer[C, Env[A & B]] = Layer.from[A, B, C, Any]((a, b) => C(a, b))

  def attempt(label: String, layer: Layer[C, Env[A & B]]): Unit < Sync =
    Abort.run[Any](Memo.run((deps to layer).run)).map: result =>
      Console.printLine(s"$label: ${if result.isSuccess then "ok" else result.toString}")

  run:
    for
      _ <- attempt("inferred", inferred)
      _ <- attempt("ascribed", ascribed)
      _ <- attempt("returned", returned)
      _ <- attempt("explicit", explicit)
    yield ()

end Repro1LayerFromAscription
