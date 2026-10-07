package course.intermediate.l10_capstone

import course.KyoSuite
import course.intermediate.l08_resilience.RecordingLog
import kyo.*
import Exercises.*
import Outcome.*

class L10Spec extends KyoSuite:

  val config = PipelineConfig(workers = 3, maxRetries = 2, retryDelay = 10.millis, jobTimeout = 100.millis)

  def job(id: Int) = Job(id, s"p$id")

  /** Runs `v` with the configuration, the processor and a recording logger; returns the result and the log. */
  def withDeps[A](processor: Processor)(v: A < (Deps & Async & Scope & Abort[Closed])): (A, List[String]) =
    val log = RecordingLog()
    runKyo:
      for
        a       <- Log.let(log.safe)(Scope.run(Env.run(config)(Env.run(processor)(v))))
        _       <- Log.flush
        entries <- log.entries
      yield (a, entries)
  end withDeps

  test("Ex 1: summary"):
    val outcomes = Seq(Done(1, "a", 1), Failed(2, "x", 3), Done(3, "b", 2))
    assertEquals(summary(outcomes), "2 done, 1 failed, 6 attempts")
    assertEquals(summary(Nil), "0 done, 0 failed, 0 attempts")

  test("Ex 2: processOnce succeeds"):
    val (r, _) = withDeps(ScriptedProcessor(Map.empty))(Abort.run(processOnce(job(1))))
    assertEquals(r, Result.succeed("processed p1"))

  test("Ex 2: processOnce turns a timeout into a retryable error"):
    val (r, _) = withDeps(ScriptedProcessor(Map(1 -> List(Step.Hang))))(Abort.run(processOnce(job(1))))
    assertEquals(r, Result.fail(Retryable("job 1 timed out")))

  test("Ex 2: processOnce keeps permanent errors"):
    val (r, _) = withDeps(ScriptedProcessor(Map(1 -> List(Step.FailPermanent))))(Abort.run(processOnce(job(1))))
    assertEquals(r, Result.fail(Permanent("job 1 is invalid")))

  test("Ex 3: processWithRetry retries retryable failures and logs them"):
    val processor = ScriptedProcessor(Map(1 -> List(Step.FailRetryable, Step.Succeed(5.millis))))
    val (r, log)  = withDeps(processor)(processWithRetry(job(1)))
    assertEquals(r, Done(1, "processed p1", 2))
    assertEquals(log, List("warn: job 1 attempt 1 failed: job 1 hiccup"))

  test("Ex 3: processWithRetry does not retry permanent failures"):
    val processor = ScriptedProcessor(Map(1 -> List(Step.FailPermanent)))
    val (r, log)  = withDeps(processor)(processWithRetry(job(1)))
    assertEquals(r, Failed(1, "job 1 is invalid", 1))
    assertEquals(log, List("warn: job 1 attempt 1 failed: job 1 is invalid"))
    assertEquals(runKyo(processor.attempts(1)), 1)

  test("Ex 3: processWithRetry gives up after maxRetries"):
    val processor = ScriptedProcessor(Map(1 -> List.fill(5)(Step.Hang)))
    val (r, log)  = withDeps(processor)(processWithRetry(job(1)))
    assertEquals(r, Failed(1, "job 1 timed out", 3))
    assertEquals(log.size, 3)

  test("Ex 4: worker processes every job until the channel closes"):
    val program =
      for
        jobs    <- Channel.init[Job](10)
        results <- Channel.init[Outcome](10)
        _       <- Kyo.foreachDiscard(1 to 3)(i => jobs.put(job(i)))
        _       <- Fiber.init(jobs.closeAwaitEmpty)
        _       <- worker(jobs, results)
        out     <- results.takeExactly(3)
      yield out.map(_.id).toList
    assertEquals(withDeps(ScriptedProcessor(Map.empty, Step.Succeed(1.millis)))(program)._1, List(1, 2, 3))

  test("Ex 5: runPipeline processes every job, sorted, with the right outcomes"):
    val processor = ScriptedProcessor(Map(2 -> List(Step.FailRetryable), 3 -> List(Step.FailPermanent)))
    val (out, _)  = withDeps(processor)(runPipeline((1 to 6).map(job)))
    assertEquals(
      out.toList,
      List(
        Done(1, "processed p1", 1),
        Done(2, "processed p2", 2),
        Failed(3, "job 3 is invalid", 1),
        Done(4, "processed p4", 1),
        Done(5, "processed p5", 1),
        Done(6, "processed p6", 1)
      )
    )

  test("Ex 5: runPipeline uses all workers, never more"):
    val processor = ScriptedProcessor(Map.empty, Step.Succeed(30.millis))
    val (out, _)  = withDeps(processor)(runPipeline((1 to 12).map(job)))
    assertEquals(out.size, 12)
    assertEquals(runKyo(processor.peakConcurrency), config.workers)

  test("Ex 5: runPipeline with no jobs"):
    assertEquals(withDeps(ScriptedProcessor(Map.empty))(runPipeline(Nil))._1, Seq.empty)

  test("Ex 6: runApp"):
    val out = runKyo(runApp(config, ScriptedProcessor(Map.empty, Step.Succeed(1.millis)))((1 to 4).map(job)))
    assertEquals(summary(out), "4 done, 0 failed, 4 attempts")
