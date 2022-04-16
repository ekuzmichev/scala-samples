package ru.ekuzmichev

object UnderscoresEverywhere extends App {
  // ==================================== //
  // #1 - ignoring stuff
  // ==================================== //

  // values
  val _ = 5

  // function arguments
  val onlyFives = (1 to 10).map(_ => 5)

  // self type annotations
  trait Singer
  trait Actor { _: Singer => } // if we don't need pointer to Singer
  class Bob extends Actor with Singer

  // generics
  def processList(list: List[Option[_]]): Int = list.length

  // ==================================== //
  // #2 - "everything" = wildcard
  // ==================================== //

  // pattern matching
  val meaningOfLife: Any = 42
  meaningOfLife match {
    case _ => "I'm fine with anything"
  }

  // imports
  import scala.concurrent.duration._

  // ==================================== //
  // #3 - default initializers
  // ==================================== //

  // variables default initializer
  var myString: String = _ // 0 for numeric, false for boolean, null for reference types

  // ==================================== //
  // #4 - lambda sugars
  // ==================================== //

  List(1, 2, 3).map(_ * 5)

  val sumFunction: (Int, Int) => Int = _ + _ // each new underscore takes placeholder for the next argument

  // ==================================== //
  // #5 - eta expansion
  // ==================================== //

  def m(x: Int): Int = x + 1
  val f1             = m _
  val f2: Int => Int = m

  // ==================================== //
  // #6 - higher kinded types (HKT)
  // ==================================== //

  class MyHKT[M[_]] // M is itself generic
  val instanceHkt = new MyHKT[List]

  // ==================================== //
  // #7 - variable args methods
  // ==================================== //

  def makeSentence(words: String*): String = words.mkString(" ")
  makeSentence("I", "love", "Scala")
  val words    = List("I", "love", "Scala")
  val sentence = makeSentence(words: _*) // auto-expands collection to var args
  println(sentence)
}
