package ru.ekuzmichev.ce

import cats.Id
import cats.effect.IO
import cats.effect.std.Random
import cats.effect.Outcome
import cats.implicits._

import scala.concurrent.duration._

object ExampleCatsEffectTestkit {
  def retry[A](ioa: IO[A], delay: FiniteDuration, max: Int, random: Random[IO]): IO[A] =
    if (max <= 1)
      ioa
    else
      ioa handleErrorWith { _ =>
        random
          .betweenLong(0L, delay.toNanos)
          .flatMap(ns => IO.sleep(ns.nanos) *> retry(ioa, delay * 2, max - 1, random))
      }
}

import cats.effect.testkit.TestControl
import munit.CatsEffectSuite

class Suite extends CatsEffectSuite {
  import ExampleCatsEffectTestkit._

  test("retry at least 3 times until success") {
    case object TestException extends RuntimeException

    var attempts = 0
    val action = IO {
      attempts += 1

      if (attempts != 3)
        throw TestException
      else
        "success!"
    }

    val program = Random.scalaUtilRandom[IO].flatMap(retry(action, 1.minute, 5, _))

    TestControl.executeEmbed(program).assertEquals("success!")
  }

  test("backoff appropriately between attempts") {
    case object TestException extends RuntimeException

    val action               = IO.raiseError(TestException)
    val program: IO[Nothing] = Random.scalaUtilRandom[IO].flatMap(retry(action, 1.minute, 5, _))

    val result: IO[TestControl[Nothing]] = TestControl.execute(program)

    result.flatMap { control =>
      for {
        _ <- control.results.assertEquals(None)
        _ <- control.tick

        _ <- 0.until(4).toList.traverse { i =>
              for {
                _ <- control.results.assertEquals(None)

                interval <- control.nextInterval
                _        <- IO(assert(interval >= 0.nanos))
                _        <- IO(assert(interval < (1 << i).minute))
                _        <- control.advanceAndTick(interval)
              } yield ()
            }

        _ <- control.results.assertEquals(Some(Outcome.errored[Id, Throwable, Nothing](TestException)))
      } yield ()
    }
  }
}
