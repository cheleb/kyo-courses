package course.beginner.l08_capstone

import kyo.*

/** B8 solutions. */
object Exercises:

  import BankError.*

  def parseCommand(line: String): Command < Abort[BankError] =
    def amount(s: String): Long < Abort[BankError] =
      Abort.get(s.toLongOption.toRight(InvalidCommand(line)))

    line.trim.split("\\s+").toList match
      case "open" :: id :: Nil                  => Command.Open(id)
      case "deposit" :: id :: n :: Nil          => amount(n).map(Command.Deposit(id, _))
      case "withdraw" :: id :: n :: Nil         => amount(n).map(Command.Withdraw(id, _))
      case "transfer" :: from :: to :: n :: Nil => amount(n).map(Command.Transfer(from, to, _))
      case "balance" :: id :: Nil               => Command.Balance(id)
      case _                                    => Abort.fail(InvalidCommand(line))
  end parseCommand

  def openAccount(id: String): Unit < (Var[Accounts] & Abort[BankError]) =
    for
      accounts <- Var.get[Accounts]
      _        <- Abort.when(accounts.contains(id))(AccountExists(id))
      _        <- Var.setDiscard(accounts.updated(id, 0L))
    yield ()

  def balanceOf(id: String): Long < (Var[Accounts] & Abort[BankError]) =
    Var.use[Accounts](accounts => Abort.get(accounts.get(id).toRight(UnknownAccount(id))))

  def deposit(id: String, amount: Long): Long < (Var[Accounts] & Abort[BankError]) =
    for
      _       <- Abort.when(amount <= 0)(InvalidAmount(amount))
      balance <- balanceOf(id)
      updated  = balance + amount
      _       <- Var.updateDiscard[Accounts](_.updated(id, updated))
    yield updated

  def withdraw(id: String, amount: Long): Long < (Var[Accounts] & Abort[BankError]) =
    for
      _       <- Abort.when(amount <= 0)(InvalidAmount(amount))
      balance <- balanceOf(id)
      _       <- Abort.when(balance < amount)(InsufficientFunds(id, balance, amount))
      updated  = balance - amount
      _       <- Var.updateDiscard[Accounts](_.updated(id, updated))
    yield updated

  def transfer(from: String, to: String, amount: Long): Unit < (Env[BankConfig] & Var[Accounts] & Abort[BankError]) =
    for
      config <- Env.get[BankConfig]
      _      <- Abort.when(amount > config.maxTransfer)(LimitExceeded(config.maxTransfer, amount))
      _      <- withdraw(from, amount)
      _      <- deposit(to, amount)
    yield ()

  def execute(command: Command): String < (Env[BankConfig] & Env[Audit] & Var[Accounts] & Abort[BankError] & Sync) =
    def audited(event: String)(answer: String): String < (Env[Audit] & Sync) =
      Env.use[Audit](_.record(event)).andThen(answer)

    command match
      case Command.Open(id) =>
        openAccount(id).andThen(audited(s"open $id")(s"opened $id"))
      case Command.Deposit(id, n) =>
        deposit(id, n).map(balance => audited(s"deposit $id $n")(s"$id: $balance"))
      case Command.Withdraw(id, n) =>
        withdraw(id, n).map(balance => audited(s"withdraw $id $n")(s"$id: $balance"))
      case Command.Transfer(from, to, n) =>
        transfer(from, to, n).andThen(audited(s"transfer $from $to $n")(s"transferred $n from $from to $to"))
      case Command.Balance(id) =>
        balanceOf(id).map(balance => s"$id: $balance")
    end match
  end execute

  def session(lines: Seq[String]): Seq[String] < (Env[BankConfig] & Env[Audit] & Var[Accounts] & Sync) =
    Kyo.foreach(lines): line =>
      Var.get[Accounts].map: before =>
        Abort.fold[BankError](
          (answer: String) => answer,
          error => Var.setDiscard(before).andThen(s"error: ${describe(error)}")
        )(parseCommand(line).map(execute))

  def runSession(config: BankConfig, audit: Audit, initial: Accounts)(
      lines: Seq[String]
  ): (Accounts, Seq[String]) < Sync =
    Env.run(config)(Env.run(audit)(Var.runTuple(initial)(session(lines))))

end Exercises

/** Play with your bank: `sbt "solutions/runMain course.beginner.l08_capstone.BankApp"`. */
object BankApp extends KyoApp:
  run(BankCli.run(Exercises.session))
