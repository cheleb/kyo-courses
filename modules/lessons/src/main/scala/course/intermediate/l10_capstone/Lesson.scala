package course.intermediate.l10_capstone

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kyo.*

/** # I10 — Capstone: a concurrent job pipeline
  *
  * This capstone combines the whole Intermediate part:
  *
  *   - a `Channel` of jobs consumed by a pool of worker fibers (I4, I5);
  *   - per-attempt timeouts and retries of transient failures (I4, I8);
  *   - `Log` warnings for every failed attempt (I8);
  *   - `Env` for the configuration and the processor (B5), `Scope` for the channels and fibers (I6);
  *   - typed errors kept apart: retryable vs permanent (B4, I1).
  *
  * {{{
  *   jobs ──put──▶ [ jobs channel ] ──take──▶ worker 1 ─┐
  *                                  ──take──▶ worker 2 ─┼──put──▶ [ results channel ] ──takeExactly──▶ outcomes
  *                                  ──take──▶ worker n ─┘
  * }}}
  *
  * Everything you need is defined below; you implement the pipeline in `Exercises.scala`, then watch it run:
  * {{{
  * sbt "exercises/runMain course.intermediate.l10_capstone.PipelineApp"
  * }}}
  */
object Lesson

final case class Job(id: Int, payload: String)

/** Failures of the processor: only `Retryable` ones are worth trying again. */
sealed trait JobError derives CanEqual:
  def message: String
final case class Retryable(message: String) extends JobError
final case class Permanent(message: String) extends JobError

enum Outcome derives CanEqual:
  case Done(jobId: Int, result: String, attempts: Int)
  case Failed(jobId: Int, error: String, attempts: Int)

  def id: Int = this match
    case Done(jobId, _, _)   => jobId
    case Failed(jobId, _, _) => jobId
end Outcome

final case class PipelineConfig(workers: Int, maxRetries: Int, retryDelay: Duration, jobTimeout: Duration)

trait Processor:
  def process(job: Job): String < (Async & Abort[Retryable | Permanent])

/** What one attempt at processing a job does. */
enum Step derives CanEqual:
  case Succeed(after: Duration)
  case FailRetryable
  case FailPermanent
  case Hang

/** A processor for tests and demos: follows a script of steps per job (one step per attempt; `default` once the
  * script is exhausted), and records the number of attempts and the peak number of jobs processed at once.
  */
final class ScriptedProcessor(script: Map[Int, List[Step]], default: Step = Step.Succeed(20.millis))
    extends Processor:
  private val attemptCounts = ConcurrentHashMap[Int, Int]()
  private val running       = AtomicInteger()
  private val peak          = AtomicInteger()

  def attempts(jobId: Int): Int < Sync = Sync.defer(attemptCounts.getOrDefault(jobId, 0))
  def peakConcurrency: Int < Sync      = Sync.defer(peak.get)

  def process(job: Job): String < (Async & Abort[Retryable | Permanent]) =
    val start = Sync.defer:
      val attempt = attemptCounts.merge(job.id, 1, _ + _)
      val now     = running.incrementAndGet()
      peak.accumulateAndGet(now, math.max)
      script.getOrElse(job.id, Nil).lift(attempt - 1).getOrElse(default)
    start.map: step =>
      Sync.ensure(Sync.defer(running.decrementAndGet())):
        step match
          case Step.Succeed(after) => Async.sleep(after).andThen(s"processed ${job.payload}")
          case Step.FailRetryable  => Async.sleep(5.millis).andThen(Abort.fail(Retryable(s"job ${job.id} hiccup")))
          case Step.FailPermanent  => Abort.fail(Permanent(s"job ${job.id} is invalid"))
          case Step.Hang           => Async.never[String]
  end process
end ScriptedProcessor
