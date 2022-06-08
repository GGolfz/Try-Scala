package example
import akka.actor.SupervisorStrategy.{Restart, Stop, stop}
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, PoisonPill, Props, Stash, SupervisorStrategy}

object Main extends App {
  class SimpleActor extends Actor with ActorLogging with Stash {
    override def receive: Receive = {
      case "stashThis" => stash()
      case "changeHandlerNow" =>
        unstashAll()
        context.become(anotherHandler)
      case "changeHandler" =>
        context.become(anotherHandler)
      case message: String => println(s"I receive: $message")
    }
    def anotherHandler: Receive = {
      case message => println(s"I receive: $message in another handler")
    }

    override def preStart(): Unit = {
      log.info("I'm start")
    }

    override def postStop(): Unit = {
      log.error("I'm stopped")
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: RuntimeException => Restart
      case _ => Stop
    }
  }
  val system = ActorSystem("AkkaTrial")
  val actor = system.actorOf(Props[SimpleActor], "SimpleActor")
  actor ! "Hello world"
  actor ! "stashThis"
  actor ! "changeHandler"
  actor ! PoisonPill
  actor ! "Hi!"
}