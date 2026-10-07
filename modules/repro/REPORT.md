# Kyo RC7: draft upstream reports

Three behaviours found while writing a Kyo course. Each section below is a self-contained draft for one GitHub
issue on [getkyo/kyo](https://github.com/getkyo/kyo/issues). Each one has a runnable minimal reproduction in this
module.

**Environment (all three):** Kyo `1.0.0-RC7` (`kyo-core`, `kyo-prelude`, `kyo-data`), Scala `3.9.0`, sbt `2.0.9`,
JDK 27 (Homebrew OpenJDK), macOS (arm64).

**Running the reproductions:**

```
sbt "repro/runMain repro.Repro1LayerFromAscription"
sbt "repro/runMain repro.Repro2LayerMemoPerInstance"
sbt "repro/runMain repro.Repro3HandleContMultiShot"   # forked with -Xmx256m
```

**Duplicates:** searching the tracker for `Layer.from`, `Layer.init`, `Layer memoization`, `handleCont`, `multi-shot
continuation` and `TypeMap missing value` turned up no matching issue (searched 2026-10-07). Possibly related, both
closed: #603 (`Env.runLayer ignores pending effects introduced by layers`) and #608 (`TypeMap.empty.get[Any] throws
fatal`).

---

## 1. `[BUG]: Layer.from with an ascribed type compiles but fails at runtime ("missing value of type A & B")`

### Summary

A two-dependency `Layer.from` behaves differently depending on whether its expected type is written out. Without
an ascription it works. With one, it compiles, then fails at runtime with a fatal `TypeMap` lookup for the
*intersection* of its dependencies.

### Reproduction

[`Repro1LayerFromAscription.scala`](src/main/scala/repro/Repro1LayerFromAscription.scala):

```scala
import kyo.*

final class A
final class B
final class C(val a: A, val b: B)

val deps: Layer[A & B, Any] = Layer(A()) and Layer(B())

val inferred                         = Layer.from((a: A, b: B) => C(a, b)) // works
val ascribed: Layer[C, Env[A & B]]   = Layer.from((a: A, b: B) => C(a, b)) // compiles, fails at runtime

Memo.run((deps to inferred).run) // ok
Memo.run((deps to ascribed).run) // Panic
```

### Actual

```
inferred: ok
ascribed: Panic(java.lang.RuntimeException: fatal: kyo.TypeMap of contents [TypeMap(...A -> ..., ...B -> ...)]
          missing value of type: [(repro.Repro1LayerFromAscription$.A & repro.Repro1LayerFromAscription$.B)])
returned: Panic(... same error ...)
explicit: ok
```

`returned` is the same definition with the expected type coming from a method's return type
(`def returned: Layer[C, Env[A & B]] = Layer.from(...)`). `explicit` passes the type arguments explicitly
(`Layer.from[A, B, C, Any](...)`) and works.

### Expected

Both definitions are equivalent and should build `C`. If that cannot be supported, it should be a compile error
rather than a runtime panic.

### Analysis

`Layer.from[A: Tag, B: Tag, C: Tag, S](f: (A, B) => C < S): Layer[C, Env[A & B] & S]` infers its type parameters
from the expected type before looking at the lambda. Matching `Env[A & B]` against `Env[A & B]` from the ascription
leaves the split between the two type parameters ambiguous, and the inferred `Tag[A]` ends up being the tag of the
intersection. The layer then calls `env.get` on that intersection, which no `TypeMap` contains.

Passing the type arguments explicitly (`Layer.from[A, B, C, Any](...)`) works around it.

### Suggestions

- Reject intersection type arguments at compile time for the individual dependency parameters of `Layer.from`,
  e.g. with a `NotIntersection` evidence like the one `Env.get` already requires.
- Or document the workaround in the `Layer.from` Scaladoc.

---

## 2. `[BUG]/[DOCS]: Layer.init builds a shared dependency once per dependent when the layer is a def`

### Summary

The `Layer` docs say: "layers use memoization to ensure efficiency when the same layer is referenced multiple times
in a dependency graph". The memo is keyed by layer *instance*. When a layer is defined with `def`, `Layer.init`
evaluates the expression once per dependent, which creates distinct instances. The "shared" dependency is then built
several times, and the dependents get different objects. Manual wiring with `to` / `and` is not affected.

### Reproduction

[`Repro2LayerMemoPerInstance.scala`](src/main/scala/repro/Repro2LayerMemoPerInstance.scala):

```scala
def dbLayer: Layer[Db, Sync] = Layer(Sync.defer { opened.incrementAndGet(); Db() })
val repoA   = Layer.from((db: Db) => RepoA(db))
val repoB   = Layer.from((db: Db) => RepoB(db))
val service = Layer.from[RepoA, RepoB, Service, Any]((a, b) => Service(a, b))

Layer.init[Service](service, repoA, repoB, dbLayer)                  // Db built twice
{ val db = dbLayer; Layer.init[Service](service, repoA, repoB, db) } // Db built once
dbLayer to (repoA and repoB) to service                               // Db built once
```

### Actual

```
def passed to Layer.init: databases opened = 2, repos share one = false
val passed to Layer.init: databases opened = 1, repos share one = true
def, manual wiring      : databases opened = 1, repos share one = true
```

### Expected

All three graphs are the same dependency graph, so `Db` should be built once in each case. At the very least, the
difference should be documented.

### Analysis

`Layer.init(layers*)` is a `transparent inline` macro, and the argument expression `dbLayer` is presumably spliced
at every place the generated graph needs a `Db`. Each splice calls the `def` again, producing a new `Layer` value.
`Memo` then treats the copies as different keys. A `def` is a natural way to write a parameterless layer
(e.g. `def configLayer: Layer[Config, Any]`) or a layer built from parameters (`def dbLayer(url: String)`), so this
is easy to hit, and the symptom (two connection pools, two caches...) appears only at runtime.

### Suggestions

- In `LayerMacros`, bind each argument to a local `val` once, before building the graph.
- Or document the instance-identity semantics of layer memoization and recommend `val`s.

---

## 3. `[BUG]: handleCont does not refuse a second resumption; multi-shot use runs out of memory`

### Summary

The Scaladoc of `ArrowEffect.handleCont` says:

> The clause applies the continuation at most once [...] Applying it a second time is refused at the first region it
> re-enters, as a use after release; a clause that resumes more than once uses `handleContRepeated`.

In practice, a second resumption is not refused. With one suspension the handler silently returns the multi-shot
result. With two, evaluation runs away until `OutOfMemoryError`.

### Reproduction

[`Repro3HandleContMultiShot.scala`](src/main/scala/repro/Repro3HandleContMultiShot.scala):

```scala
import kyo.*
import kyo.kernel.*

sealed trait Flip extends ArrowEffect[Const[Unit], Const[Boolean]]
def flip(using Frame): Boolean < Flip = ArrowEffect.suspend[Any](Tag[Flip], ())

// The same clause, with the two handler families.
def runRepeated[A](v: A < Flip)(using Frame): List[A] < Any =
  ArrowEffect.handleContRepeated(Tag[Flip], v.map(List(_)))(
    [C] => (_, cont) => cont(true).map(t => cont(false).map(f => t ++ f)))
def runSingleShot[A](v: A < Flip)(using Frame): List[A] < Any =
  ArrowEffect.handleCont(Tag[Flip], v.map(List(_)))(
    [C] => (_, cont) => cont(true).map(t => cont(false).map(f => t ++ f)))

val oneFlip  = flip
val twoFlips = for a <- flip; b <- flip yield (a, b)
```

### Actual (JVM with `-Xmx256m`)

```
handleContRepeated, 1 flip   returned List(true, false) after 33 ms
handleContRepeated, 2 flips  returned List((true,true), (true,false), (false,true), (false,false)) after 3 ms
handleCont, 1 flip           returned List(true, false) after 0 ms
handleCont, 2 flips          threw java.lang.OutOfMemoryError: Java heap space after 1748 ms
```

With the default heap, the two-flip case kept running for about 80 seconds before failing the same way.

### Expected

Following the Scaladoc, the second `cont(...)` call should be refused with a clear error, both for one and for two
suspensions. It should not succeed silently (one flip) or exhaust memory (two flips).

### Notes

- The one-flip case suggests the refusal never triggers. The two-flip case suggests that a continuation re-entered
  under `handleCont` keeps re-capturing regions without bound.
- Choosing `handleCont` instead of `handleContRepeated` is an easy mistake when writing a non-determinism handler.
  A fail-fast error naming `handleContRepeated` would turn a memory exhaustion that is hard to diagnose into a
  one-line fix.
