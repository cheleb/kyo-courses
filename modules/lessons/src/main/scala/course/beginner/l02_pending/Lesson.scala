package course.beginner.l02_pending

import kyo.*

/** # B2 — The pending type `A < S`
  *
  * Every Kyo computation has the type `A < S`, read "an `A`, pending the effects `S`":
  *
  *   - `A` is the value produced once the computation runs;
  *   - `S` is the *set* of effects still to be handled, written as an intersection: `Sync & Abort[String]`.
  *
  * Key ideas of this lesson:
  *
  *   1. A plain value is already a computation with no pending effect: `42: Int < Any`.
  *   2. `map` / `flatMap` (and so for-comprehensions) chain computations and *union* their effects.
  *   3. A computation with fewer effects can be used where more are allowed (`Int < Sync` is an
  *      `Int < (Sync & Abort[String])`).
  *   4. Once every effect is handled (`A < Any`), `.eval` extracts the value.
  *   5. A computation is a description: sequencing it twice runs it twice.
  *
  * Run it with:
  * {{{
  * sbt "lessons/runMain course.beginner.l02_pending.PendingType"
  * }}}
  */
object PendingType extends KyoApp:

  // 1. Lifting: no wrapper needed, a plain value *is* a pending computation.
  val pure: Int < Any = 42

  // 2. `map` transforms the result; the effect set is unchanged.
  val doubled: Int < Any = pure.map(_ * 2)

  // `flatMap` chains a computation that has its own effects: the result needs both effect sets.
  val roll: Int < Sync         = Random.nextInt(6).map(_ + 1)
  val twoRolls: Int < Sync     = roll.flatMap(a => roll.map(b => a + b))
  val twoRollsFor: Int < Sync  =
    for
      a <- roll
      b <- roll
    yield a + b

  // 3. Effect sets are ordinary types, so subtyping works: fewer effects fit where more are allowed.
  val widened: Int < (Sync & Abort[String]) = twoRolls

  // Effects are combined from whatever each step needs; the compiler infers the union.
  val checked: Int < (Sync & Abort[String]) =
    for
      n <- roll
      _ <- Abort.when(n > 6)("impossible roll")
    yield n

  // 4. Only `A < Any` can be evaluated directly.
  val fortyTwo: Int = pure.eval

  // 5. Descriptions are re-run each time they are sequenced.
  val hello: Unit < Sync = Console.printLine("I am printed every time I am sequenced")

  run:
    for
      _ <- Console.printLine(s"pure = ${pure.eval}, doubled = ${doubled.eval}, fortyTwo = $fortyTwo")
      a <- twoRolls
      b <- twoRollsFor
      _ <- Console.printLine(s"two rolls: $a and $b")
      _ <- hello
      _ <- hello
    yield ()

end PendingType
