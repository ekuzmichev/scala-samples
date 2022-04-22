package ru.ekuzmichev

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object TypedStatelessActors extends App {
  trait SimpleThing
  case object EatChocolate extends SimpleThing
  case object WashDishes   extends SimpleThing
  case object LearnAkka    extends SimpleThing

  // Behaviors.setup - is necessary to initialize the mutable state
  val emotionalMutableActor: Behavior[SimpleThing] = Behaviors.setup { context =>
    // spin up an actor state
    // before creating behavior we
    // can create here some sort of a mutable state
    var happiness = 0

    // behavior of the actor
    Behaviors.receiveMessage {
      case EatChocolate =>
        context.log.info(s"[Stateful] ($happiness). Eating chocolate")
        happiness += 1
        Behaviors.same
      case LearnAkka    =>
        context.log.info(s"[Stateful] ($happiness). Learning Akka")
        happiness += 10
        Behaviors.same
      case WashDishes   =>
        context.log.info(s"[Stateful] ($happiness). Washing diches")
        happiness -= 2
        Behaviors.same
      case _            =>
        context.log.info("[Stateful] Receives smth I don't know")
        Behaviors.same
    }
  }

  // Alternative functional way of creating actors with state and avoid vars
  def emotionalFunctionalActor(happiness: Int = 0): Behavior[SimpleThing] = Behaviors.receive { (context, msg) =>
    msg match {
      case EatChocolate =>
        context.log.info(s"[Stateless] ($happiness). Eating chocolate")
        emotionalFunctionalActor(happiness + 1) // new behavior // returns immediately, so it's not true recursive call
      case LearnAkka    =>
        context.log.info(s"[Stateless] ($happiness). Learning Akka")
        emotionalFunctionalActor(happiness + 10)
      case WashDishes   =>
        context.log.info(s"[Stateless] ($happiness). Washing diches")
        emotionalFunctionalActor(happiness - 2)
      case _            =>
        context.log.info("[Stateless] Receives smth I don't know")
        Behaviors.same
    }
  }

  def testStatefulActor(): Unit = {
    val emotionalActorSystem = ActorSystem(emotionalMutableActor, "EmotionalSystem")

    emotionalActorSystem ! EatChocolate
    emotionalActorSystem ! EatChocolate
    emotionalActorSystem ! EatChocolate
    emotionalActorSystem ! EatChocolate
    emotionalActorSystem ! WashDishes
    emotionalActorSystem ! LearnAkka

    Thread.sleep(1000)
    emotionalActorSystem.terminate()
  }

  def testStatelessActor(): Unit = {
    val emotionalActorSystem = ActorSystem(emotionalFunctionalActor(), "EmotionalSystem")

    emotionalActorSystem ! EatChocolate
    emotionalActorSystem ! EatChocolate
    emotionalActorSystem ! EatChocolate
    emotionalActorSystem ! EatChocolate
    emotionalActorSystem ! WashDishes
    emotionalActorSystem ! LearnAkka

    Thread.sleep(1000)
    emotionalActorSystem.terminate()
  }

  testStatefulActor()
//  testStatelessActor()
}
