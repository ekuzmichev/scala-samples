package ru.ekuzmichev

object EtaExpansionAndPartiallyAppliedFunctions extends App {
  // methods and functions are different

  // method is a member of enclosing class or object
  // only call on the instance class / object
  def incrementMethod(x: Int): Int = x + 1

  // call it as
  EtaExpansionAndPartiallyAppliedFunctions.incrementMethod(3)
  // if you call it in the body like
  incrementMethod(2)
  // this is in fact called from "this" instance
  this.incrementMethod(2)

  // a function - is a piece of code that can be called independently of a class or object
  // functions (lambdas) are assignable to vals and can be passed as arguments
  val incrementFunction = (x: Int) => x + 1
  val three             = incrementFunction(2) // actually here is this.incrementFunction(2)

  // but you can define an independent block where th function is not the member of anything
  {
    val incrementFunction = (x: Int) => x + 1
    val three             = incrementFunction(2)
  }

  // when you define function like val incrementFunction = (x: Int) => x + 1
  // you actually define it as
  val incrementFunctionExplicit = new Function[Int, Int] {
    override def apply(x: Int): Int = x + 1
  }

  // i.e. the function itself is a plain object and does not depend on enclosing class/object

  // ETA expansion allows conversion between methods and functions
  val incrementF = incrementMethod _ // _ - signal to the compiler "turn method into a function"
  // compiler makes in fact smth like
  val incrementFExplicit = (x: Int) => incrementMethod(x)

  // compiler can do eta-expansion automatically if you give an expansion type in advance
  val incrementF2: Int => Int = incrementMethod
  // auto eta-expansions takes place also when passing method as function parameter
  List(1, 2, 3).map(incrementMethod)
  List(1, 2, 3).map(incrementFunction)

  // Partially applied fns
  def multiArgAdder(x: Int)(y: Int): Int = x + y

  val add2 = multiArgAdder(2) _ // y => 2 + y. Here is also eta-expansion
  val four = add2(2)
  List(1, 2, 3).map(multiArgAdder(3)) // also automatic eta-expansion

  // interesting question #1
  def add(x: Int, y: Int): Int = x + y
  val addF                     = add _ // Whats here? this is (x, y) => x + y
  // _ turns into multi-arg function

  // interesting question #2
  def threeArgAdder(x: Int)(y: Int)(z: Int): Int = x + y + z

  val twoArgsRemaining = threeArgAdder(2) _ // Int => Int => Int // curried fn
  val ten              = twoArgsRemaining(3)(5)
  val oneArgRemaining  = threeArgAdder(2)(3) _ // Int => Int
  val ten2             = oneArgRemaining(5)

}
