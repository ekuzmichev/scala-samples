package ru.ekuzmichev

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AkkaTypedPipePattern extends App {
  // handling results of async computations
  // actor encapsulation: state of the actor is not accessed from outside
  // but inside actor's logic an async call may be initiated
  // this breaks actor encapsulation => handling async response happens on some thread,
  // which is different from the main actor thread

  object Infrastructure {
    // for running futures
    private implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(5))

    // name -> phone number
    private val db: Map[String, Int] = Map(
      "bob"   -> 123,
      "alice" -> 456,
      "duke"  -> 789
    )

    // kind of infrastructure external service
    def asyncRetrievePhoneNumber(name: String): Future[Int] = Future(db(name))
  }

  // race problem version

  object testV1 {
    trait PhoneCallProtocol
    case class FindAndCallPhoneNumber(name: String) extends PhoneCallProtocol

    val phoneCallInitiatorV1: Behavior[PhoneCallProtocol] = Behaviors.setup { context =>
      var nPhoneCalls = 0
      var nFailures   = 0

      implicit val ec: ExecutionContext = context.executionContext

      Behaviors.receiveMessage { case FindAndCallPhoneNumber(name) =>
        val futureNumber: Future[Int] = Infrastructure.asyncRetrievePhoneNumber(name)
        futureNumber.onComplete { // happens on another thread
          case Success(number)    =>
            // perform phone call
            context.log.info(s"Initiating phone call to $number")
            nPhoneCalls += 1 // potential RACE CONDITION due to this is mutated by multiple threads
          case Failure(exception) =>
            context.log.error(s"Failed to perform call for $name: $exception")
            nFailures += 1 // potential RACE CONDITION too
        }
        Behaviors.same
      }
    }
  }

  // pipe pattern version
  // can we forward async result back to us to continue handling on the same thread?
  // YES: as a message (and message processing is atomic)

  object testV2 {
    trait PhoneCallProtocol
    case class FindAndCallPhoneNumber(name: String) extends PhoneCallProtocol
    case class InitiatePhoneCall(number: Int)       extends PhoneCallProtocol
    case class PhoneCallFailure(reason: Throwable)  extends PhoneCallProtocol

    val phoneCallInitiatorV2: Behavior[PhoneCallProtocol] = Behaviors.setup { context =>
      var nPhoneCalls = 0
      var nFailures   = 0

      implicit val ec: ExecutionContext = context.executionContext

      Behaviors.receiveMessage {
        case FindAndCallPhoneNumber(name) =>
          val futureNumber: Future[Int] = Infrastructure.asyncRetrievePhoneNumber(name)
          context.pipeToSelf(futureNumber) {
            case Success(number)    => InitiatePhoneCall(number)
            case Failure(exception) => PhoneCallFailure(exception)
          }
          Behaviors.same
        case InitiatePhoneCall(number)    =>
          context.log.info(s"Initiating phone call to $number")
          nPhoneCalls += 1
          Behaviors.same
        case PhoneCallFailure(reason)     =>
          context.log.error(s"Failed to perform call: $reason")
          nFailures += 1
          Behaviors.same
      }
    }
  }

  // stateless version

  object testV3 {
    trait PhoneCallProtocol
    case class FindAndCallPhoneNumber(name: String) extends PhoneCallProtocol
    case class InitiatePhoneCall(number: Int)       extends PhoneCallProtocol
    case class PhoneCallFailure(reason: Throwable)  extends PhoneCallProtocol

    def phoneCallInitiatorV3(nPhoneCalls: Int = 0, nFailures: Int = 0): Behavior[PhoneCallProtocol] =
      Behaviors.receive { (context, message) =>
        implicit val ec: ExecutionContext = context.executionContext

        message match {
          case FindAndCallPhoneNumber(name) =>
            val futureNumber: Future[Int] = Infrastructure.asyncRetrievePhoneNumber(name)
            context.pipeToSelf(futureNumber) {
              case Success(number)    => InitiatePhoneCall(number)
              case Failure(exception) => PhoneCallFailure(exception)
            }
            Behaviors.same
          case InitiatePhoneCall(number)    =>
            context.log.info(s"Initiating phone call to $number")
            phoneCallInitiatorV3(nPhoneCalls + 1, nFailures)
          case PhoneCallFailure(reason)     =>
            context.log.error(s"Failed to perform call: $reason")
            phoneCallInitiatorV3(nPhoneCalls, nFailures + 1)
        }
      }

  }

//  import testV1._
  import testV2._
//  import testV3._

  val root = ActorSystem(
//    testV1.phoneCallInitiatorV1,
    testV2.phoneCallInitiatorV2,
//    testV3.phoneCallInitiatorV3(),
    "PhoneNumberCaller"
  )

  root ! FindAndCallPhoneNumber("alice")
  root ! FindAndCallPhoneNumber("somebody")

  Thread.sleep(1000)
  root.terminate()
}
