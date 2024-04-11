package ru.ekuzmichev

import com.fasterxml.jackson.core.JsonFactory
import zio.stream.ZStream
import zio.{Unsafe, ZIO}

import java.io.ByteArrayOutputStream
import java.time.LocalDate

object StreamingJsonApp extends App {

  case class Metric(name: String, date: LocalDate, value: Double)
  case class MetricsMessage(id: String, metrics: Seq[Metric])

  val metricsMessage = MetricsMessage(
    id = "123",
    metrics = Seq(
      Metric(name = "memory", date = LocalDate.now(), value = 12.75),
      Metric(name = "cpu", date = LocalDate.now(), value = 18.5)
    )
  )

  def toJsonByteArray(metric: Metric): Array[Byte] = {
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val jsonFactory           = new JsonFactory()
    val jsonGenerator         = jsonFactory.createGenerator(byteArrayOutputStream)

    import jsonGenerator._

    writeStartObject()
    writeStringField("name", metric.name)
    writeStringField("date", metric.date.toString)
    writeNumberField("value", metric.value)
    writeEndObject()
    close()

    byteArrayOutputStream.toByteArray
  }

  def toJsonByteArrayOutputStream(metricsMessage: MetricsMessage): ByteArrayOutputStream = {
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val jsonFactory           = new JsonFactory()
    val jsonGenerator         = jsonFactory.createGenerator(byteArrayOutputStream)

    import jsonGenerator._
    writeStartObject()
    writeStringField("id", metricsMessage.id)
    writeArrayFieldStart("metrics")
    metricsMessage.metrics.foreach { metric =>
      val bytes = toJsonByteArray(metric)
      writeRawValue(new String(bytes))
    }
    writeEndArray()
    writeEndObject()
    close()

    byteArrayOutputStream
  }

  Unsafe.unsafe { implicit unsafe =>
    zio.Runtime.default.unsafe.run {
      val metricsStream = ZStream.fromIterable(
        Seq(
          Metric(name = "memory", date = LocalDate.now(), value = 12.75),
          Metric(name = "cpu", date = LocalDate.now(), value = 18.5)
        )
      )

      ZStream
        .fromIterable("{\"id\":\"123\",\"metrics\":[".getBytes)
        .concat(
          ZStream.fromZIO(metricsStream.runCount).flatMap { count =>
            ZStream
              .fromIterable(
                Seq(
                  Metric(name = "memory", date = LocalDate.now(), value = 12.75),
                  Metric(name = "cpu", date = LocalDate.now(), value = 18.5)
                ).zipWithIndex
              )
              .flatMap { case (metric, index) =>
                ZStream.fromIterable(toJsonByteArray(metric)).concat {
                  if (index + 1 < count) ZStream.fromIterable(",".getBytes) else ZStream.empty
                }
              }
          }
        )
        .concat(ZStream.fromIterable("]}".getBytes))
        .runCollect
        .tap(bytes => ZIO.succeed(println(new String(bytes.toArray))))

//      ZStream
//        .fromOutputStreamWriter(outputStream => byteArrayOutputStream.writeTo(outputStream))
//        .tap(byte => ZIO.succeed(println(byte)))
//        .runDrain
    }
  }

  println(new String(toJsonByteArrayOutputStream(metricsMessage).toByteArray))
}
