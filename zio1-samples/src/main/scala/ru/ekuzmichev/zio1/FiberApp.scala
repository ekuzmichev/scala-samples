package ru.ekuzmichev.zio1

import zio._
import zio.duration._
import zio.random._

//$COVERAGE-OFF$
object FiberApp1 extends App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (for {
      ref   <- Ref.make(Map.empty[String, Throwable])
      _     <- UIO(println(s"Starting"))
      fork1 <- process1(ref).forkDaemon
      fork2 <- process2(ref).forkDaemon
      _     <- supervisorProcess(ref)
    } yield ()).exitCode

  def process1(ref: Ref[Map[String, Throwable]]) = (UIO(println("Process 1 running")) *> ZIO.sleep(5.seconds)).forever
  def process2(ref: Ref[Map[String, Throwable]]) =
    nextBoolean.flatMap {
      case true  => UIO(println("Process 2 running")) *> ZIO.sleep(10.seconds)
      case false =>
        UIO(println("Process 2 ERROR")) *> ref.update(_ + ("2" -> ConnectionError)) *> IO.fail(ProcessInterrupted)
    }.forever.catchSome { case ProcessInterrupted => UIO(println(s"The process 2 has been interrupted")) }

  def supervisorProcess(ref: Ref[Map[String, Throwable]]) = {
    val key2 = "2"
    ref.get
      .flatMap(_.get(key2) match {
        case Some(value) =>
          (value match {
            case ConnectionError => UIO(println("Got connection error")) *> IO.fail(ConnectionError)
            case other           => UIO(println(s"Got $other error"))
          }) *> ref.update(_ - "2")
        case None        => UIO.unit
      })
      .forever
  }

  case object ConnectionError    extends RuntimeException
  case object ProcessInterrupted extends RuntimeException
}

object FiberApp2 extends App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (for {
      queue <- Queue.unbounded[(String, Throwable)]
      _     <- UIO(println(s"Starting"))
      _     <- process1(queue).forkDaemon
      _     <- process2(queue).forkDaemon
      _     <- supervisorProcess(queue)
    } yield ()).exitCode

  def process1(queue: Queue[(String, Throwable)]) =
    (UIO(println("Process 1 running")) *> ZIO.sleep(5.seconds)).forever

  def process2(queue: Queue[(String, Throwable)])          =
    nextBoolean.flatMap {
      case true  => UIO(println("Process 2 running")) *> ZIO.sleep(10.seconds)
      case false =>
        nextBoolean.flatMap {
          case true  =>
            UIO(println("Process 2 ConnectionError")) *> queue.offer("2" -> ConnectionError) *>
              IO.fail(ProcessInterrupted)
          case false =>
            UIO(println("Process 2 RequestError")) *> queue.offer("2" -> RequestError)
        }
    }.forever.catchSome { case ProcessInterrupted => UIO(println(s"The process 2 has been interrupted")) }

  def supervisorProcess(queue: Queue[(String, Throwable)]) =
    queue.take.flatMap { case (key, error) =>
      error match {
        case ConnectionError => UIO(println(s"[#$key]Got ConnectionError")) *> IO.fail(ConnectionError)
        case RequestError    => UIO(println(s"[#$key]Got RequestError"))
        case other           => UIO(println(s"[#$key]Got $other error"))
      }
    }.forever

  case object ConnectionError    extends RuntimeException
  case object RequestError       extends RuntimeException
  case object ProcessInterrupted extends RuntimeException
}

trait Supervisor[E] {
  def supervise[A](io: IO[E, A]): IO[E, A]
  def start(): IO[E, Unit]
}

case class Event[E](id: String, error: E)

class SupervisorImpl[E](queue: Queue[Event[E]]) extends Supervisor[E] {
  override def supervise[A](io: IO[E, A]): IO[E, A] = ???

  override def start(): IO[E, Unit] =
    queue.take.map { case Event(id, error) =>
      UIO(println(s"Got id=$id, error=$error")) *> {
        ???
      }
    }.forever
}
//$COVERAGE-ON$
