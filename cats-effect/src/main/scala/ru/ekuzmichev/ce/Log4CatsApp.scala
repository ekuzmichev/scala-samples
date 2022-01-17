package ru.ekuzmichev.ce

import cats.effect.{ IO, IOApp, Sync }
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

// No support for cats effect 3
// java.lang.NoSuchMethodError: io.chrisdavenport.log4cats.slf4j.Slf4jLogger$.getLoggerFromSlf4j(Lorg/slf4j/Logger;Lcats/effect/kernel/Sync;)Lio/chrisdavenport/log4cats/SelfAwareStructuredLogger;
object Log4CatsApp extends IOApp.Simple {
  override def run: IO[Unit] =
    for {
      _ <- doSmth[IO]()
    } yield ()

  def doSmth[F[_]: Sync](): F[Unit] =
    for {
      logger <- Slf4jLogger.create[F]
      _      <- logger.info(s"Starting...")
      _ <- Sync[F].delay(logger.info("I could do anything")).onError {
            case e => logger.error(e)("Something Went Wrong in safelyDoThings")
          }
      _ <- logger.info(s"Exiting...")
    } yield ()
}
