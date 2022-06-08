package example

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object AkkaStream extends App {
  implicit val system = ActorSystem("ScalaActorStream")
  implicit val materializer = ActorMaterializer()
  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"Just received String: ${s}")
        sender() ! s"$s$s"
      case n: Int =>
        log.info(s"Just received Number: $n")
        sender() ! (2*n)
      case _ =>
    }
  }
  val simpleActor = system.actorOf(Props[SimpleActor],"simpleActor")
  val numbersSource = Source(1 to 10)
  implicit val timeout = Timeout(2 seconds)
  val actorBasedFlow = Flow[Int].ask[Int](parallelism = 4)(simpleActor)
  val actorPoweredSource = Source.actorRef[Int](bufferSize=10, overflowStrategy = OverflowStrategy.dropHead)
//  val materializedActorRef = actorPoweredSource.to(Sink.foreach[Int](number => println(s"Actor powered flow got number: ${number}"))).run()

//  materializedActorRef ! 10
//
//  materializedActorRef ! akka.actor.Status.Success("complete")

  case object StreamInit
  case object StreamAck
  case object StreamComplete
  case class StreamFail(ex: Throwable)
  class DestinationActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StreamInit =>
        log.info("Stream Initialized")
        sender() ! StreamAck
      case StreamComplete =>
        log.info("Straem Complete")
        context.stop(self)
      case StreamFail(ex) =>
        log.warning(s"Stream Failed: ${ex}")
      case message =>
        log.info(s"Message $message has come to its final resting point.")
        sender() ! StreamAck
    }
  }
  val destinationActor = system.actorOf(Props[DestinationActor],"destinationActor")

  val actorPoweredSink = Sink.actorRefWithAck[Int](
    destinationActor,
    onInitMessage = StreamInit,
    onCompleteMessage = StreamComplete,
    ackMessage = StreamAck,
    onFailureMessage = throwable => StreamFail(throwable)
  )

  Source(1 to 10).to(actorPoweredSink).run()
}
