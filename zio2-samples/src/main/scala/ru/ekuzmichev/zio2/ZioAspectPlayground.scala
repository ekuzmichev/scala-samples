package ru.ekuzmichev.zio2

import zio._

object ZioAspectSimpleApp extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    ZIO.attempt("Hello!") @@ ZIOAspect.debug
}

object ZioAspectCompositionApp extends ZIOAppDefault {
  def download(url: String): ZIO[Any, Throwable, Chunk[Byte]] = ZIO.succeed(Chunk.empty)

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    ZIO.foreachPar(List("zio.dev", "google.com")) { url =>
      download(url) @@
        ZIOAspect.retry(Schedule.fibonacci(1.seconds)) @@
        ZIOAspect.loggedWith[Chunk[Byte]](file => s"Downloaded $url file with size of ${file.length} bytes")
    }
}
