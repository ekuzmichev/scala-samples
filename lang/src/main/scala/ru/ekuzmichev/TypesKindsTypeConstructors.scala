package ru.ekuzmichev

object TypesKindsTypeConstructors extends App {
  // types in Scala are organised into kinds
  // kind = type of types

  // Int - plain simple regular type
  // Int, String, Boolean, ... - level-0 types = kind. Can be attached to values
  val number: Int = 42

  case class Person(name: String, age: Int) // type Person is also level-0 type, can be attached to value
  val bob: Person = Person("Bob", 42)

  // generic types = level-1 types
  // can not attach to the values on its own
  class LinkedList[T] {
    // code // applicable to any type T
  }

  // val list: LinkedList = ??? // does not compile
  val list: LinkedList[Int] = ??? // need to pass concrete type

  // level-1 type can take type arguments as level-0 types

  // level-1 type LinkedList[T] + level-0 type Int = new level-0 type LinkedList[Int]
  // it is similar to function provided with value argument and giving new value
  // that is why these generic types are called type constructors

  // level-2 types = higher-kinded types
  class Functor[F[_]] // takes an argument that itself is generic

  val functorList = new Functor[List] // passing just List as a level-1 type
  // this is also type constructor: [F[_]] => Functor[F]

  class Meta[F[_[_]]] // level-3 type
  val metaFunctor = new Meta[Functor] // passing level-2 argument F

  // examples
  class HashMap[K, V] // level-1, takes 2 level-0 arguments
  val map = new HashMap[String, String]

  class ComposedFunctor[F[_], G[_]] // level-2, takes 2 level-1 arguments
  val func = new ComposedFunctor[List, Option]

  class Formatter[F[_], T] // level-2 ... (but bit more complicated)
  val fmt = new Formatter[Option, String]
}
