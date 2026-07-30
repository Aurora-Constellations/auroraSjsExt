package org.aurora.sjsast.meet.separate

import org.aurora.sjsast.*

@FunctionalInterface
trait Meet[T]:
  def meet(a: T, b: T): T

object Meet:

  extension [T](a: T)(using instance: Meet[T])
    def |&|(b: T): T = instance.meet(a, b)

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
      QuReference(qu = summon[Meet[QU]].meet(a.qu, b.qu), refName = a.refName)
    else throw IllegalArgumentException("meet requires references with the same name")

  given meetQuReferences: Meet[QuReferences] = (a, b) =>
    QuReferences(intersectBy(a.qurc, b.qurc, _.refName))

  // ---- sets of qu refs and QUs ----

  private def mergeQuSet(set: LHSet[QU]): QU =
    QU(set.foldLeft(LHSet.empty[Char])((acc, qu) => acc ++ qu.query))

  given meetQuSet: Meet[LHSet[QU]] = (a, b) =>
    val merged = summon[Meet[QU]].meet(mergeQuSet(a), mergeQuSet(b))
    if merged.query.isEmpty then LHSet.empty else LHSet(merged)

  private def mergeQuReferencesSet(set: LHSet[QuReferences]): QuReferences =
    QuReferences(LHSet.from(set.flatMap(_.qurc)))

  given meetQuReferencesSet: Meet[LHSet[QuReferences]] = (a, b) =>
    val merged = meetQuReferences.meet(mergeQuReferencesSet(a), mergeQuReferencesSet(b))
    if merged.qurc.isEmpty then LHSet.empty else LHSet(merged)

  // ---- named coordinates and groups ----

  given meetClinicalCoordinate: Meet[ClinicalCoordinate] = (a, b) =>
    if a.name != b.name then throw IllegalArgumentException("meet requires coordinates with the same name")
    ClinicalCoordinate(
      name = a.name,
      narratives = summon[Meet[LHSet[NL_STATEMENT]]].meet(a.narratives, b.narratives),
      qurefs = meetQuReferencesSet.meet(a.qurefs, b.qurefs),
      qu = summon[Meet[QU]].meet(a.qu, b.qu)
    )

  given meetClinicalValue: Meet[ClinicalValue] = (a, b) =>
    if a.name != b.name then throw IllegalArgumentException("meet requires values with the same name")
    ClinicalValue(
      name = a.name,
      values = summon[Meet[List[SingleValueUnit]]].meet(a.values, b.values),
      narrative = summon[Meet[LHSet[NL_STATEMENT]]].meet(a.narrative, b.narrative),
      qurefs = meetQuReferencesSet.meet(a.qurefs, b.qurefs)
    )

  given meetIssueCoordinate: Meet[IssueCoordinate] = (a, b) =>
    if a.name != b.name then throw IllegalArgumentException("meet requires issues with the same name")
    IssueCoordinate(
      name = a.name,
      fromMods = summon[Meet[List[String]]].meet(a.fromMods, b.fromMods),
      narratives = summon[Meet[LHSet[NL_STATEMENT]]].meet(a.narratives, b.narratives),
      qurefs = meetQuReferencesSet.meet(a.qurefs, b.qurefs),
      qu = summon[Meet[QU]].meet(a.qu, b.qu)
    )

  given meetOrderCoordinate: Meet[OrderCoordinate] = (a, b) =>
    if a.name != b.name then throw IllegalArgumentException("meet requires orders with the same name")
    OrderCoordinate(
      name = a.name,
      narratives = summon[Meet[LHSet[NL_STATEMENT]]].meet(a.narratives, b.narratives),
      qurefs = meetQuReferencesSet.meet(a.qurefs, b.qurefs)
    )

  given meetRefCoordinate: Meet[RefCoordinate] = (a, b) =>
    (a, b) match
      case (x: ClinicalCoordinate, y: ClinicalCoordinate) => meetClinicalCoordinate.meet(x, y)
      case (x: ClinicalValue, y: ClinicalValue)           => meetClinicalValue.meet(x, y)
      case (x: IssueCoordinate, y: IssueCoordinate)       => meetIssueCoordinate.meet(x, y)
      case (x: OrderCoordinate, y: OrderCoordinate)       => meetOrderCoordinate.meet(x, y)
      case _ =>
        throw IllegalArgumentException(
          s"meet requires coordinates of the same subtype and name: ${a.name} / ${b.name}"
        )

  given meetRefCoordinateSet: Meet[LHSet[RefCoordinate]] = (a, b) =>
    intersectBy(a, b, _.name)

  given meetNGC: Meet[NGC] = (a, b) =>
    if a.name != b.name then throw IllegalArgumentException("meet requires groups with the same name")
    NGC(
      name = a.name,
      narratives = summon[Meet[LHSet[NL_STATEMENT]]].meet(a.narratives, b.narratives),
      coordinates = meetRefCoordinateSet.meet(a.coordinates, b.coordinates),
      refs = meetQuReferencesSet.meet(a.refs, b.refs)
    )

  given meetNGO: Meet[NGO] = (a, b) =>
    if a.name != b.name then throw IllegalArgumentException("meet requires groups with the same name")
    NGO(
      name = a.name,
      narratives = summon[Meet[LHSet[NL_STATEMENT]]].meet(a.narratives, b.narratives),
      ordercoord = intersectBy(a.ordercoord, b.ordercoord, _.name),
      qurefs = meetQuReferencesSet.meet(a.qurefs, b.qurefs),
      qu = meetQuSet.meet(a.qu, b.qu)
    )

  given meetNGCSet: Meet[LHSet[NGC]] = (a, b) => intersectBy(a, b, _.name)
  given meetNGOSet: Meet[LHSet[NGO]] = (a, b) => intersectBy(a, b, _.name)

  // Todo: figure out mixed coordinates and CIO maps
  // Todo: look into magnolia for the rest of the aurora model