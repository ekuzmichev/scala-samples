package ru.ekuzmichev.ce

object spawn {
  import cats.effect.syntax.all._
  import cats.effect.{ MonadCancel, Spawn }
  import cats.syntax.all._

  trait Server[F[_]] {
    def accept: F[Connection[F]]
  }

  trait Connection[F[_]] {
    def read: F[Array[Byte]]
    def write(bytes: Array[Byte]): F[Unit]
    def close: F[Unit]
  }

  def endpoint[F[_]: Spawn](server: Server[F])(body: Array[Byte] => F[Array[Byte]]): F[Unit] = {

    def handle(conn: Connection[F]): F[Unit] =
      for {
        request  <- conn.read
        response <- body(request)
        _        <- conn.write(response)
      } yield ()

    val handler = MonadCancel[F] uncancelable { poll =>
      poll(server.accept) flatMap { conn => handle(conn).guarantee(conn.close).start }
    }

    handler.foreverM
  }
}
