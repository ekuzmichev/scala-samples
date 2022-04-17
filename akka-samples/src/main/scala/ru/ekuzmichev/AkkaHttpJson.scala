package ru.ekuzmichev

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import de.heikoseeberger.akkahttpjackson.JacksonSupport

import java.util.UUID

case class Person(name: String, age: Int)
case class UserAdded(id: String, timestamp: Long)

// ============================== //
// 1. Spray Json
// ============================== //
import spray.json._

trait PersonJsonProtocol extends DefaultJsonProtocol {
  implicit val personFormat: RootJsonFormat[Person]       = jsonFormat2(Person)
  implicit val userAddedFormat: RootJsonFormat[UserAdded] = jsonFormat2(UserAdded)
}

// json formats provide way to convert case classes to/from spray internal json AST
// SprayJsonSupport turns these AST's into requests/responses that akka-http understands
object AkkaHttpSprayJson extends App with PersonJsonProtocol with SprayJsonSupport {
  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "HttpServer")

  val route: Route = (path("api" / "user") & post) {
    entity(as[Person]) { _: Person => complete(UserAdded(UUID.randomUUID().toString, System.currentTimeMillis())) }
  }

  Http().newServerAt("localhost", 8081).bind(route)
}

// ============================== //
// 2. Circe Json
// ============================== //

object AkkaHttpCirceJson extends App with FailFastCirceSupport {
  import io.circe.generic.auto._

  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "HttpServer")

  val route: Route = (path("api" / "user") & post) {
    entity(as[Person]) { _: Person => complete(UserAdded(UUID.randomUUID().toString, System.currentTimeMillis())) }
  }

  Http().newServerAt("localhost", 8081).bind(route)
}

// ============================== //
// 3. Jackson
// ============================== //

object AkkaHttpJacksonJson extends App with JacksonSupport {

  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "HttpServer")

  val route: Route = (path("api" / "user") & post) {
    entity(as[Person]) { _: Person => complete(UserAdded(UUID.randomUUID().toString, System.currentTimeMillis())) }
  }

  Http().newServerAt("localhost", 8081).bind(route)
}
