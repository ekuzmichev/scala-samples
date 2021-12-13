package ru.ekuzmichev.ce

import cats.effect.{ ExitCode, IO, IOApp, Resource }

import java.io._

object tutorial {
  def copy(origin: File, destination: File): IO[Long] = inputOutputStreams(origin, destination).use {
    case (in, out) => transfer(in, out)
  }

  def inputStream(f: File): Resource[IO, FileInputStream] =
    Resource.make {
      IO.blocking(new FileInputStream(f))
    }(inStream => IO.println("closing in stream") >> IO.blocking(inStream.close()).handleErrorWith(_ => IO.unit))

  def outputStream(f: File): Resource[IO, FileOutputStream] =
    Resource.make {
      IO.blocking(new FileOutputStream(f)) // build
    } { outStream =>
      IO.println("closing out stream") >> IO.blocking(outStream.close()).handleErrorWith(_ => IO.unit) // release
    }

  def inputOutputStreams(in: File, out: File): Resource[IO, (InputStream, OutputStream)] =
    for {
      inStream  <- inputStream(in)
      outStream <- outputStream(out)
    } yield (inStream, outStream)

  def transmit(origin: InputStream, destination: OutputStream, buffer: Array[Byte], acc: Long): IO[Long] =
    for {
      amount <- IO.blocking(origin.read(buffer, 0, buffer.length))
      count <- if (amount > -1)
                IO.blocking(destination.write(buffer, 0, amount)) >> transmit(origin, destination, buffer, acc + amount)
              else IO.pure(acc) // End of read stream reached (by java.io.InputStream contract), nothing to write
    } yield count               // Returns the actual amount of bytes transmitted // Returns the actual amount of bytes transmitted

  def transfer(origin: InputStream, destination: OutputStream): IO[Long] =
    transmit(origin, destination, new Array[Byte](1024 * 10), 0L)
}

import tutorial.copy

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- if (args.length < 2) IO.raiseError(new IllegalArgumentException("Need origin and destination files"))
          else IO.unit
      orig  = new File(args(0))
      dest  = new File(args(1))
      count <- copy(orig, dest)
      _     <- IO.println(s"$count bytes copied from ${orig.getPath} to ${dest.getPath}")
    } yield ExitCode.Success
}
