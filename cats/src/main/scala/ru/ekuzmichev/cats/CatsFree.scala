package ru.ekuzmichev.cats

import scala.io.StdIn

object CatsFree extends App {
  import cats.free.Free
  import cats.free.Free.liftF
  import cats.data.State
  import cats.{ ~>, Id }
  import scala.collection.mutable

  sealed trait KVStoreA[A]
  case class Put[T](key: String, value: T) extends KVStoreA[Unit]
  case class Get[T](key: String)           extends KVStoreA[Option[T]]
  case class Delete(key: String)           extends KVStoreA[Unit]

  type KVStore[A] = Free[KVStoreA, A]

  def put[T](key: String, value: T): KVStore[Unit] = liftF[KVStoreA, Unit](Put(key, value))
  def get[T](key: String): KVStore[Option[T]]      = liftF[KVStoreA, Option[T]](Get(key))
  def delete[T](key: String): KVStore[Unit]        = liftF[KVStoreA, Unit](Delete(key))
  def update[T](key: String, f: T => T): KVStore[Unit] =
    for {
      vMaybe <- get[T](key)
      _      <- vMaybe.map(v => put[T](key, f(v))).getOrElse(Free.pure(()))
    } yield ()

  def program1: KVStore[Option[Int]] =
    for {
      _ <- put("wild-cats", 2)
      _ <- update[Int]("wild-cats", _ + 12)
      _ <- put("tame-cats", 5)
      n <- get[Int]("wild-cats")
      _ <- delete("tame-cats")
    } yield n

  def impureCompiler: KVStoreA ~> Id =
    new (KVStoreA ~> Id) {
      // a very simple (and imprecise) key-value store
      val kvs = mutable.Map.empty[String, Any]

      def apply[A](fa: KVStoreA[A]): Id[A] =
        fa match {
          case Put(key, value) =>
            println(s"put($key, $value)")
            kvs(key) = value
            ()
          case Get(key) =>
            println(s"get($key)")
            kvs.get(key).map(_.asInstanceOf[A])
          case Delete(key) =>
            println(s"delete($key)")
            kvs.remove(key)
            ()
        }
    }

  val result1: Option[Int] = program1.foldMap(impureCompiler)

  println(result1)

  type KVStoreState[A] = State[Map[String, Any], A]

  val pureCompiler: KVStoreA ~> KVStoreState = new (KVStoreA ~> KVStoreState) {
    def apply[A](fa: KVStoreA[A]): KVStoreState[A] =
      fa match {
        case Put(key, value) => State.modify(_.updated(key, value))
        case Get(key)        => State.inspect(_.get(key).map(_.asInstanceOf[A]))
        case Delete(key)     => State.modify(_ - key)
      }
  }

  val result2: (Map[String, Any], Option[Int]) = program1.foldMap(pureCompiler).run(Map.empty).value

  println(result2)

  import cats.data.EitherK
  import cats.free.Free
  import cats.{ ~>, Id, InjectK }
  import scala.collection.mutable.ListBuffer

  /* Handles user interaction */
  sealed trait Interact[A]
  case class Ask(prompt: String) extends Interact[String]
  case class Tell(msg: String)   extends Interact[Unit]

  /* Represents persistence operations */
  sealed trait DataOp[A]
  case class AddCat(a: String) extends DataOp[Unit]
  case class GetAllCats()      extends DataOp[List[String]]

  type CatsApp[A] = EitherK[DataOp, Interact, A]

  class Interacts[F[_]](implicit I: InjectK[Interact, F]) {
    def tell(msg: String): Free[F, Unit]     = Free.liftInject[F](Tell(msg))
    def ask(prompt: String): Free[F, String] = Free.liftInject[F](Ask(prompt))
  }

  object Interacts {
    implicit def interacts[F[_]](implicit I: InjectK[Interact, F]): Interacts[F] = new Interacts[F]
  }

  class DataSource[F[_]](implicit I: InjectK[DataOp, F]) {
    def addCat(a: String): Free[F, Unit]  = Free.liftInject[F](AddCat(a))
    def getAllCats: Free[F, List[String]] = Free.liftInject[F](GetAllCats())
  }

  object DataSource {
    implicit def dataSource[F[_]](implicit I: InjectK[DataOp, F]): DataSource[F] = new DataSource[F]
  }

  def program2(implicit I : Interacts[CatsApp], D : DataSource[CatsApp]): Free[CatsApp, Unit] = {
    import I._, D._

    for {
      cat <- ask("What's the kitty's name?")
      _ <- addCat(cat)
      cats <- getAllCats
      _ <- tell(cats.toString)
    } yield ()
  }

  object ConsoleCatsInterpreter extends (Interact ~> Id) {
    def apply[A](i: Interact[A]) = i match {
      case Ask(prompt) =>
        println(prompt)
        StdIn.readLine()
      case Tell(msg) =>
        println(msg)
    }
  }

  object InMemoryDatasourceInterpreter extends (DataOp ~> Id) {

    private[this] val memDataSet = new ListBuffer[String]

    def apply[A](fa: DataOp[A]) = fa match {
      case AddCat(a) => memDataSet.append(a); ()
      case GetAllCats() => memDataSet.toList
    }
  }

  val interpreter: CatsApp ~> Id = InMemoryDatasourceInterpreter or ConsoleCatsInterpreter

  import DataSource._, Interacts._
  val evaled: Unit = program2.foldMap(interpreter)
}
