package example

import akka.actor.ActorSystem

import scala.util.{Failure, Success}
import akka.http.scaladsl.Http.IncomingConnection
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
object AkkaHTTPLowLevel extends App {
  implicit  val system = ActorSystem("LowLevelServerAPI")
  implicit  val materializer = ActorMaterializer()
  import system.dispatcher

  val serverSource = Http().bind("localhost", 8000)
  val connectionSink = Sink.foreach[IncomingConnection] {connection =>
    println(s"Accepted incoming connection from ${connection.remoteAddress}")
  }

  val serverBindingFuture = serverSource.to(connectionSink).run()
  serverBindingFuture.onComplete {
    case Success(binding) =>
      println("Server binding successful")
      binding.terminate(2 seconds)
    case Failure(exception) => println(s"Server binding failed: $exception")
  }

  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, _ ,_,_,_) =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |<body>
            |Status OK!
            |</body>
            |</html>
            |""".stripMargin
    ))
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(StatusCodes.NotFound, entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
        """
          |<html>
          |<body>
          |404 Not found
          |</body>
          |</html>
          |""".stripMargin))
  }
  var httpSyncConnectionHandler = Sink.foreach[IncomingConnection](connection =>
    connection.handleWithSyncHandler(requestHandler)
  )
  Http().bind("localhost",8080).runWith(httpSyncConnectionHandler)

  val asyncRequestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/ping"), _, _, _) =>
      Future(HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |<body>
            |Status OK!
            |</body>
            |</html>
            |""".stripMargin
        )))
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), _, _, _) =>
      Future(HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |<body>
            |Hello from Akka HTTP!
            |</body>
            |</html>
            |""".stripMargin
        )))
    case request: HttpRequest =>
      request.discardEntityBytes()
      Future(HttpResponse(StatusCodes.NotFound, entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
        """
          |<html>
          |<body>
          |404 Not found
          |</body>
          |</html>
          |""".stripMargin)))
  }
  val httpAsyncConnectionHandler = Sink.foreach[IncomingConnection]{ connection =>
      connection.handleWithAsyncHandler(asyncRequestHandler)
  }
  Http().bind("localhost",8081).runWith(httpAsyncConnectionHandler)

  // Async via Akka Stream
  val streamBasedRequestHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), _, _, _) =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |<body>
            |Hello from Akka HTTP!
            |</body>
            |</html>
            |""".stripMargin
        ))
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(StatusCodes.NotFound, entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
        """
          |<html>
          |<body>
          |404 Not found
          |</body>
          |</html>
          |""".stripMargin))
  }
  Http().bind("localhost",8082).runForeach( connection => connection.handleWith(streamBasedRequestHandler))
}
