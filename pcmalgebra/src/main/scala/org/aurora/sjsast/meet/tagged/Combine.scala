package org.aurora.sjsast.meet.tagged

// operation tag identifies join or meet
enum Operation:
  case Join, Meet

// one abstract method preserves the sam requirement
trait Combine[T]:
  def combine(operation: Operation, a: T, b: T): T

// each lambda handles both operations
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
