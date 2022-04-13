package ru.ekuzmichev

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorSystem, Behavior }

object AkkaTyped extends App {

  // 1 typed messages & actors
  sealed trait ShoppingCartMessage
  case class AddItem(item: String)    extends ShoppingCartMessage
  case class RemoveItem(item: String) extends ShoppingCartMessage
  case object ValidateCart            extends ShoppingCartMessage

  val shoppingRootActor = ActorSystem( // this is top-level guardian actor in the whole actor system
    Behaviors.receiveMessage[ShoppingCartMessage] { message =>
      message match {
        case AddItem(item)    => println(s"Adding $item to cart")
        case RemoveItem(item) => println(s"Removing $item from cart")
        case ValidateCart     => println(s"Validating cart")
      }
      Behaviors.same
    },
    "SimpleShoppingActor"
  )

//  shoppingRootActor ! "Hello actor" // does not compile
  shoppingRootActor ! ValidateCart

  // 2 mutable state

  val shoppingRootActorMutable = ActorSystem(
    Behaviors.setup[ShoppingCartMessage] { ctx =>
      var items: Set[String] = Set()

      Behaviors.receiveMessage[ShoppingCartMessage] { message =>
        message match {
          case AddItem(item) =>
            println(s"Adding $item to cart")
            items += item
          case RemoveItem(item) =>
            println(s"Removing $item from cart")
            items -= item
          case ValidateCart =>
            println(s"Validating cart")
        }
        Behaviors.same
      }
    },
    "SimpleShoppingActor"
  )

  def shoppingBehavior(items: Set[String]): Behavior[ShoppingCartMessage] =
    Behaviors.receiveMessage[ShoppingCartMessage] {
      case AddItem(item) =>
        println(s"Adding $item to cart")
        shoppingBehavior(items + item)
      case RemoveItem(item) =>
        println(s"Removing $item from cart")
        shoppingBehavior(items - item)
      case ValidateCart =>
        println(s"Validating cart")
        Behaviors.same
    }

  // 3 - hierarchy
  // you can only spond child actors from given actor, not from actor system
  val rootOnlineStoreActor = ActorSystem(
    Behaviors.setup[ShoppingCartMessage] { ctx =>
      // create children here
      ctx.spawn(shoppingBehavior(Set()), "myShoppingCart")
      Behaviors.empty
    },
    "OnlineStore"
  )
}
