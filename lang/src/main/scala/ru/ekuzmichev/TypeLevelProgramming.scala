package ru.ekuzmichev

object TypeLevelProgramming extends App {

  // ================================================================================================================ //
  // PART 1
  // ================================================================================================================ //

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

  // val invalidLteComparison: _3 <= _2 = <=[_3, _2] // does not compile

  // ================================================================================================================ //
  // PART 2
  // ================================================================================================================ //

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

  // ================================================================================================================ //
  // PART 3
  // ================================================================================================================ //

  // Sort instances at compile time (Merge-sort algorithm)
  // Heterogeneous list
  trait HList
  class HNil extends HList
  class ::[H <: Nat, T <: HList] extends HList

  /*
    Merge-sort algorithm
    - split the list in half
    - sort the halves
    - merge them back
   */

  trait Split[HL <: HList, L <: HList, R <: HList]
  object Split {
    // Axiom 1: Empty list is split into empty and empty lists
    implicit val basic: Split[HNil, HNil, HNil] = new Split[HNil, HNil, HNil] {}
    // Axiom 2: Single-element list is split into the same list on left and empty list on right
    implicit def basic2[N <: Nat]: Split[N :: HNil, N :: HNil, HNil] = new Split[N :: HNil, N :: HNil, HNil] {}
    // T - tail is split into L and R
    implicit def inductive[N1 <: Nat, N2 <: Nat, T <: HList, L <: HList, R <: HList]
      (implicit split: Split[T, L, R]): Split[N1 :: N2 :: T, N1 :: L, N2 :: R] =
      new Split[N1 :: N2 :: T, N1 :: L, N2 :: R] {}
    def apply[HL <: HList, L <: HList, R <: HList](implicit split: Split[HL, L, R]): Split[HL, L, R] = split
  }

  val validSplit: Split[_1 :: _2 :: _3 :: HNil, _1 :: _3 :: HNil, _2 :: HNil] = Split.apply
  /*
    Compiler works as:
    - require implicit Split[_1 :: _2 :: _3 :: HNil, _1 :: _3 :: HNil, _2 :: HNil]
    - call inductive[_1, _2, _3 :: HNil, _3 :: HNil, HNil]
    - require implicit Split[_3 :: HNil, _3 :: HNil, HNil]
    - call basic2[_3] => result is Split[_3 :: HNil, _3 :: HNil, HNil]
    - everything is built
   */
  println(show(validSplit))

  // left LA and right LB are merged to result list L
  trait Merge[LA <: HList, LB <: HList, L <: HList]
  object Merge {
    // Axioms 1: Merge with empty list gives initial list
    implicit def basicLeft[L <: HList]: Merge[HNil, L, L] = new Merge[HNil, L, L] {}
    implicit def basicRight[L <: HList]: Merge[L, HNil, L] = new Merge[L, HNil, L] {}

    /*
      L1 = N1 :: T1
      L2 = N2 :: T2
      if N1 < N2 => N1 :: {...}
      if N2 <= N1 => N2 :: {...}
     */
    // IR - intermediate result
    implicit def inductiveLte[N1 <: Nat, T1 <: HList, N2 <: Nat, T2 <: HList, IR  <: HList]
      (implicit merge: Merge[T1, N2 :: T2, IR], lte: <=[N1, N2]): Merge[N1 :: T1, N2 :: T2, N1 :: IR] =
      new Merge[N1 :: T1, N2 :: T2, N1 :: IR] {}

    implicit def inductiveGt[N1 <: Nat, T1 <: HList, N2 <: Nat, T2 <: HList, IR  <: HList]
      (implicit merge: Merge[N1 :: T1, T2, IR], lte: <[N2, N1]): Merge[N1 :: T1, N2 :: T2, N2 :: IR] =
      new Merge[N1 :: T1, N2 :: T2, N2 :: IR] {}

    def apply[LA <: HList, LB <: HList, L <: HList](implicit merge: Merge[LA, LB, L]): Merge[LA, LB, L] = merge
  }

  val validMerge: Merge[_1 :: _3 :: HNil, _2 :: _4 :: HNil, _1 :: _2 :: _3 :: _4 :: HNil] = Merge.apply
  /*
    Compiler work:
    - require Merge[_1 :: _3 :: HNil, _2 :: _4 :: HNil, _1 :: _2 :: _3 :: _4 :: HNil]
    - run inductiveLte
    - require implicit Merge[_3 :: HNil, _2 :: _4 :: HNil, _2 :: _3 :: _4 :: HNil],  <=[_1, _2]
    - run inductiveGt
    - require implicit Merge[_3 :: HNil, _4 :: HNil, _3 :: _4 :: HNil], <[_2, _3]
    - run inductiveLte
    - require implicit Merge[HNil, _4 :: HNil, _4 :: HNil], <=[_3, _4]
    - run basicLeft[_4 :: HNil]
   */
  println(show(validMerge))


  // O - output list
  trait Sort[L <: HList, O <: HList]
  object Sort {
    // Axiom 1: Empty list is sorted into empty list
    implicit val basicNil: Sort[HNil, HNil] = new Sort[HNil, HNil] {}
    // Axiom 2: List with 1 element is sorted into list of the same 1 element
    implicit def basicOne[N <: Nat]: Sort[N :: HNil, N :: HNil] = new Sort[N :: HNil, N :: HNil] {}
    // I - input list
    implicit def inductive[I <: HList, L <: HList, R <: HList, SL <: HList, SR <: HList, O <: HList]
      (
        implicit
        split: Split[I, L, R],
        sortLeft: Sort[L, SL],  // SL - sort left list
        sortRight: Sort[R, SR], // SR - sort right list
        merge: Merge[SL, SR, O]
      ): Sort[I, O]  = new Sort[I, O] {}
    def apply[L <: HList, O <: HList](implicit sort: Sort[L, O]): Sort[L, O] = sort
  }

  val validSort: Sort[_4 :: _3 :: _5 :: _1 :: _2 :: HNil, _1 :: _2 :: _3 :: _4 :: _5 :: HNil] = Sort.apply
  println(show(validSort))

  // making compiler return types automatically

  object finalInfer {
    trait Sort[L <: HList] {type Result <: HList}
    object Sort {
      type SortOp[L <: HList, O <: HList] = Sort[L] {type Result = O}
      implicit val basicNil: SortOp[HNil, HNil] = new Sort[HNil] {type Result = HNil}
      implicit def basicOne[N <: Nat]: SortOp[N :: HNil, N :: HNil] = new Sort[N :: HNil] {type Result = N :: HNil}
      implicit def inductive[I <: HList, L <: HList, R <: HList, SL <: HList, SR <: HList, O <: HList]
      (
        implicit
        split: Split[I, L, R],
        sortLeft: SortOp[L, SL],  // SL - sort left list
        sortRight: SortOp[R, SR], // SR - sort right list
        merge: Merge[SL, SR, O]
      ): SortOp[I, O]  = new Sort[I] {type Result = O}
      def apply[L <: HList](implicit sort: Sort[L]): SortOp[L, sort.Result] = sort // again the trick sort.Result
    }

    def runFinalInfer(): Unit ={
      println(show(Sort.apply[_4 :: _3 :: _5 :: _1 :: _2 :: HNil]))
    }
  }

  finalInfer.runFinalInfer()
}
