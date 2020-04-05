package ru.ekuzmichev.cats.retry

object CatsRetrySamples extends App {
  import cats.effect.{ ContextShift, IO, Timer }
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.global
  import scala.language.postfixOps
  import retry._
  import RetryPolicies._

  implicit val timer: Timer[IO] = IO.timer(global)

  val ioPolicy = limitRetries[IO](10) join capDelay(100 milliseconds, exponentialBackoff[IO](10 milliseconds))

  def ioOnFailure(failedValue: Int, details: RetryDetails): IO[Unit] =
    IO(println(s"Rolled a $failedValue, retrying ...   $details"))

  case class LoadedDie(rolls: Int*) {
    private var i = -1

    def roll(): Int = {
      i = i + 1
      if (i >= rolls.length) {
        i = 0
      }
      rolls(i)
    }
  }

  val loadedDie = LoadedDie(2, 5, 4, 1, 3, 2, 6)

  val io = retryingM(ioPolicy, (_: Int) == 6, ioOnFailure)(IO(loadedDie.roll()))

  implicit def ctx: ContextShift[IO] = IO.contextShift(global)

  val res = io.timeout(1 minute).unsafeRunSync()

  println(res)

}
