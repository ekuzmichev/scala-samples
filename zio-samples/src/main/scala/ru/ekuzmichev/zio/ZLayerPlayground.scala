package ru.ekuzmichev.zio

import zio._

object ZLayerPlayground {
  case class User(id: String, name: String)

  class MongoDb {
    def insertUser(user: User): Task[Unit] = Task.succeed(()) <* UIO(println(s"[MongoDb]: Inserted user $user"))
  }

  object MongoDbLayer {
    val live: ZLayer[Any, Nothing, Has[MongoDb]] =
      ZLayer.fromEffect(UIO.succeed(new MongoDb) <* UIO(println("[DI]: Created MongoDb")))
  }

  trait Mail {
    def send(msg: String): UIO[Unit]
  }

  class ConsoleMail extends Mail {
    override def send(msg: String): UIO[Unit] = UIO.effectTotal(println(s"[ConsoleMail]: $msg"))
  }

  object MailLayer {
    val console: ULayer[Has[Mail]] =
      ZLayer.fromEffect(UIO.succeed(new ConsoleMail) <* UIO(println("[DI]: Created ConsoleMail")))
  }

  trait UserRepository {
    def createUser(user: User): Task[Unit]
  }

  class MongoUserRepository(mongoDb: MongoDb, mail: Mail) extends UserRepository {
    override def createUser(user: User): Task[Unit] =
      for {
        _ <- UIO(println("[MongoUserRepository]: createUser")) *> mongoDb.insertUser(user)
        _ <- mail.send(s"Msg: Done for MongoUserRepository")
      } yield ()
  }

  class CachedUserRepository(decoratee: UserRepository) extends UserRepository {
    override def createUser(user: User): Task[Unit] =
      UIO(println("[CachedUserRepository]: createUser")) *> decoratee.createUser(user)
  }

  object UserRepositoryLayer {
    val live: ZLayer[Has[MongoDb] with Has[Mail], Nothing, Has[UserRepository]] =
      ZLayer.fromFunctionM(env =>
        UIO.succeed(new MongoUserRepository(env.get[MongoDb], env.get[Mail])) <* UIO(
          println("[DI]: Created MongoUserRepository")
        )
      )

    val cached: ZLayer[Has[UserRepository], Nothing, Has[UserRepository]] =
      ZLayer.fromServiceM(userRepository =>
        UIO.succeed(new CachedUserRepository(userRepository)) <* UIO(println("[DI]: Created CachedUserRepository"))
      )
  }

  trait UserService {
    def processUser(user: User): Task[Unit]
  }

  class UserServiceImpl(userRepository: UserRepository, mail: Mail) extends UserService {
    override def processUser(user: User): Task[Unit] =
      for {
        _ <- UIO(println(s"[UserServiceImpl]: Processing $user"))
        _ <- userRepository.createUser(user)
        _ <- UIO(println(s"[UserServiceImpl]: Processing $user...DONE"))
        _ <- mail.send(s"Msg: Done for UserServiceImpl")
      } yield ()
  }

  object UserServiceLayer {
    val live: ZLayer[Has[UserRepository] with Has[Mail], Nothing, Has[UserService]] =
      ZLayer.fromFunctionM(env =>
        UIO.succeed(new UserServiceImpl(env.get[UserRepository], env.get[Mail])) <* UIO(
          println("[DI]: Created UserServiceImpl")
        )
      )
  }
}

object ZLayerApp extends App {
  import ZLayerPlayground._

  val baseLayer: ULayer[Has[Mail]] = MailLayer.console
  val userRepoLayer: URLayer[Has[Mail], Has[UserRepository]] =
    ZLayer.requires[Has[Mail]] ++ MongoDbLayer.live >>> UserRepositoryLayer.live >>> UserRepositoryLayer.cached
  val userServiceLayer: URLayer[Has[UserRepository] with Has[Mail], Has[UserService]] = UserServiceLayer.live
  val programLayer: ULayer[Has[UserService]]                                          = baseLayer >+> userRepoLayer >>> userServiceLayer

  val program: ZIO[Has[UserService], Throwable, Unit] =
    ZIO.accessM(_.get.processUser(User("1", "Evgenii")))

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] =
    program
      .provideLayer(programLayer)
      .catchAll(t => ZIO.succeed(t.printStackTrace()).map(_ => ExitCode.failure))
      .as(ExitCode.success)

}
