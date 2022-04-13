package ru.ekuzmichev

import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Scanner

object ReadFilesWays extends App {
  val filePath = getClass.getClassLoader.getResource("sample.html").getPath

  println(filePath)

  // version #1 - Java way
  println("============#1============")

  val file    = new File(filePath)
  val scanner = new Scanner(file)
  while (scanner.hasNextLine) {
    val line = scanner.nextLine()
    println(line)
  }

  println()

  // version #2 - Java way  with cheats (Apache commons)
  println("============#2============")

  val fileContents: java.util.List[String] = FileUtils.readLines(file, UTF_8)
  fileContents.forEach(println)

  println()

  // version #3 - Scala way
  println("============#3============")

  import scala.io.Source
  // required when using reflection, like `using` does
  import scala.language.reflectiveCalls

  def using[A <: { def close(): Unit }, B](resource: A)(f: A => B): B =
    try {
      f(resource)
    } finally {
      resource.close()
    }

  // Here is iterator and it is not fully loaded into memory as previous version
  using[Source, Unit](Source.fromFile(file))(_.getLines().foreach(println))

  println()
}
