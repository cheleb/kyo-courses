package course.intermediate.l10_capstone

import kyo.*

/** I10 capstone — replace every `???`, then run `sbt "exercises/testOnly course.intermediate.l10_capstone.*"`.
  *
  * The domain (`Job`, `Outcome`, `Retryable`, `Permanent`, `PipelineConfig`, `Processor`) is defined in the lesson.
  */
object Exercises:

  /** Effects needed by the pipeline. */
  type Deps = Env[PipelineConfig] & Env[Processor]

  /** Ex 1: summarize outcomes as `"<n> done, <m> failed, <k> attempts"`. */
  def summary(outcomes: Seq[Outcome]): String = ???

  /** Ex 2: process a job once with the `Processor`, giving up after the configured `jobTimeout`: a timeout counts as
    * `Retryable("job <id> timed out")`.
    */
  def processOnce(job: Job): String < (Deps & Async & Abort[Retryable | Permanent]) = ???

  /** Ex 3: process a job with retries, and report the outcome (never fail):
    *   - retry `Retryable` failures up to `maxRetries` times, `retryDelay` apart; never retry `Permanent` ones;
    *   - log a `warn` `"job <id> attempt <n> failed: <message>"` after *every* failed attempt;
    *   - `Done(id, result, attempts)` or `Failed(id, lastErrorMessage, attempts)`.
    *
    * Hint: count the attempts in an `AtomicInt`.
    */
  def processWithRetry(job: Job): Outcome < (Deps & Async) = ???

  /** Ex 4: a worker: take jobs from `jobs` one at a time until the channel is closed, and put each outcome into
    * `results`.
    *
    * Careful: `streamUntilClosed()` takes as many elements as available per chunk, so one worker could grab every
    * queued job for itself. Take them one by one.
    */
  def worker(jobs: Channel[Job], results: Channel[Outcome]): Unit < (Deps & Async & Abort[Closed]) = ???

  /** Ex 5: the pipeline: start `workers` worker fibers, feed them every job through a channel (closed without losing
    * jobs once all are queued), collect exactly one outcome per job, and return them sorted by job id.
    */
  def runPipeline(jobs: Seq[Job]): Seq[Outcome] < (Deps & Async & Scope & Abort[Closed]) = ???

  /** Ex 6: run the pipeline with everything provided. */
  def runApp(config: PipelineConfig, processor: Processor)(jobs: Seq[Job]): Seq[Outcome] < (Async & Abort[Closed]) =
    ???

end Exercises

/** Watch the pipeline run: `sbt "exercises/runMain course.intermediate.l10_capstone.PipelineApp"`. */
object PipelineApp extends KyoApp:
  val config = PipelineConfig(workers = 3, maxRetries = 2, retryDelay = 50.millis, jobTimeout = 300.millis)
  val processor = ScriptedProcessor(
    Map(
      2 -> List(Step.FailRetryable, Step.Succeed(30.millis)),
      4 -> List(Step.FailPermanent),
      5 -> List(Step.Hang, Step.Hang, Step.Hang),
      7 -> List(Step.FailRetryable, Step.FailRetryable, Step.Succeed(10.millis))
    ),
    default = Step.Succeed(100.millis)
  )
  val jobs = (1 to 9).map(i => Job(i, s"payload-$i"))

  run:
    Log.withConsoleLogger:
      for
        outcomes <- Exercises.runApp(config, processor)(jobs)
        _        <- Kyo.foreachDiscard(outcomes)(o => Console.printLine(o.toString))
        _        <- Console.printLine(Exercises.summary(outcomes))
        peak     <- processor.peakConcurrency
        _        <- Console.printLine(s"peak concurrency: $peak (workers: ${config.workers})")
      yield ()
end PipelineApp
