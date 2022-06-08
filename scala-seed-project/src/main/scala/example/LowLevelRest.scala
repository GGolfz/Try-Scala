package example

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, ResponseEntity, StatusCodes, Uri}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import example.GuitarDB.{CreateGuitar, FindAllGuitars, FindGuitar, GuitarCreated}
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
case class Guitar(make:String, model:String)
object GuitarDB {
  case class CreateGuitar(guitar: Guitar)
  case class GuitarCreated(id: Int)
  case class FindGuitar(id: Int)
  case object FindAllGuitars
}
class GuitarDB extends Actor with ActorLogging {
  import GuitarDB._
  var guitars: Map[Int,Guitar] = Map()
  var currentGuitarId: Int = 0

  override def receive: Receive = {
    case FindAllGuitars =>
      log.info("Searching for all guitars")
      sender() ! guitars.values.toList
    case FindGuitar(id) =>
      log.info(s"Searching guitar by id: $id")
      sender() ! guitars.get(id)
    case CreateGuitar(guitar) =>
      log.info(s"Adding guitar $guitar with id $currentGuitarId")
      guitars = guitars + (currentGuitarId -> guitar)
      sender() ! GuitarCreated(currentGuitarId)
      currentGuitarId += 1
  }
}

trait GuitarStoreJSONProtocol extends DefaultJsonProtocol {
  implicit val guitarFormat = jsonFormat2(Guitar)
}

object LowLevelRest extends App with GuitarStoreJSONProtocol {
  implicit val system = ActorSystem("LowLevelRest")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  val guitarDB = system.actorOf(Props[GuitarDB], "GuitarDB")
  val guitarList = List(Guitar("Fender","Stratocaster"),Guitar("Gibson","Les Paul"),Guitar("Martin","LX1"))

  guitarList.foreach{ guitar => guitarDB ! CreateGuitar(guitar)}

  implicit val defaultTimeout: Timeout = Timeout(3 seconds)

  var requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar"),_,_,_) =>
      var query = uri.query()
      if(query.isEmpty) {
        val guitarsFuture: Future[List[Guitar]] = (guitarDB ? FindAllGuitars).mapTo[List[Guitar]]
        guitarsFuture.map {
          guitars =>
            HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, guitars.toJson.prettyPrint))
        }
      } else {
        try {
          var id = query.get("id").orNull.toInt
          val guitarFuture: Future[Option[Guitar]] = (guitarDB ? FindGuitar(id)).mapTo[Option[Guitar]]
          guitarFuture.map {
            case Some(guitar) =>
              HttpResponse(StatusCodes.OK,entity = HttpEntity(ContentTypes.`application/json`,guitar.toJson.prettyPrint))
          }
        } catch {
          case e: RuntimeException =>
            Future(HttpResponse(StatusCodes.NotFound))
        }
      }
    case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"),_,entity: HttpEntity,_) =>
      val strictEntityFuture = entity.toStrict(3 seconds)
      strictEntityFuture.flatMap { strictEntity =>
        val guitarJsonString = strictEntity.data.utf8String
        val guitar = guitarJsonString.parseJson.convertTo[Guitar]
        val guitarCreatedFuture = (guitarDB ? CreateGuitar(guitar)).mapTo
        guitarCreatedFuture.map { _ =>
          HttpResponse(StatusCodes.OK,entity = HttpEntity(ContentTypes.`application/json`, "{\"success\":true}" ))
        }
      }


    case request: HttpRequest =>
      request.discardEntityBytes()
      Future(HttpResponse(StatusCodes.NotFound))
  }
  Http().bind("localhost",8000).runWith(Sink.foreach[IncomingConnection]{
    connection => connection.handleWithAsyncHandler(requestHandler)
  })
}
