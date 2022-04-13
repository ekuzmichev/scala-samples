package ru.ekuzmichev

object ConciseAndExpressiveTechnique extends App {
  // trick 1 - the single abstract method pattern
  trait Action {
    def act(x: Int): Int
  }

  val myAction1: Action = new Action {
    override def act(x: Int): Int = x + 1
  }
  val myAction2: Action = (x: Int) => x + 1 // can be replaced with lambda
  val myAction3: Action = _ + 1

  // here one can pass zero-arg lambda
  new Thread(() => println("I'm running")).start()

  // trick 2 - right associative methods (right-most operator executes first)
  val prependedElement = 2 :: List(3, 4)

  val list1 = 1 :: 2 :: 3 :: List()
  // equivalent
  val list2 = 1 :: (2 :: (3 :: List()))
  // compiler rewrites the above code
  val list3 = List().::(3).::(2).::(1)

  // custom operators
  class MessageQueue[T] {
    // an enqueue method
    def -->:(value: T): MessageQueue[T] = new MessageQueue[T] // implementation is any
  }

  val queue = 3 -->: 2 -->: 1 -->: new MessageQueue[Int]

  // trick 3 - baked-in "setters"
  class MutableIntWrapper {
    private var internalValue = 0

    // getter
    def value: Int = internalValue
    // setter
    def value_=(newValue: Int): Unit = internalValue = newValue
  }

  val wrapper = new MutableIntWrapper
  wrapper.value = 43 // compiler rewrites as wrapper.value_=(43)
  println(wrapper.value)

  // trick 4 - multi-word members
  case class Person(name: String) {
    def `then said`(phrase: String): Unit =
      println(phrase)
  }

  val bob = Person("Bob")
  bob.`then said`("Hi!")
  bob `then said` "Hi again!"

  // real life example: Akka Http
  // ContentTypes.`application/json`

  // trick 5 - backticks for pattern matching
  val someValue = 42 // assume wee need to match on it
  val data: Any = 45

  val result1 = data match {
    case someValue => 21 // wrong - it matches every variable
  }

  println(result1)

  val result2 = data match {
    case x if x == someValue => 0
    case _                   => 22
  }

  println(result2)

  // elegant way
  val result3 = data match {
    case `someValue` => 0 // using backticks "match the exact val value"
    case _           => 23
  }

  println(result3)
}
