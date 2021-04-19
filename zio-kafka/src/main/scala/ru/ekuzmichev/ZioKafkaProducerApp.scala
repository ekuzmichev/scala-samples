package ru.ekuzmichev

import zio._
import zio.console.putStrLn
import zio.duration._
import zio.kafka.producer._
import zio.kafka.serde._

object ZioKafkaProducerApp extends App with SerdeComponent {
  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = produce.exitCode

  private def produce = {
    val producerSettings: ProducerSettings =
      ProducerSettings(List("localhost:9092"))
        .withCloseTimeout(30.seconds)

    Producer.make[Any, String, Msg](producerSettings, Serde.string, msgSerde).use { producer =>
      (producer.produce(
        "topic",
        "key",
        Msg("id", 1000L)
      ) <* putStrLn(s"Published msg"))
        .repeat(Schedule.spaced(10.seconds))
    }
  }
}
