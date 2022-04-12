package ru.ekuzmichev

object PatternMatchingTricks extends App {
  val numbers = List(1, 2, 3, 4, 5)

  // #1 Varargs match
  println {
    numbers match {
      case List(_ , 2, _*) => s"second is 2"
    }
  }

  // #2 Last element match
  println {
    numbers match {
      case _ :+ 5 => s"last is 5"
    }
  }
}
