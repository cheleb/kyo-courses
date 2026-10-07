package course.advanced.l04_context

import kyo.*
import kyo.kernel.*

/** A4 exercises — replace every `???`, then run `sbt "exercises/testOnly course.advanced.l04_context.*"`.
  *
  * The effects `User`, `Depth` and `Spans`, and the `Tree` type, are defined in the lesson.
  */
object Exercises:

  /** Ex 1: read the current user. It is required: `User` stays in the row until bound. */
  def currentUser(using Frame): String < User = ???

  /** Ex 2: bind the current user to `name` for `v`. */
  def withUser[A, S](name: String)(v: A < (User & S))(using Frame): A < S = ???

  /** Ex 3: read the current depth, `0` when nothing is bound. No effect may appear in the row. */
  def currentDepth(using Frame): Int < Any = ???

  /** Ex 4: run `v` one level deeper: `1` when nothing is bound, otherwise the outer depth plus one. */
  def deeper[A, S](v: A < S)(using Frame): A < S = ???

  /** Ex 5: render a tree, one line per node, indented by two spaces per level of depth. The root is rendered at the
    * current depth. Use `currentDepth` and `deeper`, *not* a depth parameter.
    *
    * {{{
    * root
    *   a
    *     a1
    *   b
    * }}}
    */
  def renderTree(tree: Tree)(using Frame): List[String] < Any = ???

  /** Ex 6: the current tracing path, spans joined with `/`, or `"root"` when there is none. */
  def currentPath(using Frame): String < Any = ???

  /** Ex 7: run `v` inside a new span named `name`, appended to the current path. Forks inherit it. */
  def inSpan[A, S](name: String)(v: A < S)(using Frame): A < S = ???

  /** Ex 8: like `inSpan`, but *non-inheritable*: a fork started inside restarts the region, seeing only `name`. */
  def inLocalSpan[A, S](name: String)(v: A < S)(using Frame): A < S = ???

end Exercises
