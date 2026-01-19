package org.aurora.sjsast

import magnolia1._
import scala.collection.mutable.{LinkedHashSet, LinkedHashMap}

trait JoinMeet[T]:
  def join(a: T, b: T): T

object JoinMeet extends AutoDerivation[JoinMeet]:

  // --- Helpers for Merging Named Items ---
  // Merges two sets by name, recursively joining items with the same name
  private def mergeNamedSets[T](a: LHSet[T], b: LHSet[T], getName: T => String)(using jm: JoinMeet[T]): LHSet[T] =
    val merged = LinkedHashMap.empty[String, T]
    a.foreach { item => merged(getName(item)) = item }
    b.foreach { item =>
      val name = getName(item)
      if (merged.contains(name)) then
        merged(name) = jm.join(merged(name), item)
      else
        merged(name) = item
    }
    LinkedHashSet.from(merged.values)

  // --- Basic Types ---
  given JoinMeet[String] = (a, b) => 
    if (a == b) then a 
    else if (a.isEmpty) then b 
    else if (b.isEmpty) then a 
    else s"$a; $b"
    
  given JoinMeet[Int] = _ + _
  given JoinMeet[Boolean] = _ || _

  // --- Collections ---
  given [T](using jm: JoinMeet[T]): JoinMeet[Option[T]] = 
    case (Some(a), Some(b)) => Some(jm.join(a, b))
    case (a, None) => a
    case (None, b) => b

  given [K, V](using jm: JoinMeet[V]): JoinMeet[LHMap[K, V]] = (a, b) =>
    val res = a.clone()
    b.foreach { (k, v) =>
      if (res.contains(k)) then res(k) = jm.join(res(k), v)
      else res(k) = v
    }
    res

  // Default Set behavior: Union (no deduplication by name)
  given [T]: JoinMeet[LHSet[T]] = (a, b) => a ++ b

  // --- SPECIFIC MERGE STRATEGIES ---
  
  // QuReferences: Union of all references
  given joinQuRefs: JoinMeet[QuReferences] = (a, b) =>
    QuReferences(a.refs ++ b.refs)

  // Coordinates: Merge by name (items with same name are recursively joined)
  given joinOrderCoords: JoinMeet[LHSet[OrderCoordinate]] = (a, b) =>
    mergeNamedSets(a, b, _.name)

  given joinIssueCoords: JoinMeet[LHSet[IssueCoordinate]] = (a, b) =>
    mergeNamedSets(a, b, _.name)

  given joinClinicalCoords: JoinMeet[LHSet[ClinicalCoordinate]] = (a, b) =>
    mergeNamedSets(a, b, _.name)

  // Named Groups: Merge by name (groups with same name are recursively joined)
  given joinNGOs: JoinMeet[LHSet[NGO]] = (a, b) =>
    mergeNamedSets(a, b, _.name)

  given joinNGCs: JoinMeet[LHSet[NGC]] = (a, b) =>
    mergeNamedSets(a, b, _.name)

  // --- Magnolia Auto-Derivation ---
  
  def join[T](ctx: CaseClass[JoinMeet, T]): JoinMeet[T] = (a, b) =>
    ctx.construct { param =>
      param.typeclass.join(param.deref(a), param.deref(b))
    }

  def split[T](ctx: SealedTrait[JoinMeet, T]): JoinMeet[T] = (a, b) =>
    ctx.choose(a) { sub =>
      if (sub.cast.isDefinedAt(b)) then
        sub.typeclass.join(sub.value, sub.cast(b))
      else
        a 
    }