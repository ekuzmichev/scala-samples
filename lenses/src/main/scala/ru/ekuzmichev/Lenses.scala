package ru.ekuzmichev

object Lenses extends App {
  case class Guitar(make: String, model: String)
  case class Guitarist(name: String, favoriteGuitar: Guitar)
  case class RockBand(name: String, yearFormed: Int, leadGuitarist: Guitarist)

  val metallica = RockBand("Metallica", 1981, Guitarist("Kirk Hammett", Guitar("ESP", "M II")))

  println(metallica)

  // We want to replace all whitespaces in guitar models with dash to store in DB (e.g.)
  // The code is a mess:
  val metallicaFixed = metallica.copy(
    leadGuitarist = metallica.leadGuitarist.copy(
      favoriteGuitar = metallica.leadGuitarist.favoriteGuitar.copy(
        model = metallica.leadGuitarist.favoriteGuitar.model.replace(" ", "-")
      )
    )
  )

  println(metallicaFixed)

  // ========== MONOCLE ========== //

  // ========== Lenses ========== //
  // Lens "zooms in"
  val kirksFavGuitar = Guitar("ESP", "M II")
  import monocle.Lens
  import monocle.macros.GenLens

  val guitarModelLens: Lens[Guitar, String] = GenLens[Guitar](_.model)

  // inspect a data structure using a lens
  val kirksGuitarModel: String = guitarModelLens.get(kirksFavGuitar)
  println(kirksGuitarModel)

  // modify a data structure using a lens
  val formattedGuitar: Guitar = guitarModelLens.modify(_.replace(" ", "-"))(kirksFavGuitar)
  println(formattedGuitar)

  val leadGuitaristLens: Lens[RockBand, Guitarist] = GenLens[RockBand](_.leadGuitarist)
  val favGuitarLens: Lens[Guitarist, Guitar]       = GenLens[Guitarist](_.favoriteGuitar)

  // compose lenses
  val composedLens: Lens[RockBand, String] = leadGuitaristLens.composeLens(favGuitarLens).composeLens(guitarModelLens)
  // and with DSL
  val composedLens2: Lens[RockBand, String] = leadGuitaristLens ^|-> favGuitarLens ^|-> guitarModelLens

  val kirksGuitarModel2: String = composedLens.get(metallica)
  println(kirksGuitarModel2)

  val metallicaFixed2: RockBand = composedLens.modify(_.replace(" ", "-"))(metallica)
  println(metallicaFixed2)

  // lens is reusable

  // ========== Prisms ========== //

  sealed trait Shape
  case class Circle(radius: Double)                    extends Shape
  case class Rectangle(width: Double, height: Double)  extends Shape
  case class Triangle(a: Double, b: Double, c: Double) extends Shape

  val aCircle       = Circle(20)
  val aRectangle    = Rectangle(10, 20)
  val aTriangle     = Triangle(3, 4, 5)
  val aShape: Shape = aCircle

  // We would like to increase aShape radius if it is a Circle and leave it intact otherwise

  // naive approach
  if (aShape.isInstanceOf[Circle]) {
    // change the radius
  }

  // 2nd approach (but you repeat this every time)
  aShape match {
    case Circle(radius) => // change the radius
    case s              => s
  }

  // Prism
  // It is for either to construct shapes or to reuse shapes
  // Prism works with a type hierarchy and is just looking for one in that hierarchy
  // Prism "isolates" a type
  import monocle.Prism
  val circlePrism: Prism[Shape, Double] =
    Prism[Shape, Double] {
      case Circle(r) => Some(r)
      case _         => None
    }(r => Circle(r))

  val anotherCircle = circlePrism(30) // kind of "smart constructor"
  println(anotherCircle)

  val radius = circlePrism.getOption(aCircle)
  println(radius)
  val noRadius = circlePrism.getOption(aRectangle)
  println(noRadius)

  // We can combine Lens to zoom in and Prism to isolate a type
  case class Icon(background: String, shape: Shape)
  case class Logo(color: String)
  case class BrandIdentity(logo: Logo, icon: Icon)

  // We want to change the radius of the Icon of a brand
  val iconLens     = GenLens[BrandIdentity](_.icon)
  val shapeLens    = GenLens[Icon](_.shape)
  val brandCircleR = iconLens ^|-> shapeLens ^<-? circlePrism
  // And with no DSL
  // val brandCircleR = iconLens.composeLens(shapeLens).composePrism(circlePrism)

  val aCircleBrand: BrandIdentity = BrandIdentity(Logo("red"), Icon("white", Circle(45)))
  println(aCircleBrand)
  val enlargedCircleBrand: BrandIdentity = brandCircleR.modify(_ * 2)(aCircleBrand) // We can modify here
  println(enlargedCircleBrand)

  val aTriangleBrand: BrandIdentity = BrandIdentity(Logo("black"), Icon("red", Triangle(3, 4, 5)))
  println(aTriangleBrand)
  val enlargedRadiusTriangle: BrandIdentity = brandCircleR.modify(_ * 2)(aTriangleBrand)
  println(enlargedRadiusTriangle)

  // EXTRA with prisms
  sealed trait Json
  case object JNull                     extends Json
  case class JStr(v: String)            extends Json
  case class JNum(v: Double)            extends Json
  case class JObj(v: Map[String, Json]) extends Json

  val jStr = Prism[Json, String] {
    case JStr(v) => Some(v)
    case _       => None
  }(JStr)

  // or like this
  val jStr2 = Prism.partial[Json, String] { case JStr(v) => v }(JStr)
  println(jStr("hello"))
  println(jStr.getOption(JStr("Hello")))
  println(jStr.getOption(JNum(3.2)))
  println(jStr.set("Bar")(JStr("Hello")))
  println(jStr.modify(_.reverse)(JStr("Hello")))
  println(jStr.set("Bar")(JNum(10)))
  println(jStr.modify(_.reverse)(JNum(10)))
}
