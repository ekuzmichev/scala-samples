package ru.ekuzmichev

object ByValueByNameMethods extends App {
  val externalFlag = true

  def memoryInfo(): String = "Used Memory: " + (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory)

  def outerFn(x: => String): Unit = {
    println(s"Start outerFn: ${memoryInfo()}")
    innerFn(x)
    println(s"End outerFn: ${memoryInfo()}")
  }

  def innerFn(x: => String): Unit = {
    println(s"Start innerFn: ${memoryInfo()}")
    if (externalFlag) println(s"Got ${x.length} symbols string")
    println(s"End innerFn: ${memoryInfo()}")
  }

  outerFn(List.fill(100000)("a").mkString)
}
