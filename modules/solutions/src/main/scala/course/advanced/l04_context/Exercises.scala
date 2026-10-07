package course.advanced.l04_context

import kyo.*
import kyo.kernel.*

/** A4 solutions. */
object Exercises:

  def currentUser(using Frame): String < User = ContextEffect.suspend(Tag[User])

  def withUser[A, S](name: String)(v: A < (User & S))(using Frame): A < S =
    ContextEffect.handleInheritable(Tag[User], name)(v)

  def currentDepth(using Frame): Int < Any = ContextEffect.suspend(Tag[Depth], 0)

  def deeper[A, S](v: A < S)(using Frame): A < S =
    ContextEffect.handleInheritable(Tag[Depth], 1, (outer: Int) => outer + 1)(v)

  def renderTree(tree: Tree)(using Frame): List[String] < Any =
    for
      depth    <- currentDepth
      children <- deeper(Kyo.foreach(tree.children)(renderTree))
    yield ("  " * depth + tree.name) :: children.flatten.toList

  def currentPath(using Frame): String < Any =
    ContextEffect.suspend(Tag[Spans], List.empty[String]).map(spans => if spans.isEmpty then "root" else spans.mkString("/"))

  def inSpan[A, S](name: String)(v: A < S)(using Frame): A < S =
    ContextEffect.handleInheritable(Tag[Spans], List(name), (outer: List[String]) => outer :+ name)(v)

  def inLocalSpan[A, S](name: String)(v: A < S)(using Frame): A < S =
    ContextEffect.handleNonInheritable(Tag[Spans], List(name), (outer: List[String]) => outer :+ name)(v)

end Exercises
