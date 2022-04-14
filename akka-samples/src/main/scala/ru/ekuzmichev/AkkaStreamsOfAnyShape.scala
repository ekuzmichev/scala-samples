package ru.ekuzmichev

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip }
import akka.stream.{ ClosedShape, FlowShape, SinkShape, SourceShape }

object AkkaStreamsOfAnyShape extends App {
  implicit val system: ActorSystem = ActorSystem()

  // basics
  val source = Source(1 to 1000)
  val flow   = Flow[Int].map(_ * 2)
  val sink   = Sink.foreach[Int](println)

  val graph = source.via(flow).to(sink)

  graph.run()

  // graph dsl

  // source of ints -> 2 independent hard computations -> stitch results to tuple -> print to console

  // step 1 - the frame
  val specialGraph = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    // step 2.1 - create basic building blocks

    val input: SourceShape[Int]          = builder.add(Source(1 to 1000))
    val incrementer: FlowShape[Int, Int] = builder.add(Flow[Int].map(_ + 1))
    val multiplier: FlowShape[Int, Int]  = builder.add(Flow[Int].map(_ * 2))
    val output: SinkShape[(Int, Int)]    = builder.add(Sink.foreach[(Int, Int)](println))

    // step 2.1 - create non-basic building blocks
    val broadcast = builder.add(Broadcast[Int](2))
    val zip       = builder.add(Zip[Int, Int]())

    // step 3 - glue the components together
    input ~> broadcast
    broadcast.out(0) ~> incrementer ~> zip.in0
    broadcast.out(1) ~> multiplier ~> zip.in1
    zip.out ~> output

    // step 4 - closing
    ClosedShape
  }

  RunnableGraph.fromGraph(specialGraph).run()
}
