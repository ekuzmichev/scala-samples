package ru.ekuzmichev

object NothingType extends App {
  class MyPrecious // extends AnyRef
  val x: AnyRef = new MyPrecious

  // nothing
  def gimmeNumber(): Int          = throw new NoSuchElementException
  def gimmeString(): String       = throw new NoSuchElementException
  def gimmePrecious(): MyPrecious = throw new NoSuchElementException

  // throw returns Nothing type (no instances and values)
  // Nothing != Unit != Null != anything
  // Nothing is replacement for any other type
  // Nothing extends any type

  def gimmePrecious2: MyPrecious = null
  def gimmePrecious3: Null       = null
  // def gimmeNumber2: Int           = null // does not compile
  // def gimmeNumber3: Null = 1 // does not compile

  // Nothing is at the bottom of the whole hierarchy
  // Null is at the bottom of AnyRef hierarchy

  // use of Nothing
  def functionAboutNothing(a: Nothing): Int = 45

  // valid from compiler perspective but crashes due to argument evaluation
  functionAboutNothing(throw new NullPointerException)

  def functionReturningNothing(): Nothing = throw new RuntimeException

  // Useful in generics (especially COVARIANT)
  abstract class MyList[+T] // If Dog <: Animal => MyList[Dog] <: MyList[Animal]
  object EmptyList extends MyList[Nothing] // => All the empty lists equal due to covariance
  val listOfStrings: MyList[String] = EmptyList // Nothing <: String => EmptyList = MyList[Nothing] <: MyList[String]
  val listOfInts: MyList[Int]       = EmptyList // Nothing <: Int => EmptyList = MyList[Nothing] <: MyList[Int]

  // ???
  def someUnimplementedMethod(): String = ??? // throw new NotImplementedError
}
