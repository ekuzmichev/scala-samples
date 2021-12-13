package ru.ekuzmichev.ce

import cats.effect._
import cats.effect.std.Console
import cats.syntax.all._
import cats.effect.syntax.all._
import collection.immutable.Queue

object ineficcient {
  def producer[F[_]: Sync: Console](queueR: Ref[F, Queue[Int]], counter: Int): F[Unit] =
    for {
      _ <- if (counter % 10000 == 0) Console[F].println(s"Produced $counter items") else Sync[F].unit
      _ <- queueR.getAndUpdate(_.enqueue(counter + 1))
      _ <- producer(queueR, counter + 1)
    } yield ()

  def consumer[F[_]: Sync: Console](queueR: Ref[F, Queue[Int]]): F[Unit] =
    for {
      iO <- queueR.modify { queue =>
             queue.dequeueOption.fold((queue, Option.empty[Int])) { case (i, queue) => (queue, Option(i)) }
           }
      _ <- if (iO.exists(_ % 10000 == 0)) Console[F].println(s"Consumed ${iO.get} items") else Sync[F].unit
      _ <- consumer(queueR)
    } yield ()
}

object InefficientProducerConsumer extends IOApp {
  import ineficcient._

  override def run(args: List[String]): IO[ExitCode] =
    for {
      queueR <- Ref.of[IO, Queue[Int]](Queue.empty[Int])
      res <- (consumer(queueR), producer(queueR, 0))
              .parMapN((_, _) => ExitCode.Success) // Run producer and consumer in parallel until done (likely by user cancelling with CTRL-C)
              .handleErrorWith(t => Console[IO].errorln(s"Error caught: ${t.getMessage}").as(ExitCode.Error))
    } yield res

}

object fiber {
  case class State[F[_], A](queue: Queue[A], takers: Queue[Deferred[F, A]])

  object State {
    def empty[F[_], A]: State[F, A] = State(Queue.empty, Queue.empty)
  }

  def consumer[F[_]: Async: Console](id: Int, stateR: Ref[F, State[F, Int]]): F[Unit] = {

    val take: F[Int] =
      Deferred[F, Int].flatMap { taker =>
        stateR.modify {
          case State(queue, takers) if queue.nonEmpty =>
            val (i, rest) = queue.dequeue
            State(rest, takers) -> Async[F].pure(i) // Got element in queue, we can just return it
          case State(queue, takers) =>
            State(queue, takers.enqueue(taker)) -> taker.get // No element in queue, must block caller until some is available
        }.flatten
      }

    for {
      i <- take
      _ <- if (i % 10000 == 0) Console[F].println(s"Consumer $id has reached $i items") else Async[F].unit
      _ <- consumer(id, stateR)
    } yield ()
  }

  def producer[F[_]: Sync: Console](id: Int, counterR: Ref[F, Int], stateR: Ref[F, State[F, Int]]): F[Unit] = {

    def offer(i: Int): F[Unit] =
      stateR.modify {
        case State(queue, takers) if takers.nonEmpty =>
          val (taker, rest) = takers.dequeue
          State(queue, rest) -> taker.complete(i).void
        case State(queue, takers) =>
          State(queue.enqueue(i), takers) -> Sync[F].unit
      }.flatten

    for {
      i <- counterR.getAndUpdate(_ + 1)
      _ <- offer(i)
      _ <- if (i % 10000 == 0) Console[F].println(s"Producer $id has reached $i items") else Sync[F].unit
      _ <- producer(id, counterR, stateR)
    } yield ()
  }
}

object ProducerConsumer extends IOApp {

  import fiber._

  override def run(args: List[String]): IO[ExitCode] =
    for {
      stateR    <- Ref.of[IO, State[IO, Int]](State.empty[IO, Int])
      counterR  <- Ref.of[IO, Int](1)
      producers = List.range(1, 11).map(producer(_, counterR, stateR)) // 10 producers
      consumers = List.range(1, 11).map(consumer(_, stateR)) // 10 consumers
      res <- (producers ++ consumers).parSequence
              .as(
                ExitCode.Success
              ) // Run producers and consumers in parallel until done (likely by user cancelling with CTRL-C)
              .handleErrorWith(t => Console[IO].errorln(s"Error caught: ${t.getMessage}").as(ExitCode.Error))
    } yield res
}

object bounded {
  case class State[F[_], A](
    queue: Queue[A],
    capacity: Int,
    takers: Queue[Deferred[F, A]],
    offerers: Queue[(A, Deferred[F, Unit])]
  )

  object State {
    def empty[F[_], A](capacity: Int): State[F, A] = State(Queue.empty, capacity, Queue.empty, Queue.empty)
  }

  def consumer[F[_]: Async: Console](id: Int, stateR: Ref[F, State[F, Int]]): F[Unit] = {

    val take: F[Int] =
      Deferred[F, Int].flatMap { taker =>
        stateR.modify {
          case State(queue, capacity, takers, offerers) if queue.nonEmpty && offerers.isEmpty =>
            val (i, rest) = queue.dequeue
            State(rest, capacity, takers, offerers) -> Async[F].pure(i)
          case State(queue, capacity, takers, offerers) if queue.nonEmpty =>
            val (i, rest)               = queue.dequeue
            val ((move, release), tail) = offerers.dequeue
            State(rest.enqueue(move), capacity, takers, tail) -> release.complete(()).as(i)
          case State(queue, capacity, takers, offerers) if offerers.nonEmpty =>
            val ((i, release), rest) = offerers.dequeue
            State(queue, capacity, takers, rest) -> release.complete(()).as(i)
          case State(queue, capacity, takers, offerers) =>
            State(queue, capacity, takers.enqueue(taker), offerers) -> taker.get
        }.flatten
      }

    for {
      i <- take
      _ <- if (i % 10000 == 0) Console[F].println(s"Consumer $id has reached $i items") else Async[F].unit
      _ <- consumer(id, stateR)
    } yield ()
  }

  def producer[F[_]: Async: Console](id: Int, counterR: Ref[F, Int], stateR: Ref[F, State[F, Int]]): F[Unit] = {

    def offer(i: Int): F[Unit] =
      Deferred[F, Unit].flatMap[Unit] { offerer =>
        stateR.modify {
          case State(queue, capacity, takers, offerers) if takers.nonEmpty =>
            val (taker, rest) = takers.dequeue
            State(queue, capacity, rest, offerers) -> taker.complete(i).void
          case State(queue, capacity, takers, offerers) if queue.size < capacity =>
            State(queue.enqueue(i), capacity, takers, offerers) -> Async[F].unit
          case State(queue, capacity, takers, offerers) =>
            State(queue, capacity, takers, offerers.enqueue(i -> offerer)) -> offerer.get
        }.flatten
      }

    for {
      i <- counterR.getAndUpdate(_ + 1)
      _ <- offer(i)
      _ <- if (i % 10000 == 0) Console[F].println(s"Producer $id has reached $i items") else Async[F].unit
      _ <- producer(id, counterR, stateR)
    } yield ()
  }
}

object ProducerConsumerBounded extends IOApp {

  import bounded._

  override def run(args: List[String]): IO[ExitCode] =
    for {
      stateR    <- Ref.of[IO, State[IO, Int]](State.empty[IO, Int](capacity = 100))
      counterR  <- Ref.of[IO, Int](1)
      producers = List.range(1, 11).map(producer(_, counterR, stateR)) // 10 producers
      consumers = List.range(1, 11).map(consumer(_, stateR)) // 10 consumers
      res <- (producers ++ consumers).parSequence
              .as(
                ExitCode.Success
              ) // Run producers and consumers in parallel until done (likely by user cancelling with CTRL-C)
              .handleErrorWith(t => Console[IO].errorln(s"Error caught: ${t.getMessage}").as(ExitCode.Error))
    } yield res
}

object poll {
  case class State[F[_], A](
    queue: Queue[A],
    capacity: Int,
    takers: Queue[Deferred[F, A]],
    offerers: Queue[(A, Deferred[F, Unit])]
  )

  object State {
    def empty[F[_], A](capacity: Int): State[F, A] = State(Queue.empty, capacity, Queue.empty, Queue.empty)
  }

  def producer[F[_]: Async: Console](id: Int, counterR: Ref[F, Int], stateR: Ref[F, State[F, Int]]): F[Unit] = {

    def offer(i: Int): F[Unit] =
      Deferred[F, Unit].flatMap[Unit] { offerer =>
        Async[F].uncancelable { poll => // `poll` used to embed cancelable code, i.e. the call to `offerer.get`
          stateR.modify {
            case State(queue, capacity, takers, offerers) if takers.nonEmpty =>
              val (taker, rest) = takers.dequeue
              State(queue, capacity, rest, offerers) -> taker.complete(i).void
            case State(queue, capacity, takers, offerers) if queue.size < capacity =>
              State(queue.enqueue(i), capacity, takers, offerers) -> Async[F].unit
            case State(queue, capacity, takers, offerers) =>
              val cleanup = stateR.update(s => s.copy(offerers = s.offerers.filter(_._2 ne offerer)))
              State(queue, capacity, takers, offerers.enqueue(i -> offerer)) -> poll(offerer.get).onCancel(cleanup)
          }.flatten
        }
      }

    for {
      i <- counterR.getAndUpdate(_ + 1)
      _ <- offer(i)
      _ <- if (i % 10000 == 0) Console[F].println(s"Producer $id has reached $i items") else Async[F].unit
      _ <- producer(id, counterR, stateR)
    } yield ()
  }

  def consumer[F[_]: Async: Console](id: Int, stateR: Ref[F, State[F, Int]]): F[Unit] = {

    val take: F[Int] =
      Deferred[F, Int].flatMap { taker =>
        Async[F].uncancelable { poll =>
          stateR.modify {
            case State(queue, capacity, takers, offerers) if queue.nonEmpty && offerers.isEmpty =>
              val (i, rest) = queue.dequeue
              State(rest, capacity, takers, offerers) -> Async[F].pure(i)
            case State(queue, capacity, takers, offerers) if queue.nonEmpty =>
              val (i, rest)               = queue.dequeue
              val ((move, release), tail) = offerers.dequeue
              State(rest.enqueue(move), capacity, takers, tail) -> release.complete(()).as(i)
            case State(queue, capacity, takers, offerers) if offerers.nonEmpty =>
              val ((i, release), rest) = offerers.dequeue
              State(queue, capacity, takers, rest) -> release.complete(()).as(i)
            case State(queue, capacity, takers, offerers) =>
              val cleanup = stateR.update(s => s.copy(takers = s.takers.filter(_ ne taker)))
              State(queue, capacity, takers.enqueue(taker), offerers) -> poll(taker.get).onCancel(cleanup)
          }.flatten
        }
      }

    for {
      i <- take
      _ <- if (i % 10000 == 0) Console[F].println(s"Consumer $id has reached $i items") else Async[F].unit
      _ <- consumer(id, stateR)
    } yield ()
  }
}

object ProducerConsumerBoundedPoll extends IOApp {

  import poll._

  override def run(args: List[String]): IO[ExitCode] =
    for {
      stateR    <- Ref.of[IO, State[IO, Int]](State.empty[IO, Int](capacity = 100))
      counterR  <- Ref.of[IO, Int](1)
      producers = List.range(1, 11).map(producer(_, counterR, stateR)) // 10 producers
      consumers = List.range(1, 11).map(consumer(_, stateR)) // 10 consumers
      res <- (producers ++ consumers).parSequence
              .as(
                ExitCode.Success
              ) // Run producers and consumers in parallel until done (likely by user cancelling with CTRL-C)
              .handleErrorWith(t => Console[IO].errorln(s"Error caught: ${t.getMessage}").as(ExitCode.Error))
    } yield res
}
