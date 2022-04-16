package ru.ekuzmichev

import eu.timepit.refined.numeric.Interval.OpenClosed

object RefinedTypes extends App {

  // PROBLEM:
  case class User(name: String, email: String)
  val daniel = User("daniel", "daniel@gmail.com")
  // but we also can:
  val invalidDaniel = User("daniel@gmail.com", "daniel") // compilable but wrong at runtime
  // Also can be confusing case class User(v1: String, v2: String)

  /*
    Possible workarounds:
    - validate logic at creation time
    - use value types
    - create smart constructors

    But it does not prevent app to crash at runtime
   */

  // Refined types

  // Example: only positive nums

  import eu.timepit.refined.api.Refined
  import eu.timepit.refined.auto._    // implicit conversions
  import eu.timepit.refined.numeric._ // predicates for numeric types

  val aPositiveInt: Refined[Int, Positive] = 42 // macros + type-level converions
  // Positive - predicate type for type level constraint

  // val invalidPositiveInt: Refined[Int, Positive] = -42 // does not compile

  val onlyNegative: Refined[Int, Negative] = -23
  val evenNumber: Refined[Int, Even]       = 12 // Refined[Int, Even] = Int Refined Even

  import eu.timepit.refined.W // shapeless.Witness - macros magic for runtime types creation

  val smallEnough: Int Refined Less[W.`100`.T]                  = 99
  val zeroToHundred: Int Refined OpenClosed[W.`0`.T, W.`100`.T] = 21

  val divisibleByFive: Int Refined Divisible[W.`5`.T] = 15

  import eu.timepit.refined.string._

  val commandPrompt: String Refined EndsWith[W.`"$"`.T] = "daniel@mbp $"
  // tests if string is a proper regular expression
  val isRegex: String Refined Regex = "rege(x(es)?)"

  type Email = String Refined MatchesRegex[W.`"""[a-z0-9]+@[a-z0-9]+\\.[a-z0-9]{2,}"""`.T]
  val email: Email = "daniel@gmail.com"

  type Name = String Refined MatchesRegex[W.`"""[A-Z][a-z]+"""`.T] // starts with capital letter
  val name: Name = "Daniel"

  case class RefinedUser(name: Name, email: Email)
  val danielRefined = RefinedUser("Daniel", "daniel@gmail.com")
  // RefinedUser("daniel@gmail.com", "Daniel") does not compile

  // How to refine at runtime
  import eu.timepit.refined.api.RefType
  val poorEmail   = "daniel.com"
  val refineCheck = RefType.applyRef[Email].apply(poorEmail)
  println(refineCheck) // Left(Predicate failed: "daniel.com".matches("[a-z0-9]+@[a-z0-9]+\.[a-z0-9]{2,}").)
}
