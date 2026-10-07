package course.beginner.l08_capstone

import kyo.*

/** B8 capstone — replace every `???`, then run `sbt "exercises/testOnly course.beginner.l08_capstone.*"`.
  *
  * The domain (`Command`, `BankError`, `BankConfig`, `Audit`, `Accounts`) is defined in the lesson.
  */
object Exercises:

  /** Ex 1: parse one line of input (words separated by spaces):
    *   - `open <id>`, `deposit <id> <n>`, `withdraw <id> <n>`, `transfer <from> <to> <n>`, `balance <id>`;
    *   - anything else, including a non-numeric amount, fails with `InvalidCommand(line)`.
    *
    * Amounts are not validated here: `deposit alice -5` parses fine.
    */
  def parseCommand(line: String): Command < Abort[BankError] = ???

  /** Ex 2: open an account with a zero balance; fail with `AccountExists` if it is already open. */
  def openAccount(id: String): Unit < (Var[Accounts] & Abort[BankError]) = ???

  /** Ex 3: the balance of an account; fail with `UnknownAccount` if it does not exist. */
  def balanceOf(id: String): Long < (Var[Accounts] & Abort[BankError]) = ???

  /** Ex 4: add `amount` to the account and return the new balance. Fail with `InvalidAmount` if `amount <= 0`, or
    * `UnknownAccount`.
    */
  def deposit(id: String, amount: Long): Long < (Var[Accounts] & Abort[BankError]) = ???

  /** Ex 5: remove `amount` from the account and return the new balance. Fail with `InvalidAmount` if
    * `amount <= 0`, `UnknownAccount`, or `InsufficientFunds` if the balance is too low.
    */
  def withdraw(id: String, amount: Long): Long < (Var[Accounts] & Abort[BankError]) = ???

  /** Ex 6: move `amount` from one account to another. Fail with `LimitExceeded` if `amount` is above the configured
    * `maxTransfer`, and with the errors of `withdraw` / `deposit`.
    */
  def transfer(from: String, to: String, amount: Long): Unit < (Env[BankConfig] & Var[Accounts] & Abort[BankError]) =
    ???

  /** Ex 7: execute a command and return the answer for the user:
    *   - `Open(id)` → `"opened <id>"`, audit `"open <id>"`;
    *   - `Deposit(id, n)` → `"<id>: <new balance>"`, audit `"deposit <id> <n>"`;
    *   - `Withdraw(id, n)` → `"<id>: <new balance>"`, audit `"withdraw <id> <n>"`;
    *   - `Transfer(from, to, n)` → `"transferred <n> from <from> to <to>"`, audit `"transfer <from> <to> <n>"`;
    *   - `Balance(id)` → `"<id>: <balance>"`, no audit.
    *
    * Only successful commands are audited.
    */
  def execute(command: Command): String < (Env[BankConfig] & Env[Audit] & Var[Accounts] & Abort[BankError] & Sync) =
    ???

  /** Ex 8: parse and execute every line, in order, and return one answer per line. A failing line answers
    * `"error: <BankError.describe(e)>"`, leaves the accounts exactly as they were before that line, and does not
    * stop the session.
    *
    * Hint: read the state before each line; `Abort.fold` lets you restore it on failure.
    */
  def session(lines: Seq[String]): Seq[String] < (Env[BankConfig] & Env[Audit] & Var[Accounts] & Sync) = ???

  /** Ex 9: run a session with everything provided; return the final accounts and the answers. */
  def runSession(config: BankConfig, audit: Audit, initial: Accounts)(
      lines: Seq[String]
  ): (Accounts, Seq[String]) < Sync = ???

end Exercises

/** Play with your bank: `sbt "exercises/runMain course.beginner.l08_capstone.BankApp"`. */
object BankApp extends KyoApp:
  run(BankCli.run(Exercises.session))
