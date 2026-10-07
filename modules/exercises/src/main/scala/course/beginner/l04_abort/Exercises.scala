package course.beginner.l04_abort

import kyo.*

/** B4 exercises — replace every `???`, then run `sbt "exercises/testOnly course.beginner.l04_abort.*"`.
  *
  * The error type `AgeError` is defined in the lesson.
  */
object Exercises:

  /** Ex 1: return `age` unchanged if it is between 0 and 150 included. Otherwise fail with `AgeError.Negative`
    * or `AgeError.TooOld`. Hint: `Abort.when` or `Abort.fail`.
    */
  def checkAge(age: Int): Int < Abort[AgeError] = ???

  /** Ex 2: convert `input` to an `Int` with `input.toInt`, turning a `NumberFormatException` into
    * `AgeError.NotANumber(input)`. Hint: `Abort.catching`.
    */
  def parseInt(input: String): Int < Abort[AgeError] = ???

  /** Ex 3: parse then check an age, reusing `parseInt` and `checkAge`. */
  def parseAge(input: String): Int < Abort[AgeError] = ???

  /** Ex 4: handle the `Abort` effect of `parseAge`: the result is a plain `Result`. Hint: `Abort.run`, `.eval`. */
  def safeParseAge(input: String): Result[AgeError, Int] = ???

  /** Ex 5: parse an age, falling back to `0` when it is invalid. Hint: `Abort.recover`.
    *
    * The remaining `Abort[Nothing]` means "may still panic": recovering from `AgeError` does not handle panics.
    */
  def ageOrZero(input: String): Int < Abort[Nothing] = ???

  /** Ex 6: describe the outcome of `parseAge`:
    *   - `"age: 42"` for a valid age,
    *   - `"not a number: abc"`, `"negative: -1"` or `"too old: 200"` for each error.
    *
    * Hint: `Abort.fold`, then a `match` on the error.
    */
  def describe(input: String): String < Abort[Nothing] = ???

  /** Ex 7: find the raw age of `name` in `users`; fail with `"unknown user: <name>"` if it is missing.
    * Hint: `Abort.get` with an `Either`.
    */
  def findUser(users: Map[String, String], name: String): String < Abort[String] = ???

  /** Ex 8: find the user, then parse their age. Both error types end up in the effect row. */
  def userAge(users: Map[String, String], name: String): Int < Abort[String | AgeError] = ???

  /** Ex 9: like `userAge`, but an unknown user gives `-1`. Age errors must stay unhandled. */
  def userAgeOrUnknown(users: Map[String, String], name: String): Int < Abort[AgeError] = ???

end Exercises
