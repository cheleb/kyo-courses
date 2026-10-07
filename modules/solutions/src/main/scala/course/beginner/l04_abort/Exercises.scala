package course.beginner.l04_abort

import kyo.*

/** B4 solutions. */
object Exercises:

  def checkAge(age: Int): Int < Abort[AgeError] =
    for
      _ <- Abort.when(age < 0)(AgeError.Negative(age))
      _ <- Abort.when(age > 150)(AgeError.TooOld(age))
    yield age

  def parseInt(input: String): Int < Abort[AgeError] =
    Abort.catching[NumberFormatException](_ => AgeError.NotANumber(input))(input.toInt)

  def parseAge(input: String): Int < Abort[AgeError] =
    parseInt(input).map(checkAge)

  def safeParseAge(input: String): Result[AgeError, Int] =
    Abort.run(parseAge(input)).eval

  def ageOrZero(input: String): Int < Abort[Nothing] =
    Abort.recover[AgeError](_ => 0)(parseAge(input))

  def describe(input: String): String < Abort[Nothing] =
    Abort.fold[AgeError](
      age => s"age: $age",
      {
        case AgeError.NotANumber(input) => s"not a number: $input"
        case AgeError.Negative(age)     => s"negative: $age"
        case AgeError.TooOld(age)       => s"too old: $age"
      }
    )(parseAge(input))

  def findUser(users: Map[String, String], name: String): String < Abort[String] =
    Abort.get(users.get(name).toRight(s"unknown user: $name"))

  def userAge(users: Map[String, String], name: String): Int < Abort[String | AgeError] =
    findUser(users, name).map(parseAge)

  def userAgeOrUnknown(users: Map[String, String], name: String): Int < Abort[AgeError] =
    Abort.recover[String](_ => -1)(userAge(users, name))

end Exercises
