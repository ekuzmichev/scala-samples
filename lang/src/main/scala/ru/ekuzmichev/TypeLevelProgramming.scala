package ru.ekuzmichev

object TypeLevelProgramming extends App {
  import scala.reflect.runtime.universe._

  // prints type of a value
  def show[T](value: T)(implicit tag: TypeTag[T]) =
    tag.toString().replace("ru.ekuzmichev.TypeLevelProgramming.", "")

  println(show(1))             // TypeTag[Int]
  println(show(List(1, 2, 3))) // TypeTag[List[Int]]

  // type-level programming

  // we program by enforcing some type constraints
  // we can represent natural values as types not as values
  // ===Peano arithmetic===
  // This is Peano representation of natural numbers in terms of number zero and
  // successor relationships between numbers
  trait Nat // Natural numbers
  class _0             extends Nat
  class Succ[N <: Nat] extends Nat // Successor

  type _1 = Succ[_0]
  type _2 = Succ[_1] // Succ[Succ[_0]]
  type _3 = Succ[_2] // Succ[Succ[Succ[_0]]]
  type _4 = Succ[_3] // Succ[Succ[Succ[Succ[_0]]]]
  type _5 = Succ[_4] // Succ[Succ[Succ[Succ[Succ[_0]]]]]

//  type Animal = Succ[Int] // does not compile due to Int is not subtype of Nat

  // we want _2 < _4 relationship. how?
  trait <[A <: Nat, B <: Nat]

  object < {
    // for every B which is Natural a compiler can automatically build on demand
    // an instance of < for _0 and successor of B
    // _0 is < than Successor of any number (which is at least _1)
    implicit def ltBasic[B <: Nat]: _0 < Succ[B]                                          = new <[_0, Succ[B]] {}
    implicit def inductive[A <: Nat, B <: Nat](implicit lt: <[A, B]): <[Succ[A], Succ[B]] = new <[Succ[A], Succ[B]] {}
    def apply[A <: Nat, B <: Nat](implicit lt: A < B): A < B                              = lt
  }

  val comparison0_1: _0 < _1 = <[_0, _1]
  val comparison1_3: _1 < _3 = <[_1, _3]
  /*
  HOW Compiler do it:
  <.apply[_1, _3] -> requires implicit <[_1, _3]
  inductive[_1, _3] -> requires implicit <[_0, _2]
  ltBasic[_1] -> produces implicit <[_0, Succ[_1]] == <[_0, _2]
   */

  println(show(comparison0_1)) // TypeTag[_0 < _1]
  println(show(comparison1_3)) // TypeTag[_1 < _3]

  // if the type TypeTag[_1 < _3] exists by the compiler, relationship between 1 and 3 is validated
  // if code does not compile, relationship is not true:
  // val invalidComparison: _3 < _2 = <[_3, _2] // does not compile

  trait <=[A <: Nat, B <: Nat]

  object <= {
    implicit def lteBasic[B <: Nat]: _0 <= B = new <=[_0, B] {}
    implicit def inductive[A <: Nat, B <: Nat](implicit lte: <=[A, B]): <=[Succ[A], Succ[B]] =
      new <=[Succ[A], Succ[B]] {}
    def apply[A <: Nat, B <: Nat](implicit lte: A <= B): A <= B = lte
  }

  val lteComparison: _1 <= _1 = <=[_1, _1]
  println(show(lteComparison))

//  val invalidLteComparison: _3 <= _2 = <=[_3, _2] // does not compile
}
