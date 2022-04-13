package ru.ekuzmichev

import scala.concurrent.Future
import scala.util.Try

object CallByNameTricks extends App {
  def byValueFn(x: Int): Int = 43
  byValueFn(2 + 3) // 2 + 3 is evaluated first

  def byNameFn(x: => Int): Int = 43
  byNameFn(2 + 3) // 2 + 3 is not evaluated, passed literally

  // trick #1 - reevaluation
  def byValuePrint(x: Long): Unit = {
    println(x)
    println(x)
  }

  def byNamePrint(x: => Long): Unit = {
    println(x)
    println(x)
  }

  println("===NANO TIME by value===")
  byValuePrint(System.nanoTime())
  println("===NANO TIME by name===")
  byNamePrint(System.nanoTime())
  println()

  // trick #2 - call by need (e.g. infinite collections: LazyList)
  abstract class MyList[+T] {
    def head: T
    def tail: MyList[T]
  }
  class NonEmptyList[+T](h: => T, t: => MyList[T]) extends MyList[T] {
    override lazy val head: T         = h // override with lazy val
    override lazy val tail: MyList[T] = t // override with lazy val
  }

  // trick #3 - hold the door
  // Construction of Try is possible because argument does not need to be evaluated first (due to call-by-name nature)
  val anAttempt: Try[Int] = Try { // part of the language
    throw new NullPointerException
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  val f: Future[Int] = Future {
    // hard computation for another thread is passed by name too
    // it is not evaluated before Future construction
    43
  }
}
