package course.advanced.l04_context

import course.KyoSuite
import kyo.*
import kyo.kernel.*
import Exercises.*

class L04Spec extends KyoSuite:

  /** The value of `v` here and in a forked fiber. */
  def hereAndInFork[A](v: A < Any): (A, A) < (Async & Scope) =
    v.map(here => Fiber.init(v).map(_.get).map(fork => (here, fork)))

  test("Ex 1: currentUser reads the bound user"):
    assertEquals(ContextEffect.handleInheritable(Tag[User], "ada")(currentUser).eval, "ada")

  test("Ex 2: withUser binds the user, the innermost binding wins"):
    assertEquals(withUser("ada")(currentUser).eval, "ada")
    assertEquals(withUser("ada")(withUser("grace")(currentUser)).eval, "grace")
    assertEquals(withUser("ada")(currentUser.map(u => withUser("grace")(currentUser).map(i => s"$u,$i"))).eval, "ada,grace")

  test("Ex 3: currentDepth defaults to 0"):
    assertEquals(currentDepth.eval, 0)
    assertEquals(ContextEffect.handleInheritable(Tag[Depth], 7)(currentDepth).eval, 7)

  test("Ex 4: deeper nests"):
    assertEquals(deeper(currentDepth).eval, 1)
    assertEquals(deeper(deeper(deeper(currentDepth))).eval, 3)
    assertEquals(deeper(currentDepth.map(d => deeper(currentDepth).map(e => (d, e)))).eval, (1, 2))

  test("Ex 5: renderTree"):
    val tree = Tree("root", List(Tree("a", List(Tree("a1"))), Tree("b")))
    assertEquals(renderTree(tree).eval, List("root", "  a", "    a1", "  b"))
    assertEquals(deeper(renderTree(Tree("leaf"))).eval, List("  leaf"))

  test("Ex 6: currentPath defaults to root"):
    assertEquals(currentPath.eval, "root")
    assertEquals(ContextEffect.handleInheritable(Tag[Spans], List("x", "y"))(currentPath).eval, "x/y")

  test("Ex 7: inSpan appends to the path"):
    assertEquals(inSpan("api")(inSpan("users")(currentPath)).eval, "api/users")
    assertEquals(inSpan("api")(currentPath.map(p => inSpan("db")(currentPath).map(q => s"$p | $q"))).eval, "api | api/db")

  test("Ex 7: inSpan is inherited by forks"):
    assertEquals(runKyo(inSpan("api")(inSpan("users")(hereAndInFork(currentPath)))), ("api/users", "api/users"))

  test("Ex 8: inLocalSpan restarts the region in forks"):
    assertEquals(runKyo(inSpan("api")(inLocalSpan("users")(hereAndInFork(currentPath)))), ("api/users", "users"))
