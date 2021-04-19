package ru.ekuzmichev

import zio.kafka.serde.Serde

trait SerdeComponent {

  import com.goyeau.kafka.streams.circe.CirceSerdes._
  import io.circe.generic.auto._
  import org.apache.kafka.common.serialization.{ Serde => KSerde }

  lazy val msgSerde: Serde[Any, Msg] = Serde(implicitly[KSerde[Msg]])
}
