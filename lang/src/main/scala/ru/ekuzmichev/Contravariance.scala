package ru.ekuzmichev

object Contravariance extends App {

  class Animal
  class Dog(name: String) extends Animal

  // If Dog <: Animal => List[Dog] <: List[Animal] ?
  // if YES: type is Covariant (List in the case)

  val lassie = new Dog("Lassie")
  val hutchy = new Dog("Hutchy")
  val laika  = new Dog("Laika")

  val anAnimal: Animal              = lassie                      // a dog
  val myDogsCovariant: List[Animal] = List(lassie, hutchy, laika) // list of dogs is a list of animals

  // if NO: type is Invariant
  class MyInvariantList[T]
  // val myDogsInvariant: MyInvariantList[Animal] = new MyInvariantList[Dog] // compile error
  val myAnimalsInvariant: MyInvariantList[Animal] = new MyInvariantList[Animal]

  // If Dog <: Animal => List[Dog] :> List[Animal] ?
  // if YES: type is Contravariant
  class MyContravariantList[-T]
  val myDogsContravariant: MyContravariantList[Dog] = new MyContravariantList[Animal]

  // ======CONTRAVARIANCE USE CASE======== //
  trait Vet[-T <: Animal] {
    def heal(animal: T): Boolean
  }

  // He can heal all animals
  def giveMeAVet(): Vet[Dog] = new Vet[Animal] {
    override def heal(animal: Animal): Boolean = {
      println("You'll be fine")
      true
    }
  }

  val myDog: Dog      = new Dog("Buddy")
  val myVet: Vet[Dog] = giveMeAVet()
  myVet.heal(myDog)

  // If your generic type contains or creates elements it should be covariant +T
  // Examples: cage, garage, a factory, a list
  // If your generic type acts on or consumes elements it should be contravariant -T
  // Examples: a vet, mechanic, a garbage pit, a function (in terms of arg type)
}
