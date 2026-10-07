# Kyo effect-system course

A hands-on course on [Kyo](https://getkyo.io) (`1.0.0-RC7`), in three parts: **Beginner**, **Intermediate**, **Advanced**.
The full outline lives in [`specs/plan.md`](specs/plan.md).

## Requirements

- JDK 25 or newer (Kyo RC7 does not run on older JDKs)
- sbt 2.x

## Layout

| Module                | Content |
|-----------------------|---------|
| `modules/lessons`     | `Lesson.scala` per topic: explained, runnable examples |
| `modules/exercises`   | `Exercises.scala` per topic: stubs (`???`) for you to fill in |
| `modules/solutions`   | Reference solutions, same packages and names as the exercises |
| `modules/tests`       | munit specs, shared by `exercises` and `solutions` |

## How to follow a lesson

1. Read and run the lesson:
   ```
   sbt "lessons/runMain course.beginner.l01_hello.HelloKyo"
   ```
2. Fill in the `???` in `modules/exercises/.../Exercises.scala`.
3. Run the tests until they are green:
   ```
   sbt "exercises/testOnly course.beginner.l01_hello.*"
   ```
4. Stuck? Compare with `modules/solutions/.../Exercises.scala`.

`sbt solutions/testFull` must always pass. `sbt exercises/test` is expected to fail until you have solved the exercises.
(sbt 2's `test` only re-runs tests affected by your changes; `testFull` runs everything.)

## Lessons

### Beginner — Effects as values
- B1 `l01_hello` — Hello Kyo: `KyoApp`, `Console`, sequencing
- B2 `l02_pending` — The pending type `A < S`
- B3 `l03_sync` — `Sync`: controlled side effects, `Clock`, `Random`, deterministic testing
- B4 `l04_abort` — `Abort`: typed failures, `Result`, recovering, exceptions and panics
- B5 `l05_env` — `Env`: dependency injection, providing production vs test services
- B6 `l06_var` — `Var`: local state, `Var.run` / `Var.runTuple`, a stack machine with `Var` + `Abort`
- B7 `l07_data` — kyo-data: `Maybe`, `Result`, `Chunk`, `Duration`, `Instant`
- B8 `l08_capstone` — **Capstone**: a bank-account CLI combining `Abort`, `Env`, `Var` and `Sync`.
  Once solved, play with it: `sbt "exercises/runMain course.beginner.l08_capstone.BankApp"`
