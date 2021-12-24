package ru.ekuzmichev.ce

import cats.effect.{ IO, IOApp }

object alloc {
  import cats.effect.MonadCancel
  import cats.effect.std.Semaphore
  import cats.effect.syntax.all._
  import cats.syntax.all._

  def guarded[F[_], R, A, E](s: Semaphore[F], alloc: F[R])(
    use: R => F[A]
  )(release: R => F[Unit])(implicit F: MonadCancel[F, E]): F[A] =
    F uncancelable { poll =>
      for {
        r          <- alloc
        _          <- poll(s.acquire).onCancel(release(r))
        releaseAll = s.release >> release(r)
        a          <- poll(use(r)).guarantee(releaseAll)
      } yield a
    }
}

object Cancel extends IOApp.Simple {
  override def run: IO[Unit] = {
    import cats.effect.IO

    for {
      fib <- (IO.uncancelable(_ => IO.canceled >> IO.println("This will print as cancelation is suppressed")) >>
              IO.println("This will never be called as we are canceled as soon as the uncancelable block finishes")).start
      res <- fib.join
    } yield ()
  }
}
