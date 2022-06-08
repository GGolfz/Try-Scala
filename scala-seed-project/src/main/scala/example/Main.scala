package example
import akka.actor.{Actor, ActorSystem, Props}

object Main extends App {
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message: String => println(s"I receive: $message")
    }
  }
  val system = ActorSystem("AkkaTrial")
  val actor = system.actorOf(Props[SimpleActor], "SimpleActor")
  actor ! "Hello world"
}