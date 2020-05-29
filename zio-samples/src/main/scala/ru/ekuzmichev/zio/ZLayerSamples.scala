package ru.ekuzmichev.zio

import zio._
import zio.console._
import zio.clock._
import zio.random._

object ZLayerSamples {
  case class UserId(value: Long)
  case class User(id: UserId, name: String)
  case class DBError(msg: String)

  type UserRepo = Has[UserRepo.Service]

  object UserRepo {
    trait Service {
      def getUser(userId: UserId): IO[DBError, Option[User]]
      def createUser(user: User): IO[DBError, Unit]
    }

    // This simple live version depends only on a DB Connection
    val inMemory: Layer[Nothing, UserRepo] = ZLayer.succeed(
      new Service {
        def getUser(userId: UserId): IO[DBError, Option[User]] = UIO(???)
        def createUser(user: User): IO[DBError, Unit]          = UIO(???)
      }
    )

    def getUser(userId: UserId): ZIO[UserRepo, DBError, Option[User]] = ZIO.accessM(_.get.getUser(userId))
    def createUser(user: User): ZIO[UserRepo, DBError, Unit]          = ZIO.accessM(_.get.createUser(user))
  }

  type Logging = Has[Logging.Service]

  object Logging {
    trait Service {
      def info(s: String): UIO[Unit]
      def error(s: String): UIO[Unit]
    }

    val consoleLogger: ZLayer[Console, Nothing, Logging] = ZLayer.fromFunction(console =>
      new Service {
        def info(s: String): UIO[Unit]  = console.get.putStrLn(s"info - $s")
        def error(s: String): UIO[Unit] = console.get.putStrLn(s"error - $s")
      }
    )

    //accessor methods
    def info(s: String): ZIO[Logging, Nothing, Unit]  = ZIO.accessM(_.get.info(s))
    def error(s: String): ZIO[Logging, Nothing, Unit] = ZIO.accessM(_.get.error(s))
  }

//  ZLayer[-RIn, +E, +ROut <: Has[_]]
//  val repo: Has[UserRepo.Service]                          = Has(UserRepo.inMemory)
//  val logger: Has[Logging.Service]                         = Has(Logging.consoleLogger)
//  val mix: Has[UserRepo.Service] with Has[Logging.Service] = repo ++ logger
//  // get back the logger service from the mixed value:
//  val log: UIO[Unit] = mix.get[Logging.Service].info("Hello modules!")

  val user: User = User(UserId(123), "Tommy")
  val makeUser: ZIO[Logging with UserRepo, DBError, Unit] = for {
    _ <- Logging.info(s"inserting user") // ZIO[Logging, Nothing, Unit]
    _ <- UserRepo.createUser(user)       // ZIO[UserRepo, DBError, Unit]
    _ <- Logging.info(s"user inserted")  // ZIO[Logging, Nothing, Unit]
  } yield ()

  // compose horizontally
  val horizontal: ZLayer[Console, Nothing, Logging with UserRepo] = Logging.consoleLogger ++ UserRepo.inMemory

  // fulfill missing deps, composing vertically
  val fullLayer: Layer[Nothing, Logging with UserRepo] = Console.live >>> horizontal

  // provide the layer to the program
  makeUser.provideLayer(fullLayer)

  val makeUser2: ZIO[Logging with UserRepo with Clock with Random, DBError, Unit] = for {
    userId    <- zio.random.nextLong.map(UserId)
    createdAt <- zio.clock.currentDateTime.orDie
    _         <- Logging.info(s"inserting user")
    _         <- UserRepo.createUser(User(userId, "Chet"))
    _         <- Logging.info(s"user inserted, created at $createdAt")
  } yield ()

  val zEnvMakeUser: ZIO[ZEnv, DBError, Unit] = makeUser2.provideCustomLayer(fullLayer)

  val withPostgresService: ZLayer[Console, Nothing, Logging with UserRepo] = horizontal.update[UserRepo.Service] {
    oldRepo =>
      new UserRepo.Service {
        override def getUser(userId: UserId): IO[DBError, Option[User]] = UIO(???)
        override def createUser(user: User): IO[DBError, Unit]          = UIO(???)
      }
  }

  val dbLayer: Layer[Nothing, UserRepo] = ZLayer.succeed(new UserRepo.Service {
    override def getUser(userId: UserId): IO[DBError, Option[User]] = ???
    override def createUser(user: User): IO[DBError, Unit]          = ???
  })

  val updatedHorizontal2: ZLayer[Console, Nothing, Logging with UserRepo] = horizontal ++ dbLayer

  import java.sql.Connection
  def makeConnection: UIO[Connection] = UIO(???)

  val connectionLayer: Layer[Nothing, Has[Connection]] = ZLayer.fromAcquireRelease(makeConnection)(c => UIO(c.close()))
  val postgresLayer: ZLayer[Has[Connection], Nothing, UserRepo] =
    ZLayer.fromFunction { hasC =>
      new UserRepo.Service {
        override def getUser(userId: UserId): IO[DBError, Option[User]] = UIO(???)
        override def createUser(user: User): IO[DBError, Unit]          = UIO(???)
      }
    }

  class CachedUserRepoService(userRepo: UserRepo.Service) extends UserRepo.Service {
    override def getUser(userId: UserId): IO[DBError, Option[User]] = ???
    override def createUser(user: User): IO[DBError, Unit]          = ???
  }

//  val cachedUserRepoLayer: ZLayer[Has[UserRepo], Nothing, UserRepo] =
//    ZLayer.fromService(hasUserRepo => new CachedUserRepoService(hasUserRepo.get))

  val fullRepo: Layer[Nothing, UserRepo] = connectionLayer >>> postgresLayer // >>> cachedUserRepoLayer

  val connection: ZLayer[Any, Nothing, Has[Connection]]    = connectionLayer
  val userRepo: ZLayer[Has[Connection], Nothing, UserRepo] = postgresLayer
  val layer: ZLayer[Any, Nothing, UserRepo]                = connection >>> userRepo

  val updatedLayer: ZLayer[Any, Nothing, UserRepo] = dbLayer

  val layerPassedThrough: ZLayer[Any, Nothing, Has[Connection] with UserRepo] = connection >+> userRepo

  trait Baker
  trait Ingredients
  trait Oven
  trait Dough
  trait Cake
  lazy val baker: ZLayer[Any, Nothing, Baker]                      = ???
  lazy val ingredients: ZLayer[Any, Nothing, Ingredients]          = ???
  lazy val oven: ZLayer[Any, Nothing, Oven]                        = ???
  lazy val dough: ZLayer[Baker with Ingredients, Nothing, Dough]   = ???
  lazy val cake: ZLayer[Baker with Oven with Dough, Nothing, Cake] = ???

//  lazy val all: ZLayer[Any, Nothing, Baker with Ingredients with Oven with Dough with Cake] =
//    baker >+>       // Baker
//      ingredients >+> // Baker with Ingredients
//      oven >+>        // Baker with Ingredients with Oven
//      dough >+>       // Baker with Ingredients with Oven with Dough
//      cake            // Baker with Ingredients with Oven with Dough with Cake

//  lazy val hidden: ZLayer[Any, Nothing, Cake] = all

  case class Pet(id: Long, name: String)
  case class PetRepoError(msg: String)
  case class DbError(msg: String)

  class MongoDatabase {
    def find(id: Long): IO[DbError, Option[Pet]] = ???
  }
  
  type PetRepo = Has[PetRepo.Service]

  object PetRepo {
    trait Service {
      def getPet(id: Long): IO[PetRepoError, Option[Pet]]
    }
    val inMemory: Layer[Nothing, PetRepo] = ZLayer.succeed(
      new Service {
        val pets: Map[Long, Pet] = ???
        
        override def getPet(id: Long): IO[PetRepoError, Option[Pet]] = ???
      }
    )

    val mongoDb: ZLayer[Has[MongoDatabase], Nothing, PetRepo] = ZLayer.fromFunction{
      hasMongoDatabase => new Service {
        val database = hasMongoDatabase.get
        override def getPet(id: Long): IO[PetRepoError, Option[Pet]] =
          database.find(id).mapError(err => PetRepoError(s"db error: $err"))
      }
    }

  }
}
