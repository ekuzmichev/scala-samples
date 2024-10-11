package ru.ekuzmichev

import com.rabbitmq.client.{Connection, ConnectionFactory}
import zio.ZIO.attemptBlocking
import zio._
import zio.amqp.model._
import zio.logging._

import java.net.URI

/**
 * Experiment with usage of [[zio.ZPool]] holding [[com.rabbitmq.client.Connection]]:
 *
 * - RabbitMQ broker is in shutdown state
 *
 * - Application starts
 *
 * - [[zio.ZPool]] initiates [[com.rabbitmq.client.Connection]] creation (obviously failed in the background)
 *
 * - RabbitMQ broker starts
 *
 * - One gets connection from [[zio.ZPool]]
 *
 * '''AS IS''': Fiber is failed while performing `pool.get` due to `.orDie` call inside release connection function
 * and calling `.close()` on [[com.rabbitmq.client.Connection]]
 *
 * '''TO BE''': [[com.rabbitmq.client.Connection]] retrieval is successful
 *
 * '''Solution''': Usage of `.ignore` instead of `.orDie` inside release connection function,
 * retry and invalidation of connection in [[zio.ZPool]] solves the problem
 */
object ZioAmqpExperimentWithCustomReconnectionLogicApp extends ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger(ConsoleLoggerConfig.default.copy(format = LogFormat.colored))

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    (for {
      pool <- ZPool.make(Amqp.connect(AMQPConfig.default), 1)

      _ <- Console.readLine("Continue?\n")

      // Either stop RabbitMQ (if it is running) or start it (if it is down) & press ENTER

      connection <- getConnectionFrom(pool)

      _ <- ZIO.log(s"Connection from pool: ${toString(connection)}")
    } yield ())
      .catchAll(t => ZIO.logError(s"Caught error: $t"))

  private def getConnectionFrom(pool: ZPool[Throwable, Connection]): Task[Connection] =
    ZIO.scoped {
      ZIO.log(s"Getting connection from pool") *>
        pool.get
          .tap(connection =>
            ZIO.when(!connection.isOpen)(
              ZIO.log(s"Invalidating closed connection") *>
                pool.invalidate(connection) *>
                ZIO.log(s"Invalidated closed connection") *>
                ZIO.fail(new RuntimeException(s"Connection is closed and invalidated"))
            )
          )
          .tapBoth(
            t => ZIO.logError(s"Failed to get connection from pool: $t"),
            connection => ZIO.log(s"Got connection from pool: ${toString(connection)}")
          )
    }.retry(Schedule.recurs(5) && Schedule.exponential(500.milliseconds))

  private def toString(connection: Connection): String =
    s"$connection (${if (connection.isOpen) "open" else "not open"})"
}

object Amqp {
  def connect(factory: ConnectionFactory): ZIO[Scope, Throwable, Connection] =
    ZIO.acquireRelease {
      attemptBlocking(factory.newConnection())
        .tapBoth(
          t => ZIO.logError(s"Failed to create connection: $t"),
          connection => ZIO.log(s"Created new connection: ${toString(connection)}")
        )
    } { connection =>
      ZIO
        .attempt(connection.close())
        .tapError(t => ZIO.logError(s"Failed to close connection: $t"))
        .ignore // zio.amqp.Amqp dies here and this leads to fiber failure and inability to perform kind of retrying
    }

  def connect(uri: URI): ZIO[Scope, Throwable, Connection] = {
    val factory = new ConnectionFactory()
    factory.setUri(uri)
    connect(factory)
  }

  def connect(amqpConfig: AMQPConfig): ZIO[Scope, Throwable, Connection] = {
    val factory = new ConnectionFactory()
    factory.setUri(amqpConfig.toUri)
    connect(factory)
  }

  private def toString(connection: Connection): String =
    s"$connection (${if (connection.isOpen) "open" else "not open"})"
}
