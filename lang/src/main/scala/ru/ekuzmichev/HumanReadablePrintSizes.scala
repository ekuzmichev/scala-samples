package ru.ekuzmichev

import com.jakewharton.byteunits.BinaryByteUnit.format

import java.lang.Runtime.getRuntime

object HumanReadablePrintSizes extends App {
  println(format(123456))
  println(format(getRuntime.totalMemory() - getRuntime.freeMemory()))
  println(format(getRuntime.freeMemory()))
  println(format(getRuntime.totalMemory()))
  println(format(getRuntime.maxMemory()))
}
// Sample output:
// 120.6 KiB
// 23.1 MiB
// 222.4 MiB
// 245.5 MiB
// 3.6 GiB
