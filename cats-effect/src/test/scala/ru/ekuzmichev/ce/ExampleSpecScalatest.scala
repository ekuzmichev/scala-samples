package ru.ekuzmichev.ce

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class ExampleSpecScalatest extends AsyncFlatSpec with Matchers with AsyncIOSpec {
  it should "make sure IO computes the right result" in {
    val io = IO.println("start") >> IO.pure(1).map(_ + 2)
    io.asserting { result => result should be(3) }
  }
}