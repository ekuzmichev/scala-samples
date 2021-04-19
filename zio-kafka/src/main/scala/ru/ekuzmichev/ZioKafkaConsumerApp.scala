package ru.ekuzmichev

import zio._
import zio.console.putStrLn
import zio.duration._
import zio.kafka.consumer._
import zio.kafka.serde._

object ZioKafkaConsumerApp extends App with SerdeComponent {
  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = consume.exitCode

  private def consume = {
    val consumerSettings: ConsumerSettings =
      ConsumerSettings(List("localhost:9092"))
        .withGroupId("group")
        .withClientId("client")
        .withCloseTimeout(30.seconds)

    val subscription: Subscription = Subscription.topics("topic")

    Consumer.make(consumerSettings).use { consumer =>
      consumer.consumeWith(subscription, Serde.string, msgSerde) {
        case (key, value) => putStrLn(s"Received message $key: $value")
      }
    }
  }

}
