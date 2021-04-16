package ru.ekuzmichev

import zio.{App, ExitCode, URIO}

object ZioKafkaApp extends App {
  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = ???
}