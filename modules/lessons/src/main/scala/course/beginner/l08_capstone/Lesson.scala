package course.beginner.l08_capstone

import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import kyo.*
import scala.jdk.CollectionConverters.*

/** # B8 — Capstone: a bank-account CLI
  *
  * This capstone puts the whole Beginner part together:
  *
  *   - `Abort[BankError]` for business errors (B4),
  *   - `Env[BankConfig]` and `Env[Audit]` for configuration and services (B5),
  *   - `Var[Accounts]` for the state of the bank (B6),
  *   - `Sync` for the audit trail and the console (B3).
  *
  * The domain, the error messages and the interactive console loop are given below. You implement the bank itself
  * in `Exercises.scala`, then play with it:
  * {{{
  * sbt "exercises/runMain course.beginner.l08_capstone.BankApp"
  * > open alice
  * > deposit alice 100
  * > transfer alice bob 20
  * error: unknown account: bob
  * > quit
  * }}}
  *
  * The interesting design question is *atomicity*: when a command fails halfway (a transfer that withdrew the money
  * but cannot deposit it), the state must go back to what it was before the command. Intermediate lesson I1 shows
  * how the order of handlers decides this; here you do it explicitly.
  */
object Lesson

/** All accounts, by id, with their balance. */
type Accounts = Map[String, Long]

final case class BankConfig(maxTransfer: Long)

enum Command derives CanEqual:
  case Open(id: String)
  case Deposit(id: String, amount: Long)
  case Withdraw(id: String, amount: Long)
  case Transfer(from: String, to: String, amount: Long)
  case Balance(id: String)

enum BankError derives CanEqual:
  case InvalidCommand(input: String)
  case UnknownAccount(id: String)
  case AccountExists(id: String)
  case InvalidAmount(amount: Long)
  case InsufficientFunds(id: String, balance: Long, requested: Long)
  case LimitExceeded(limit: Long, requested: Long)

object BankError:
  def describe(error: BankError): String = error match
    case InvalidCommand(input)                 => s"invalid command: $input"
    case UnknownAccount(id)                    => s"unknown account: $id"
    case AccountExists(id)                     => s"account already exists: $id"
    case InvalidAmount(amount)                 => s"invalid amount: $amount"
    case InsufficientFunds(id, balance, req)   => s"insufficient funds on $id: $balance < $req"
    case LimitExceeded(limit, requested)       => s"transfer limit exceeded: $requested > $limit"

/** Records every successful change to the accounts. */
trait Audit:
  def record(event: String): Unit < Sync

object Audit:
  val console: Audit = event => Console.printLineErr(s"[audit] $event")

final class RecordingAudit extends Audit:
  private val events                      = ConcurrentLinkedQueue[String]()
  def record(event: String): Unit < Sync  = Sync.defer(events.add(event)).unit
  def recorded: List[String] < Sync       = Sync.defer(events.asScala.toList)

/** The interactive loop: reads commands until `quit` (or the end of the input) and prints the answers. */
object BankCli:

  type Session = Seq[String] => Seq[String] < (Env[BankConfig] & Env[Audit] & Var[Accounts] & Sync)

  private val readCommand: String < Sync =
    for
      _    <- Console.print("> ")
      line <- Abort.recover[IOException](_ => "quit")(Console.readLine)
    yield Maybe(line).getOrElse("quit").trim

  def loop(session: Session): Unit < (Env[BankConfig] & Env[Audit] & Var[Accounts] & Sync) =
    readCommand.map:
      case "quit" => Console.printLine("bye")
      case ""     => loop(session)
      case line   =>
        for
          answers <- session(Seq(line))
          _       <- Kyo.foreachDiscard(answers)(Console.printLine)
          _       <- loop(session)
        yield ()

  def run(session: Session): Unit < Sync =
    Env.run(BankConfig(maxTransfer = 1000)):
      Env.run(Audit.console):
        Var.run(Map.empty: Accounts):
          Console.printLine("commands: open <id> | deposit <id> <n> | withdraw <id> <n> | " +
            "transfer <from> <to> <n> | balance <id> | quit").andThen(loop(session))

end BankCli
