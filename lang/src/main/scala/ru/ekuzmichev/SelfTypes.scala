package ru.ekuzmichev

object SelfTypes extends App {
  trait Edible // съедобный

  // hierarchy #1
  trait Person {
    def hasAllergiesTo(thing: Edible): Boolean
  }
  trait Child extends Person
  trait Adult extends Person

  // hierarchy #2
  trait Diet {
    def eat(thing: Edible): Boolean
  }
  trait Carnivore  extends Diet // хищник
  trait Vegetarian extends Diet

  // PROBLEM: Diet must be applicable to Persons only
  // It should access Person methods directly while implementing its own methods/logic
  // class VegetarianAthlete extends Vegetarian with Adult // enforce at compile time

  // OPTION #1: Enforce a subtype relationship

  //  // hierarchy #1
  //  trait Person {
  //    def hasAllergiesTo(thing: Edible): Boolean
  //  }
  //  trait Child extends Person
  //  trait Adult extends Person
  //
  //  // hierarchy #2
  //  trait Diet extends Person {
  //    def eat(thing: Edible): Boolean =
  //      !this.hasAllergiesTo(thing) // have access to the logic in Person class because extends it
  //  }
  //  trait Carnivore  extends Diet
  //  trait Vegetarian extends Diet

  // OPTION #2: Add type argument

  //  // hierarchy #1
  //  trait Person {
  //    def hasAllergiesTo(thing: Edible): Boolean
  //  }
  //  trait Child extends Person
  //  trait Adult extends Person
  //
  //  // hierarchy #2
  //  trait Diet[T <: Person]  { // Need kind of constructor with t: T but this is in Scala 3 only
  //    def eat(thing: Edible): Boolean = ???
  //  }
  //  trait Carnivore[T <: Person]  extends Diet[T]
  //  trait Vegetarian[T <: Person] extends Diet[T]

  // OPTION #3: A self type

  //  // hierarchy #1
  //  trait Person {
  //    def hasAllergiesTo(thing: Edible): Boolean
  //  }
  //  trait Child extends Person
  //  trait Adult extends Person
  //
  //  // hierarchy #2
  //  trait Diet { self: Person => // self-type: Whoever extends Diet must also extend Person (mix-in). This is a marker to compiler
  //    def eat(thing: Edible): Boolean = self.hasAllergiesTo(thing)
  //  }
  //  trait Carnivore  extends Diet with Person // Satisfies new self-type constraint
  //  trait Vegetarian extends Diet with Person
  //
  //  class VegetarianAthlete extends Vegetarian with Adult {
  //    override def hasAllergiesTo(thing: Edible): Boolean = false
  //  }

  // WHAT is the difference between inheritance and self-type?
  trait Animal
  class Dog extends Animal // A Dog IS AN Animal (is-a relationship)

  trait Habit { self: Person => } // A Habit REQUIRES a Person
}
