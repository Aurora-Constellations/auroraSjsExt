package org.aurora.sjsast.meet.separate

import org.aurora.sjsast.*

@FunctionalInterface
// separate trait keeps meet as its own sam
trait Meet[T]:
  def meet(a: T, b: T): T

object Meet:

  extension [T](a: T)(using instance: Meet[T])
    def |&|(b: T): T = instance.meet(a, b)

  // intersects values that share the same logical key
  private def intersectBy[T, K](
      a: LHSet[T],
      b: LHSet[T],
      getKey: T => K
  )(using instance: Meet[T]): LHSet[T] =
    val rightByKey = LHMap[K, T]()

    b.foreach(item => rightByKey(getKey(item)) = item)

    val intersection = LHMap[K, T]()

    a.foreach { left =>
      val key = getKey(left)

      rightByKey.get(key).foreach { right =>
        intersection(key) = instance.meet(left, right)
      }
    }
    LHSet.from(intersection.values)

  // basic values
  given Meet[String] = (a, b) => if a == b then a else ""

  given Meet[Int] = (a, b) => math.min(a, b)

  given Meet[Double] = (a, b) => math.min(a, b)

  given Meet[Boolean] = (a, b) => a && b

  given Meet[Value] = (a, b) => if a == b then a else IncompleteValue

  given Meet[SingleValueUnit] = (a, b) =>
    if a == b then a
    else throw IllegalArgumentException("meet requires identical value units")

  // immutable collections
  given [T]: Meet[Set[T]] = (a, b) => a intersect b

  given [T]: Meet[List[T]] = (a, b) => a.filter(b.contains).distinct

  given [T](using instance: Meet[T]): Meet[Option[T]] =
    case (Some(a), Some(b)) => Some(instance.meet(a, b))
    case _                  => None

  given [K, V](using instance: Meet[V]): Meet[Map[K, V]] = (a, b) =>
    a.iterator.flatMap { (key, leftValue) =>
      b.get(key).map { rightValue =>
        key -> instance.meet(leftValue, rightValue)
      }
    }.toMap

  // aurora linked collections
  given [T]: Meet[LHSet[T]] = (a, b) => LHSet.from(a.filter(b.contains))

  given [K, V](using instance: Meet[V]): Meet[LHMap[K, V]] = (a, b) =>
    val intersection = LHMap[K, V]()
    a.foreach { (key, leftValue) =>
      b.get(key).foreach { rightValue =>
        intersection(key) = instance.meet(leftValue, rightValue)
      }
    }
    intersection

  // qualifier values
  given Meet[QU] = (a, b) => QU(LHSet.from(a.query.filter(b.query.contains)))

  given Meet[QuReference] = (a, b) =>
    if a.refName == b.refName then
      QuReference(
        qu = summon[Meet[QU]].meet(a.qu, b.qu),
        refName = a.refName
      )
    else throw IllegalArgumentException("meet requires references with the same name")

  given meetQuReferences: Meet[QuReferences] = (a, b) => QuReferences(intersectBy(a.qurc, b.qurc, _.refName))

  // Todo figure out sets of qu refs and QUs
  // todo:    figure out named coordinates and groups
  // Todo: figure out mixed coordinates and CIO maps
  // Todo  : look into magnolia for the rest of the aurora model
