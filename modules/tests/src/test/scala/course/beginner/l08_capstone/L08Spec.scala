package course.beginner.l08_capstone

import course.KyoSuite
import kyo.*
import BankError.*
import Command.*
import Exercises.*

class L08Spec extends KyoSuite:

  val config = BankConfig(maxTransfer = 100)

  /** Runs a bank operation from `accounts`; returns the outcome and the final accounts. */
  def bank[A](accounts: Accounts)(v: A < (Env[BankConfig] & Var[Accounts] & Abort[BankError])): (Result[BankError, A], Accounts) =
    Env.run(config)(Var.runTuple(accounts)(Abort.run(v))).map(_.swap).eval

  test("Ex 1: parseCommand parses every command"):
    def parse(line: String) = Abort.run(parseCommand(line)).eval
    assertEquals(parse("open alice"), Result.succeed(Open("alice")))
    assertEquals(parse("deposit alice 100"), Result.succeed(Deposit("alice", 100)))
    assertEquals(parse("  withdraw   alice 30 "), Result.succeed(Withdraw("alice", 30)))
    assertEquals(parse("transfer alice bob 20"), Result.succeed(Transfer("alice", "bob", 20)))
    assertEquals(parse("balance bob"), Result.succeed(Balance("bob")))
    assertEquals(parse("deposit alice -5"), Result.succeed(Deposit("alice", -5)))

  test("Ex 1: parseCommand rejects invalid input"):
    def parse(line: String) = Abort.run(parseCommand(line)).eval
    assertEquals(parse("deposit alice lots"), Result.fail(InvalidCommand("deposit alice lots")))
    assertEquals(parse("open"), Result.fail(InvalidCommand("open")))
    assertEquals(parse("rob the bank"), Result.fail(InvalidCommand("rob the bank")))

  test("Ex 2: openAccount"):
    assertEquals(bank(Map.empty)(openAccount("alice")), (Result.succeed(()), Map("alice" -> 0L)))
    assertEquals(bank(Map("alice" -> 5L))(openAccount("alice"))._1, Result.fail(AccountExists("alice")))

  test("Ex 3: balanceOf"):
    assertEquals(bank(Map("alice" -> 5L))(balanceOf("alice"))._1, Result.succeed(5L))
    assertEquals(bank(Map.empty)(balanceOf("bob"))._1, Result.fail(UnknownAccount("bob")))

  test("Ex 4: deposit"):
    assertEquals(bank(Map("alice" -> 5L))(deposit("alice", 10)), (Result.succeed(15L), Map("alice" -> 15L)))
    assertEquals(bank(Map("alice" -> 5L))(deposit("alice", 0))._1, Result.fail(InvalidAmount(0)))
    assertEquals(bank(Map.empty)(deposit("bob", 10))._1, Result.fail(UnknownAccount("bob")))

  test("Ex 5: withdraw"):
    assertEquals(bank(Map("alice" -> 50L))(withdraw("alice", 20)), (Result.succeed(30L), Map("alice" -> 30L)))
    assertEquals(bank(Map("alice" -> 50L))(withdraw("alice", -1))._1, Result.fail(InvalidAmount(-1)))
    assertEquals(bank(Map("alice" -> 5L))(withdraw("alice", 6))._1, Result.fail(InsufficientFunds("alice", 5, 6)))
    assertEquals(bank(Map.empty)(withdraw("bob", 1))._1, Result.fail(UnknownAccount("bob")))

  test("Ex 6: transfer moves money"):
    val accounts = Map("alice" -> 50L, "bob" -> 0L)
    assertEquals(bank(accounts)(transfer("alice", "bob", 20)), (Result.succeed(()), Map("alice" -> 30L, "bob" -> 20L)))

  test("Ex 6: transfer respects the limit and the balances"):
    val accounts = Map("alice" -> 500L, "bob" -> 0L)
    assertEquals(bank(accounts)(transfer("alice", "bob", 101))._1, Result.fail(LimitExceeded(100, 101)))
    assertEquals(bank(accounts)(transfer("bob", "alice", 1))._1, Result.fail(InsufficientFunds("bob", 0, 1)))
    assertEquals(bank(accounts)(transfer("alice", "carol", 1))._1, Result.fail(UnknownAccount("carol")))

  def runExecute(accounts: Accounts, audit: Audit)(command: Command): Result[BankError, String] =
    runKyo(Env.run(config)(Env.run(audit)(Var.run(accounts)(Abort.run(execute(command))))))

  test("Ex 7: execute answers and audits successful commands"):
    val audit    = RecordingAudit()
    val accounts = Map("alice" -> 50L, "bob" -> 0L)
    val exec     = runExecute(accounts, audit)
    assertEquals(exec(Open("carol")), Result.succeed("opened carol"))
    assertEquals(exec(Deposit("alice", 10)), Result.succeed("alice: 60"))
    assertEquals(exec(Withdraw("alice", 10)), Result.succeed("alice: 40"))
    assertEquals(exec(Transfer("alice", "bob", 5)), Result.succeed("transferred 5 from alice to bob"))
    assertEquals(exec(Balance("alice")), Result.succeed("alice: 50"))
    assertEquals(
      runKyo(audit.recorded),
      List("open carol", "deposit alice 10", "withdraw alice 10", "transfer alice bob 5")
    )

  test("Ex 7: execute does not audit failures"):
    val audit = RecordingAudit()
    assertEquals(runExecute(Map.empty, audit)(Deposit("alice", 10)), Result.fail(UnknownAccount("alice")))
    assertEquals(runKyo(audit.recorded), Nil)

  test("Ex 8: session answers every line and keeps going after errors"):
    val program = Env.run(config)(Env.run(RecordingAudit(): Audit)(Var.runTuple(Map.empty: Accounts)(
      session(Seq("open alice", "deposit alice 100", "withdraw alice 500", "oops", "balance alice"))
    )))
    val (accounts, answers) = runKyo(program)
    assertEquals(
      answers.toList,
      List(
        "opened alice",
        "alice: 100",
        "error: insufficient funds on alice: 100 < 500",
        "error: invalid command: oops",
        "alice: 100"
      )
    )
    assertEquals(accounts, Map("alice" -> 100L))

  test("Ex 8: a failed transfer leaves the accounts untouched"):
    val program = Env.run(config)(Env.run(RecordingAudit(): Audit)(Var.runTuple(Map("alice" -> 50L): Accounts)(
      session(Seq("transfer alice bob 20"))
    )))
    val (accounts, answers) = runKyo(program)
    assertEquals(answers.toList, List("error: unknown account: bob"))
    assertEquals(accounts, Map("alice" -> 50L))

  test("Ex 9: runSession provides everything"):
    val audit = RecordingAudit()
    val (accounts, answers) = runKyo(
      runSession(config, audit, Map("alice" -> 10L))(Seq("open bob", "transfer alice bob 10", "balance bob"))
    )
    assertEquals(accounts, Map("alice" -> 0L, "bob" -> 10L))
    assertEquals(answers.toList, List("opened bob", "transferred 10 from alice to bob", "bob: 10"))
    assertEquals(runKyo(audit.recorded), List("open bob", "transfer alice bob 10"))
