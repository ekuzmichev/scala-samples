package ru.ekuzmichev

object TypeClasses extends App {
  // PROBLEM:
  // specialized implementations
  // def processMyList[T](list: List[T]): T = ???
  // How to sum up all the elements
  // for integers => sum is actual sum of ints
  // for strings => concat of all elements
  // for other types => ERROR

  // implicits
  trait Summable[T] {
    def sumElements(list: List[T]): T
  }

  // and 2 different implementations
  implicit object IntSummable extends Summable[Int] {
    override def sumElements(list: List[Int]): Int = list.sum
  }

  implicit object StringSummable extends Summable[String] {
    override def sumElements(list: List[String]): String = list.mkString
  }

  def processMyList[T](list: List[T])(implicit summable: Summable[T]): T = // ad-hoc polymorphism
    summable.sumElements(list)

  println(processMyList(List(1, 2, 3)))
  println(processMyList(List("a", "b", "c")))

}
