package org.aurora.sjsast.meet.combined

trait JoinMeet[T]:
  def join(a: T, b: T): T
  def meet(a: T, b: T): T

object JoinMeet:
  given JoinMeet[Int] with
    def join(a: Int, b: Int): Int = a + b
    def meet(a: Int, b: Int): Int = math.min(a, b)

  given JoinMeet[Boolean] with
    def join(a: Boolean, b: Boolean): Boolean = a || b
    def meet(a: Boolean, b: Boolean): Boolean = a && b

  given [T]: JoinMeet[Set[T]] with
    def join(a: Set[T], b: Set[T]): Set[T] = a union b
    def meet(a: Set[T], b: Set[T]): Set[T] = a intersect b
