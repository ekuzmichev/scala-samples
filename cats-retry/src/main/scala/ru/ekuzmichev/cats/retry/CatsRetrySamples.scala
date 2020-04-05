package ru.ekuzmichev.cats.retry

object CatsRetrySamples {
  import cats.effect.IO
  import retry._

  val retryFiveTimes: RetryPolicy[IO] = RetryPolicies.limitRetries[IO](5)
}
