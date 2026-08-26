package org.aurora.sjsast.meet.separate

import magnolia1.*
import org.aurora.sjsast.*

@FunctionalInterface
// separate trait keeps meet as its own sam
trait Meet[T]:
  def meet(a: T, b: T): T

object Meet extends AutoDerivation[Meet]:

  extension [T](a: T)(using instance: Meet[T]) def |&|(b: T): T = instance.meet(a, b)

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

  // keeps reference-coordinate categories separate when names match
  private def refCoordinateKey(coordinate: RefCoordinate): (String, String) =
    val coordinateType = coordinate match
      case _: ClinicalItem    => "clinical item"
      case _: IssueCoordinate => "issue coordinate"
      case _: OrderCoordinate => "order coordinate"

    coordinateType -> coordinate.name

  // checks whether two sections have the same variant
  private def sameCioVariant(a: CIO, b: CIO): Boolean =
    (a, b) match
      case (_: Clinical, _: Clinical) => true
      case (_: Issues, _: Issues)     => true
      case (_: Orders, _: Orders)     => true
      case _                          => false

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

  // parsed reference fields contain one wrapper
  given meetQuReferenceSets: Meet[LHSet[QuReferences]] = (a, b) =>
    val left = QuReferences(LHSet.from(a.iterator.flatMap(_.qurc)))
    val right = QuReferences(LHSet.from(b.iterator.flatMap(_.qurc)))
    val intersection = summon[Meet[QuReferences]].meet(left, right)

    if intersection.qurc.isEmpty then LHSet()
    else LHSet(intersection)

  given meetQUSets: Meet[LHSet[QU]] = (a, b) => LHSet.from(a.filter(b.contains))

  // named coordinates
  given meetOrderCoordinates: Meet[LHSet[OrderCoordinate]] = (a, b) => intersectBy(a, b, _.name)

  given meetIssueCoordinates: Meet[LHSet[IssueCoordinate]] = (a, b) => intersectBy(a, b, _.name)

  given meetClinicalItems: Meet[LHSet[ClinicalItem]] = (a, b) => intersectBy(a, b, _.name)

  // named groups
  given meetNGOs: Meet[LHSet[NGO]] = (a, b) => intersectBy(a, b, _.name)

  given meetNGCs: Meet[LHSet[NGC]] = (a, b) => intersectBy(a, b, _.name)

  // mixed coordinate collections
  given meetRefCoordinates: Meet[LHSet[RefCoordinate]] = (a, b) => intersectBy(a, b, refCoordinateKey)

  // section maps omit keys whose section variants disagree
  given meetCioMaps: Meet[LHMap[String, CIO]] = (a, b) =>
    val intersection = LHMap[String, CIO]()
    val cioMeet = summon[Meet[CIO]]

    a.foreach { (key, leftSection) =>
      b.get(key).foreach { rightSection =>
        if sameCioVariant(leftSection, rightSection) then intersection(key) = cioMeet.meet(leftSection, rightSection)
      }
    }
    intersection

  // magnolia calls this hook join for case class derivation
  def join[T](ctx: CaseClass[Meet, T]): Meet[T] = (a, b) =>
    ctx.construct { param =>
      param.typeclass.meet(param.deref(a), param.deref(b))
    }

  // sealed values can only meet when their variants match
  def split[T](ctx: SealedTrait[Meet, T]): Meet[T] = (a, b) =>
    ctx.choose(a) { sub =>
      if sub.cast.isDefinedAt(b) then sub.typeclass.meet(sub.value, sub.cast(b))
      else throw IllegalArgumentException("meet requires values of the same variant")
    }
