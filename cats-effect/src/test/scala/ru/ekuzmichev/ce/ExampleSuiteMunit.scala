package ru.ekuzmichev.ce

import cats.effect.IO
import munit.CatsEffectSuite

class ExampleSuiteMunit extends CatsEffectSuite {
  test("make sure IO computes the right result") {
    IO.pure(1).map(_ + 2).flatMap { result =>
      IO(assertEquals(result, 3))
    }
  }
}