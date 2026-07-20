package org.aurora.sjsast.meet.separate

// separate trait keeps meet as its own sam
trait Meet[T]:
  def meet(a: T, b: T): T

// each given can use a lambda because meet is a sam
object Meet:
  given Meet[Int] = (a, b) => math.min(a, b)
  given Meet[Boolean] = (a, b) => a && b
  given [T]: Meet[Set[T]] = (a, b) => a intersect b
