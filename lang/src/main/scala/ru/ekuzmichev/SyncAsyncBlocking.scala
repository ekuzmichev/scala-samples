package ru.ekuzmichev

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.{ Future, Promise }

object SyncAsyncBlocking extends App {
  // synchronous and blocking
  def blockingFn(x: Int): Int = {
    Thread.sleep(3000)
    x + 42
  }

  blockingFn(5) // blocking call
  val y = 42 // This waits 3 seconds before evaluating

  import scala.concurrent.ExecutionContext.Implicits.global

  // asynchronous and blocking
  def asyncBlockingFn(x: Int): Future[Int] = Future {
    Thread.sleep(3000)
    x + 42
  }

  asyncBlockingFn(5) // it is still blocking because that  other thread is idle
  val z = 42 // evaluates immediately

  // asynchronous and non-blocking
  def createSimpleActor() = Behaviors.receiveMessage[String] { message =>
    println(s"Received $message")
    Behaviors.same
  }

  val rootActor = ActorSystem(createSimpleActor(), "TestSystem")
  rootActor ! "My message" // enqueuing message, asynchronous and non-blocking

  // Making API returning meaningful values (not just fire and forget)
  val promiseResolver = ActorSystem(
    Behaviors.receiveMessage[(String, Promise[Int])] {
      case (message, promise) =>
        println(s"Received $message")
        promise.success(message.length)
        Behaviors.same
    },
    "PromiseResolver"
  )

  def doAsyncNonBlockingComputation(s: String): Future[Int] = {
    val promise = Promise[Int]()
    promiseResolver ! (s, promise)
    promise.future
  }

  val f = doAsyncNonBlockingComputation("my promised message") // async & non-blocking
  f.onComplete(println)

}
