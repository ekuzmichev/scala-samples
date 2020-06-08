package ru.ekuzmichev.zio

import zio._

object ZLayerPlayground {
  case class User(id: String, name: String)

  class MongoDb {
    def insertUser(user: User): Task[Unit] = Task.succeed(())
  }

  object MongoDbLayer {
    val live: ZLayer[Any, Nothing, Has[MongoDb]] =
      ZLayer.fromEffect(UIO.succeed(new MongoDb) <* UIO(println("Created MongoDb")))
  }

  trait Mail {
    def send(msg: String): UIO[Unit]
  }

  class ConsoleMail extends Mail {
    override def send(msg: String): UIO[Unit] = UIO.effectTotal(println(msg))
  }

  object MailLayer {
    val console: ULayer[Has[Mail]] =
      ZLayer.fromEffect(UIO.succeed(new ConsoleMail) <* UIO(println("Created ConsoleMail")))
  }

  trait UserRepository {
    def createUser(user: User): Task[Unit]
  }

  class MongoUserRepository(mongoDb: MongoDb, mail: Mail) extends UserRepository {
    override def createUser(user: User): Task[Unit] = UIO(println("[MONGO createUser]")) *> mongoDb.insertUser(user)
  }

  class CachedUserRepository(decoratee: UserRepository) extends UserRepository {
    override def createUser(user: User): Task[Unit] =
      UIO(println("[CACHED createUser]")) *> decoratee.createUser(user)
  }

  object UserRepositoryLayer {
    val live: ZLayer[Has[MongoDb] with Has[Mail], Nothing, Has[UserRepository]] =
      ZLayer.fromFunctionM(env =>
        UIO.succeed(new MongoUserRepository(env.get[MongoDb], env.get[Mail])) <* UIO(
          println("Created MongoUserRepository")
        )
      )

    val cached: ZLayer[Has[UserRepository], Nothing, Has[UserRepository]] =
      ZLayer.fromServiceM(userRepository =>
        UIO.succeed(new CachedUserRepository(userRepository)) <* UIO(println("Created CachedUserRepository"))
      )
  }

  trait UserService {
    def processUser(user: User): Task[Unit]
  }

  class UserServiceImpl(userRepository: UserRepository, mail: Mail) extends UserService {
    override def processUser(user: User): Task[Unit] =
      UIO(println(s"Processing $user")) *> userRepository.createUser(user) <* UIO(println(s"Processing $user...DONE"))
  }

  object UserServiceLayer {
    val live: ZLayer[Has[UserRepository] with Has[Mail], Nothing, Has[UserService]] =
      ZLayer.fromFunctionM(env =>
        UIO.succeed(new UserServiceImpl(env.get[UserRepository], env.get[Mail])) <* UIO(
          println("Created UserServiceImpl")
        )
      )
  }
}

import ru.ekuzmichev.zio.ZLayerPlayground._

object ZLayerApp extends App {
  val baseLayer: ULayer[Has[Mail]] = MailLayer.console
  val userRepoLayer: URLayer[Has[Mail], Has[UserRepository]] =
    ZLayer.requires[Has[Mail]] ++ MongoDbLayer.live >>> UserRepositoryLayer.live >>> UserRepositoryLayer.cached
  val userServiceLayer: URLayer[Has[UserRepository] with Has[Mail], Has[UserService]] = UserServiceLayer.live
  val program: ULayer[Has[UserService]]                                               = baseLayer >+> userRepoLayer >>> userServiceLayer

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] =
    program.build
      .use(_.get[UserService].processUser(User("1", "Bob")))
      .either
      .as(ExitCode.success)

}
