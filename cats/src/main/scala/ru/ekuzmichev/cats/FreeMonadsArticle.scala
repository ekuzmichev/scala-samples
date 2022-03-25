package ru.ekuzmichev.cats

// https://perevillega.com/understanding-free-monads/
object FreeMonadsArticle extends App {
  type Symbol   = String
  type Response = String

  sealed trait Orders[A]
  case class Buy(stock: Symbol, amount: Int)  extends Orders[Response]
  case class Sell(stock: Symbol, amount: Int) extends Orders[Response]

  import cats.free.Free

  type OrdersF[A] = Free[Orders, A]

  import cats.free.Free.liftF

  def buy(stock: Symbol, amount: Int): OrdersF[Response]  = liftF[Orders, Response](Buy(stock, amount))
  def sell(stock: Symbol, amount: Int): OrdersF[Response] = liftF[Orders, Response](Sell(stock, amount))

  val smartTrade: OrdersF[Response] = for {
    _   <- buy("APPL", 50)
    _   <- buy("MSFT", 10)
    rsp <- sell("GOOG", 200)
  } yield rsp

  import cats.{Id, ~>}

  // This is an interpreter
  def orderPrinter: Orders ~> Id =
    new (Orders ~> Id) {
      def apply[A](fa: Orders[A]): Id[A] = fa match {
        case Buy(stock, amount) =>
          println(s"Buying $amount of $stock")
          "ok"
        case Sell(stock, amount) =>
          println(s"Selling $amount of $stock")
          "ok"
      }
    }

  val res: Id[Response] = smartTrade.foldMap(orderPrinter)

  println(res)

  type ErrorOr[A] = Either[String, A]

  def eitherInterpreter: Orders ~> ErrorOr =
    new (Orders ~> ErrorOr) {
      def apply[A](fa: Orders[A]): ErrorOr[A] =
        fa match {
          case Buy(stock, amount) =>
            Right(s"$stock - $amount")
          case Sell(stock, amount) =>
            Left("Why are you selling that?")
        }
    }

  val res2: ErrorOr[Response] = smartTrade.foldMap(eitherInterpreter)

  println(res2)

}
