package course.beginner.l05_env

import java.util.concurrent.ConcurrentLinkedQueue
import kyo.*
import scala.collection.concurrent.TrieMap
import scala.jdk.CollectionConverters.*

/** Services used in this lesson and its exercises. */
final case class AppConfig(appName: String, maxUsers: Int)

trait UserRepo:
  def find(id: Int): Option[String] < Sync
  def save(id: Int, name: String): Unit < Sync
  def count: Int < Sync

object UserRepo:
  /** Creating the repository allocates mutable state, so creation itself is a side effect. */
  def inMemory(initial: Map[Int, String] = Map.empty): UserRepo < Sync =
    Sync.defer:
      val users = TrieMap.from(initial)
      new UserRepo:
        def find(id: Int): Option[String] < Sync          = Sync.defer(users.get(id))
        def save(id: Int, name: String): Unit < Sync      = Sync.defer(users.update(id, name))
        def count: Int < Sync                             = Sync.defer(users.size)

trait Logger:
  def log(message: String): Unit < Sync

object Logger:
  /** The production logger. */
  val console: Logger = message => Console.printLine(s"[log] $message")

/** A logger for tests: remembers every message. */
final class RecordingLogger extends Logger:
  private val buffer                         = ConcurrentLinkedQueue[String]()
  def log(message: String): Unit < Sync      = Sync.defer(buffer.add(message)).unit
  def messages: List[String] < Sync          = Sync.defer(buffer.asScala.toList)
