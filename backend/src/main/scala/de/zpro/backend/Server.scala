package de.zpro.backend

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.{DefaultJsonProtocol, JsObject}

import scala.language.postfixOps

object Server extends SprayJsonSupport with DefaultJsonProtocol {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "my-system")
    val initializer = new Initializer

    val route: Route = concat(
      (post & path("graphql")) {
        entity(as[JsObject]) { requestJson =>
          initializer.endpoint.graphQLEndpoint(requestJson)
        }
      }
    )
    Http().newServerAt("0.0.0.0", 8888).bind(route)
  }
}
