# Kyo Course Plan

Source of truth for the course. Target: Kyo `1.0.0-RC7`, Scala 3.9, JDK 25+.
Scope: Kyo only (`kyo-core`, `kyo-prelude`, `kyo-data`, `kyo-combinators`). ZIO / Cats interop comes later.

## Build layout

```
modules/
  lessons/    src/main/scala/course/<level>/lNN_<topic>/Lesson.scala     explained, runnable examples
  exercises/  src/main/scala/course/<level>/lNN_<topic>/Exercises.scala  stubs with ???
  solutions/  src/main/scala/course/<level>/lNN_<topic>/Exercises.scala  reference solutions (same package/names)
  tests/      src/test/scala/course/<level>/lNN_<topic>/LNNSpec.scala    shared munit specs
```

- `exercises` and `solutions` both depend on `lessons` and both compile the shared `tests` sources.
- `sbt exercises/test` is red until the learner solves the stubs; `sbt solutions/test` must always be green.
- Rule: a lesson is done only when it compiles and `solutions/test` passes against RC7. API names are verified
  with the compiler / Metals docs, never from memory.

## Part 1 — Beginner: "Effects as values"

| #  | Lesson                       | Key concepts |
|----|------------------------------|--------------|
| B1 | Hello Kyo                    | `KyoApp`, `run:` blocks, `Console.printLine` |
| B2 | The pending type `A < S`     | lifting, `map`/`flatMap`, for-comprehensions, effect rows, `.eval` |
| B3 | `Sync`                       | `Sync.defer`, `Clock`, `Random`, deferred side effects |
| B4 | `Abort`                      | `Abort.fail`, `Abort.when`, `Abort.run` → `Result`, catching, recovery |
| B5 | `Env`                        | `Env.get`, `Env.run`, multiple services |
| B6 | `Var`                        | `Var.get/set/update`, `Var.run` |
| B7 | kyo-data toolbox             | `Maybe`, `Result`, `Chunk`, `Duration`, `Instant` |
| B8 | Capstone: bank-account CLI   | `Abort` + `Env` + `Var` + `Sync` |

## Part 2 — Intermediate: "Composing and concurrency"

| #   | Lesson                          | Key concepts |
|-----|---------------------------------|--------------|
| I1  | Stacking & handler order        | `Abort.run(Var.run(…))` vs `Var.run(Abort.run(…))` |
| I2  | `Choice` & `Emit`               | non-determinism, search, emitting values |
| I3  | `Stream`                        | build, transform, chunk, run |
| I4  | `Async` & fibers                | sleep, fibers, race, parallel, timeout, interruption |
| I5  | Concurrency primitives          | `Queue`, `Channel`, `Hub`, atomics, `Meter` |
| I6  | `Scope` & resources             | acquire/release, `ensure` |
| I7  | `Layer`                         | wiring `Env` dependency graphs |
| I8  | Resilience                      | `Schedule`, retry, `Log` |
| I9  | Testing Kyo code                | running `Async` in munit, controlled time |
| I10 | Capstone: concurrent job pipeline | producers/consumers, retry, resources, logging |

## Part 3 — Advanced: "Inside the kernel"

| #  | Lesson                       | Key concepts |
|----|------------------------------|--------------|
| A1 | How `<` works                | suspensions, `Tag`, `Frame`, handler loop |
| A2 | `ArrowEffect`                | `Ask` / `Say`, `ArrowEffect.suspend` |
| A3 | Writing handlers             | `handle`, `handleCont`, `done`/`recover`, `handleLoop(State)` |
| A4 | `ContextEffect`              | defaults, inheritable handlers (`Level`) |
| A5 | Effects across fibers        | `Isolate` |
| A6 | Stack safety & performance   | `Loop`, `Kyo` ops, `Arrow` |
| A7 | Rebuilding the basics        | mini `Abort` and `Var` on the kernel |
| A8 | Capstone: custom effect      | e.g. `Cache` / `Transaction` with multiple handlers |
