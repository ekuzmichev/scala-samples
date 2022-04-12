package ru.ekuzmichev

import scala.concurrent.{ Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global

object ControllableFutures extends App {
  val aFuture = Future {
    42 // Is evaluated on some thread at some point of time
  }

  // Futures are inherently non-deterministic

  // given this multi-threaded service
  object MyService {
    // Assume the function is completely deterministic (the same result for same argument)
    def produceAPreciousValue(theArg: Int): String                = s"Result: ${theArg / 42}"
    def sumbitTask[A](actualArg: A)(function: A => Unit): Boolean =
      // Run the function on actualArg at some point
      true
  }

  // Introducing Promises

  // step 1
  val myPromise = Promise[String]()
  // step 2
  val myFuture = myPromise.future
  // step 3
  val furtherProcessing = myFuture.map(_.toUpperCase)
  // step 4
  def asyncCall(promise: Promise[String]): Unit =
    promise.success("Result")
  // step 5
  asyncCall(myPromise)

  // ======================================================= //
  // target method one wants to implement
  def giveMeMyPreciousValue(yourArg: Int): Future[String] = {
    // step 1 - create promise
    val thePromise = Promise[String]()

    // step 5 - submit task
    MyService.sumbitTask(yourArg) { x: Int =>
      // step 4 - producer logic
      val preciousValue = MyService.produceAPreciousValue(x)
      thePromise.success(preciousValue)
    }

    // step 2 - extract the future
    thePromise.future
  }

  // step 3 - someone will consume my Future
  val f = giveMeMyPreciousValue(1)
}
