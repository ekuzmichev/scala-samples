package ru.ekuzmichev.ce

import cats.effect._
import cats.effect.std.Console
import cats.syntax.all._

import java.io.{ Console => _, _ }

object tutorial {
  def copy[F[_]: Sync: Console](origin: File, destination: File, bufferSize: Int): F[Long] =
    inputOutputStreams(origin, destination).use {
      case (in, out) => transfer(in, out, bufferSize)
    }

  def inputStream[F[_]: Sync: Console](f: File): Resource[F, FileInputStream] =
    Resource.make {
      Console[F].println("Closing input stream") >> Sync[F].blocking(new FileInputStream(f))
    }(inStream => Sync[F].blocking(inStream.close()).handleErrorWith(_ => Sync[F].unit))

  def outputStream[F[_]: Sync: Console](f: File): Resource[F, FileOutputStream] =
    Resource.make {
      Console[F].println("Closing output stream") >> Sync[F].blocking(new FileOutputStream(f)) // build
    }(outStream => Sync[F].blocking(outStream.close()).handleErrorWith(_ => Sync[F].unit))

  def inputOutputStreams[F[_]: Sync: Console](in: File, out: File): Resource[F, (InputStream, OutputStream)] =
    for {
      inStream  <- inputStream(in)
      outStream <- outputStream(out)
    } yield (inStream, outStream)

  def transmit[F[_]: Sync](origin: InputStream, destination: OutputStream, buffer: Array[Byte], acc: Long): F[Long] =
    for {
      amount <- Sync[F].blocking(origin.read(buffer, 0, buffer.length))
      count <- if (amount > -1)
                Sync[F].blocking(destination.write(buffer, 0, amount)) >>
                  transmit(origin, destination, buffer, acc + amount)
              else Sync[F].pure(acc)
    } yield count

  def transfer[F[_]: Sync](origin: InputStream, destination: OutputStream, bufferSize: Int): F[Long] =
    transmit(origin, destination, new Array[Byte](bufferSize), 0L)
}

import tutorial.copy

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- if (args.length < 2) IO.raiseError(new IllegalArgumentException("Need origin and destination files"))
          else IO.unit
      orig = new File(args(0))
      dest = new File(args(1))
      bufferSizeRaw <- IO(args(2))
                        .handleErrorWith(_ => IO.raiseError(new IllegalArgumentException(s"Not found arg bufferSize")))
      bufferSize <- IO(bufferSizeRaw.toInt)
                     .handleErrorWith(_ =>
                       IO.raiseError(new IllegalArgumentException(s"Argument bufferSize not parseable to int"))
                     )
      _ <- if (orig.getPath == dest.getPath)
            IO.raiseError(new IllegalArgumentException(s"Origin file can not be destination file"))
          else IO.unit
      _ <- if (dest.exists())
            IO.println(s"Destination file ${dest.getPath} already exists. Continue? [yN]") >>
              IO.readLine.flatMap(input => if (input.toLowerCase == "y") doCopy(orig, dest, bufferSize) else IO.unit)
          else doCopy(orig, dest, bufferSize)
    } yield ExitCode.Success

  private def doCopy(orig: File, dest: File, bufferSize: Int): IO[Unit] =
    for {
      count <- ((origin: File, destination: File) => copy[IO](origin, destination, bufferSize))(orig, dest)
      _     <- IO.println(s"$count bytes copied from ${orig.getPath} to ${dest.getPath}")
    } yield ()
}
