package ru.ekuzmichev

import scala.annotation.tailrec

object HofsForOop extends App {
  class Applicable {
    def apply(x: Int): Int = x + 1
  }
  val applicable = new Applicable
  applicable.apply(1) // 2
  applicable(1)       // 2

  // apply allows to invoke objects like a functions

  // function objects
  val incrementer = new Function[Int, Int] {
    override def apply(i: Int): Int = i + 1
  }
  incrementer.apply(2) // 3
  incrementer(2)       // 3

  // syntax sugar
  val incrementerAlt = (i: Int) => i + 1
  incrementerAlt.apply(3) // 4
  incrementerAlt(3)       // 4

  // HOF's

  // Example
  def nTimes(f: Int => Int, n: Int): Int => Int = {
    @tailrec
    def acc(fAcc: Int => Int, nAcc: Int): Int => Int =
      if (nAcc <= 0) identity
      else if (nAcc == 1) fAcc
      else acc(f.andThen(fAcc), nAcc - 1)

    acc(f, n)
  }
  /*
    g = nTimes(f, 30)
    g(x) = f(f(f(...(f(x))))) 30 times
   */

  println("Res: " + nTimes(_ * 2, 15)(1))
}
