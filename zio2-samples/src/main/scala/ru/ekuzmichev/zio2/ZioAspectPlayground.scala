package ru.ekuzmichev.zio2

import zio._

object ZioAspectApp extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    ZIO.attempt("Hello!") @@ ZIOAspect.debug
}