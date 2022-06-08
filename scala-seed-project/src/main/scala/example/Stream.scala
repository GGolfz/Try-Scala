package example

import akka.actor.{ActorSystem, ClassicActorSystemProvider}
import akka.stream.{Materializer, OverflowStrategy, SystemMaterializer}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.collection.immutable.{Stream => ColStream}
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}
object Stream extends App {

        implicit val system = ActorSystem("FirstPrinciples")
        implicit def matFromSytem(implicit provider: ClassicActorSystemProvider): Materializer = SystemMaterializer(provider.classicSystem).materializer
        val source = Source(1 to 10)
        val sink = Sink.foreach[Int](println)
        val graph = source.to(sink)
//        graph.run()
        val flow = Flow[Int].map(x=>x+1)
//        source.via(flow).to(sink).run()
//        val illegalSource = Source.single[String](null)
//        illegalSource.to(Sink.foreach(println)).run()
        // Kind of Sources
        val finiteSource = Source.single(1)
        val anotherFiniteSource = Source(List(1,2,3,4))
        val emptySource = Source.empty[Int]
        var infiniteSource = Source(ColStream.from(1))
        import scala.concurrent.ExecutionContext.Implicits.global
        val futureSource = Source.fromFuture(Future(42))
        // Sinks
        val theMostBoringSink = Sink.ignore
        val foreachSink = Sink.foreach[String](println)
        val headSink = Sink.head[Int]
        val foldSInk = Sink.fold[Int,Int](0)((a,b) => a + b)

        // flows
        val mapFlow = Flow[Int].map(x => 2*x)
        val takeFlow = Flow[Int].take(5)
        // Source -> flow -> flow -> ... -> Sinks
        val doubleFlowGraph = source.via(mapFlow).via(takeFlow).to(sink)
//        doubleFlowGraph.run()

        val names = List("Alice","Bob","Charlie","David","Martin","AkkaStream")
        val nameSource = Source(names)
        val filterFlow = Flow[String].filter(x => x.length > 5)
        val takeNameFlow = Flow[String].take(2)
//        nameSource.vaia(filterFlow).via(takeNameFlow).to(foreachSink).run()

        // Materialize Stream

        val simpleSource = Source(1 to 5)
        val simpleFlow = Flow[Int].map(x=>x+1)
        val simpleSink = Sink.foreach[Int](println)
        val simpleGraph = simpleSource.viaMat(simpleFlow)(Keep.right).toMat(simpleSink)(Keep.right)
//        simpleGraph.run().onComplete {
//                case Success(_) => println("Success")
//                case Failure(exception) => println(s"Stream failed with: $exception")
//        }

        // Async Boundary

        val complexFlow1 = Flow[Int].map{ x =>
          Thread.sleep(1000)
          x + 1
        }
        val complexFlow2 = Flow[Int].map{ x =>
          Thread.sleep(1000)
          x*10
        }
//        simpleSource.via(complexFlow1).async.via(complexFlow2).async.to(simpleSink).run()

        // Action to Back Pressure

        val fastSource = Source(1 to 1000)
        val slowSink = Sink.foreach[Int]{ x=>
          Thread.sleep(1000)
          println(s"Sink: ${x}")
        }
        val newSimpleFlow = Flow[Int].map { x=>
          println(s"Incoming: ${x}")
          x + 1
        }
//        fastSource.async.via(newSimpleFlow).async.to(slowSink).run() // This cause back pressure
        val bufferFlow = newSimpleFlow.buffer(10, OverflowStrategy.dropHead)
//        fastSource.async.via(bufferFlow).async.to(slowSink).run() // Fix back pressure but it drops some buffer

        // Throttling
        import scala.concurrent.duration._
        fastSource.throttle(10,1 second).runWith(Sink.foreach(println))

}