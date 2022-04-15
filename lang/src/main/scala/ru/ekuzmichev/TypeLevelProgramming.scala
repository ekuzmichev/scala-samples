package ru.ekuzmichev

import ru.ekuzmichev.TypeLevelProgramming.{+, four, two, zero}

object TypeLevelProgramming extends App {

  // Part 1

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

  // Part 2

  // ADD NUMBERS as types
  trait +[A <: Nat, B <: Nat, S <: Nat] // S - sum between A and B
  // If compiler could create implicit instance of + for A,B,S
  // this means that statement A + B = S is true
  object + {
    // Aksiom 1: We have as truth: 0 + 0 = 0
    implicit val zero: +[_0, _0, _0] = new +[_0, _0, _0] {}
    // Aksiom 2: For any number A <: Nat such that A > 0 => A + 0 and 0 + A = A
    implicit def basicRight[A <: Nat](implicit lt: _0 < A): +[_0, A, A] = new +[_0, A, A] {}
    implicit def basicLeft[A <: Nat](implicit lt: _0 < A): +[A, _0, A]  = new +[A, _0, A] {}

    // Inductive reasoning
    // If A + B = S then Succ[A] + Succ[B] = Succ[Succ[S]] // "S + 2"
    implicit def inductive[A <: Nat, B <: Nat, S <: Nat](implicit plus: +[A, B, S]): +[Succ[A], Succ[B], Succ[Succ[S]]] =
      new +[Succ[A], Succ[B], Succ[Succ[S]]] {}

    def apply[A <: Nat, B <: Nat, S <: Nat](implicit plus: +[A, B, S]): +[A, B, S] = plus
  }

  val zero: +[_0, _0, _0] = +.apply
  val two: +[_0, _2, _2] = +.apply
  val four: +[_1, _3, _4] = +.apply
  /*
    Compiler works as:
    - I need an implicit +[1, 3, 4] from apply method
    - +[1, 3, 4] == +[Succ[0], Succ[2], Succ[Succ[2]]]
    - +[Succ[0], Succ[2], Succ[Succ[2]]] matches the pattern of implicit inductive method
    - I need an implicit +[0, 2, 2]
    - +[0, 2, 2] corresponds to basicRight method
    - I need an implicit <[0, 2]
    - <[0, 2] corresponds to implicit ltBasic
    - done. can construct everything
   */

//  val invalidFour: +[_2, _3, _4] = +.apply // does not compile

  println(show(zero))
  println(show(two))
  println(show(four))

  // How to infer types automatically
  // Let's change type signatures a little
  object infer {
    // We moving Sum type argument to abstract type member
    trait +[A <: Nat, B <: Nat] { type Result <: Nat }
    object + {
      type Plus[A <: Nat, B <: Nat, S <: Nat] = +[A, B] {type Result = S}
      implicit val zero: Plus[_0, _0, _0] = new +[_0, _0] {type Result = _0}
      implicit def basicRight[A <: Nat](implicit lt: _0 < A): Plus[_0, A, A] = new +[_0, A] {type Result = A}
      implicit def basicLeft[A <: Nat](implicit lt: _0 < A): Plus[A, _0, A]  = new +[A, _0] {type Result = A}
      implicit def inductive[A <: Nat, B <: Nat, S <: Nat](implicit plus: Plus[A, B, S]): Plus[Succ[A], Succ[B], Succ[Succ[S]]] =
        new +[Succ[A], Succ[B]] {type Result = Succ[Succ[S]]}
      // We need to force compiler to show Result type to us: Using Plus as return type with reference to type member
      def apply[A <: Nat, B <: Nat](implicit plus: +[A, B]): Plus[A, B, plus.Result] = plus
    }

    val zero: +[_0, _0] = +.apply
    val two: +[_0, _2] = +.apply
    val four: +[_1, _3] = +.apply

    def runInferExample(): Unit = {
      println(show(zero))
      println(show(two))
      // here is type is "fixed" at the left
      println(show(four)) // TypeTag[_1 + _3]

      // here is "automatic" inference
      println(show(+.apply[_1, _3])) // TypeTag[Succ[_0] + Succ[Succ[Succ[_0]]]{type Result = Succ[Succ[Succ[Succ[_0]]]]}]
    }
  }

  infer.runInferExample()

}
