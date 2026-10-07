package course.advanced.l08_capstone

import kyo.*
import kyo.kernel.*

/** A8 solutions. */
object Exercises:

  def get(key: String)(using Frame): Maybe[String] < KV =
    ArrowEffect.suspend[Maybe[String]](Tag[KV], KVOp.Get(key))

  def put(key: String, value: String)(using Frame): Unit < KV =
    ArrowEffect.suspend[Unit](Tag[KV], KVOp.Put(key, value))

  def delete(key: String)(using Frame): Boolean < KV =
    ArrowEffect.suspend[Boolean](Tag[KV], KVOp.Delete(key))

  def increment(key: String)(using Frame): Int < KV =
    for
      current <- get(key)
      next = current.flatMap(s => Maybe.fromOption(s.toIntOption)).getOrElse(0) + 1
      _ <- put(key, next.toString)
    yield next

  def runInMemory[A, S](initial: Map[String, String])(v: A < (KV & S))(using Frame): (Map[String, String], A) < S =
    ArrowEffect.handleLoopState(Tag[KV], initial, v)(
      [C] =>
        (store, op) =>
          op match
            case KVOp.Get(key)        => Loop.continue(store, Maybe.fromOption(store.get(key)).asInstanceOf[C])
            case KVOp.Put(key, value) => Loop.continue(store.updated(key, value), ().asInstanceOf[C])
            case KVOp.Delete(key)     => Loop.continue(store - key, store.contains(key).asInstanceOf[C])
      ,
      done = (store, a) => (store, a)
    )

  def runReadOnly[A, S](data: Map[String, String])(v: A < (KV & S))(using Frame): A < (S & Abort[String]) =
    ArrowEffect.handleLoop(Tag[KV], v)(
      [C] =>
        op =>
          op match
            case KVOp.Get(key)    => Loop.continue(Maybe.fromOption(data.get(key)).asInstanceOf[C])
            case KVOp.Put(key, _) => Abort.fail(s"read-only: put $key")
            case KVOp.Delete(key) => Abort.fail(s"read-only: delete $key")
    )

  /** Performs `op` against the next `KV` handler. */
  private def forward[C](op: KVOp[C])(using Frame): C < KV =
    op match
      case KVOp.Get(key)        => get(key).asInstanceOf[C < KV]
      case KVOp.Put(key, value) => put(key, value).asInstanceOf[C < KV]
      case KVOp.Delete(key)     => delete(key).asInstanceOf[C < KV]

  def runLogged[A, S](v: A < (KV & S))(using Frame): A < (S & KV & Emit[String]) =
    ArrowEffect.handleLoop(Tag[KV], v)(
      [C] =>
        op =>
          val line = op match
            case KVOp.Get(key)        => s"get $key"
            case KVOp.Put(key, value) => s"put $key=$value"
            case KVOp.Delete(key)     => s"delete $key"
          Emit.value(line).andThen(forward(op)).map(answer => Loop.continue(answer))
    )

  def runCounted[A, S](v: A < (KV & S))(using Frame): (Int, A) < (S & KV) =
    ArrowEffect.handleLoopState(Tag[KV], 0, v)(
      [C] => (count, op) => forward(op).map(answer => Loop.continue(count + 1, answer)),
      done = (count, a) => (count, a)
    )

  def visitAll(users: Seq[String])(using Frame): Map[String, Int] < KV =
    for
      _      <- Kyo.foreachDiscard(users)(user => increment(s"visits:$user"))
      counts <- Kyo.foreach(users.distinct)(user => get(s"visits:$user").map(v => user -> v.fold(0)(_.toInt)))
    yield counts.toMap

end Exercises
