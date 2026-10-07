package course.intermediate.l10_capstone

import kyo.*

/** I10 solutions. */
object Exercises:

  type Deps = Env[PipelineConfig] & Env[Processor]

  def summary(outcomes: Seq[Outcome]): String =
    val done     = outcomes.count(_.isInstanceOf[Outcome.Done])
    val attempts = outcomes.map {
      case Outcome.Done(_, _, n)   => n
      case Outcome.Failed(_, _, n) => n
    }.sum
    s"$done done, ${outcomes.size - done} failed, $attempts attempts"
  end summary

  def processOnce(job: Job): String < (Deps & Async & Abort[Retryable | Permanent]) =
    for
      config    <- Env.get[PipelineConfig]
      processor <- Env.get[Processor]
      result <- Abort.recover[Timeout](_ => Abort.fail(Retryable(s"job ${job.id} timed out"))):
        Async.timeout(config.jobTimeout)(processor.process(job))
    yield result

  def processWithRetry(job: Job): Outcome < (Deps & Async) =
    for
      config   <- Env.get[PipelineConfig]
      attempts <- AtomicInt.init
      attempt = attempts.incrementAndGet.map: n =>
        Abort.recover[Retryable | Permanent] { (error: Retryable | Permanent) =>
          Log.warn(s"job ${job.id} attempt $n failed: ${error.message}").andThen(Abort.fail(error))
        }(processOnce(job))
      result <- Abort.run[Retryable | Permanent]:
        Retry[Retryable](Schedule.fixed(config.retryDelay).take(config.maxRetries))(attempt)
      count <- attempts.get
    yield result match
      case Result.Success(value)  => Outcome.Done(job.id, value, count)
      case Result.Failure(error)  => Outcome.Failed(job.id, error.message, count)
      case Result.Panic(ex)       => Outcome.Failed(job.id, s"panic: ${ex.getMessage}", count)

  def worker(jobs: Channel[Job], results: Channel[Outcome]): Unit < (Deps & Async & Abort[Closed]) =
    jobs.streamUntilClosed(maxChunkSize = 1).foreach(job => processWithRetry(job).map(results.put))

  def runPipeline(jobs: Seq[Job]): Seq[Outcome] < (Deps & Async & Scope & Abort[Closed]) =
    for
      config   <- Env.get[PipelineConfig]
      queue    <- Channel.init[Job](config.workers)
      results  <- Channel.init[Outcome](jobs.size max 1)
      _        <- Kyo.foreachDiscard(1 to config.workers)(_ => Fiber.init(worker(queue, results)))
      _        <- Fiber.init(Kyo.foreachDiscard(jobs)(queue.put).andThen(queue.closeAwaitEmpty))
      outcomes <- results.takeExactly(jobs.size)
    yield outcomes.sortBy(_.id)

  def runApp(config: PipelineConfig, processor: Processor)(jobs: Seq[Job]): Seq[Outcome] < (Async & Abort[Closed]) =
    Scope.run(Env.run(config)(Env.run(processor)(runPipeline(jobs))))

end Exercises

/** Watch the pipeline run: `sbt "solutions/runMain course.intermediate.l10_capstone.PipelineApp"`. */
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
