package example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._

object HighLevelRest extends App {
  implicit val system = ActorSystem("HighLevelREST")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  var simpleRoute: Route = {
    path("home") {
      get {
        complete(StatusCodes.OK)
      } ~
      post {
        complete(StatusCodes.Forbidden)
      }
    } ~
      path("about") {
        get {
          complete(StatusCodes.Found)
        }
      }
  }
  Http().bindAndHandle(simpleRoute,"localhost",8080)

}
