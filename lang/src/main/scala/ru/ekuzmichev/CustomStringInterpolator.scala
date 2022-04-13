package ru.ekuzmichev

object CustomStringInterpolator extends App {
  val pi = 3.141828

  // s-interpolator
  val sInterpolator = s"The value is ${pi / 2}\n"
  println(sInterpolator)

  // raw-interpolator
  val rawInterpolator = raw"The value is ${pi / 2}\n" // escape characters are not escaped
  println(rawInterpolator)

  // f-interpolator
  val fInterpolator = f"The value is ${pi / 2}%4.2f\n"
  println(fInterpolator)

  // a custom interpolator
  case class Person(name: String, age: Int)
  // we want to parse CSV-like "name,age" to Person

  // "normal" approach
  def fromStringToPerson(s: String): Person = {
    val tokens = s.split(",")
    Person(tokens(0), tokens(1).toInt)
  }

  // interpolator approach
  implicit class PersonInterpolator(sc: StringContext) {
    // args - expressions that passed to interpolated string
    // strings between args are called parts and available from StringContext
    // Any* - because args are of any type
    def person(args: Any*): Person = {
      val parts = sc.parts
      // s method concatenates all parts and all args in right order into total expanded string
      val totalString = sc.s(args: _*)
      val tokens      = totalString.split(",")
      Person(tokens(0), tokens(1).toInt)
    }
  }

  val bob   = person"Bob,34"
  val name  = "Claus"
  val age   = 21
  val claus = person"$name,$age"

  println(bob)
  println(claus)
}
