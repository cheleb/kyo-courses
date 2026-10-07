import sbt.*
import sbt.Keys.*

object Dependencies {
  object Version {
    val kyo = "1.0.0-RC7"

  }

  val core = Seq(
    "io.getkyo" %% "kyo-core" % Version.kyo,
    "io.getkyo" %% "kyo-combinators" % Version.kyo,
    "io.getkyo" %% "kyo-data" % Version.kyo
  )

}
