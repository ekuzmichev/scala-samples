package ru.ekuzmichev

import com.rabbitmq.client.Connection
import zio._
import zio.amqp.model._
import zio.logging._

/**
 * Experiment with usage of [[zio.ZPool]] holding [[com.rabbitmq.client.Connection]]:
 *
 *   - RabbitMQ broker is in shutdown state
 *
 *   - Application starts
 *
 *   - [[zio.ZPool]] initiates [[com.rabbitmq.client.Connection]] creation (obviously failed in the background)
 *
 *   - RabbitMQ broker starts
 *
 *   - One gets connection from [[zio.ZPool]]
 *
 * '''AS IS''': Fiber is failed while performing `pool.get` due to `.orDie` call inside release connection function and
 * calling `.close()` on [[com.rabbitmq.client.Connection]]
 *
 * '''TO BE''': [[com.rabbitmq.client.Connection]] retrieval is successful
 *
 * '''Solution''': Usage of `.tapErrorCause` & `.retry` on `pool.get` solves the problem
 */
object ZioAmqpExperimentWithRetryApp extends ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger(ConsoleLoggerConfig.default.copy(format = LogFormat.colored))

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    (for {
      pool <- ZPool.make(zio.amqp.Amqp.connect(AMQPConfig.default), 1)

      _ <- Console.readLine("Continue?\n")

      // Either stop RabbitMQ (if it is running) or start it (if it is down) & press ENTER

      connection <-
        pool.get
          .tapErrorCause(cause => ZIO.logError(s"Failed to get connection from pool: $cause"))
          .retry(Schedule.recurs(3) && Schedule.exponential(500.milliseconds))

      _ <- ZIO.log(s"Connection from pool: ${toString(connection)}")
    } yield ())
      /**
       * The problem here:
       *   - RabbitMQ is running
       *   - App is started
       *   - RabbitMQ is stopped
       *   - Enter pressed in console
       *   - The exception is thrown in the thread other than main due to .orDie in release action
       *     com.rabbitmq.client.AlreadyClosedException: connection is already closed due to connection error; protocol
       *     method: #method<connection.close>(reply-code=320, reply-text=CONNECTION_FORCED - broker forced connection
       *     closure with reason 'shutdown', class-id=0, method-id=0)
       *   - The exception/cause is not caught and it is difficult to handle it
       */
      .catchAllCause(cause => ZIO.logError(s"Caught error: $cause"))

  private def toString(connection: Connection): String =
    s"$connection (${if (connection.isOpen) "open" else "not open"})"
}
