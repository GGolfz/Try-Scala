package example

import akka.actor.{ActorSystem, ClassicActorSystemProvider}
import akka.stream.{Materializer, SystemMaterializer}
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.collection.immutable.{Stream => ColStream}
import scala.concurrent.Future
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
        nameSource.via(filterFlow).via(takeNameFlow).to(foreachSink).run()




}