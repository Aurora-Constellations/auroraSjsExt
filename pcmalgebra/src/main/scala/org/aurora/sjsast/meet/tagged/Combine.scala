package org.aurora.sjsast.meet.tagged

enum Operation:
  case Join, Meet

trait Combine[T]:
  def combine(operation: Operation, a: T, b: T): T

object Combine:
  given Combine[Int] = (operation, a, b) =>
    operation match
      case Operation.Join => a + b
      case Operation.Meet => math.min(a, b)

  given Combine[Boolean] = (operation, a, b) =>
    operation match
      case Operation.Join => a || b
      case Operation.Meet => a && b

  given [T]: Combine[Set[T]] = (operation, a, b) =>
    operation match
      case Operation.Join => a union b
      case Operation.Meet => a intersect b
