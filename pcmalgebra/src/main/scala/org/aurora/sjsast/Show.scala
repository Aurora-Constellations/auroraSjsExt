package org.aurora.sjsast

import magnolia1._
import scala.collection.mutable.LinkedHashSet
import scala.collection.mutable.LinkedHashMap

trait Show[T]:
  def show(t: T): String

object Show extends AutoDerivation[Show]:
  
  extension [T](t: T)(using s: Show[T])
    def show: String = s.show(t)

  // --- Primitives ---
  given Show[String] = (t: String) => t
  given Show[Int] = _.toString
  given Show[Boolean] = _.toString
  given Show[Char] = _.toString
  
  // --- Generic Collections (Fallback) ---
  given [T](using s: Show[T]): Show[Option[T]] = 
    case Some(v) => s.show(v)
    case None => ""

  given [T](using s: Show[T]): Show[LHSet[T]] = 
    _.map(s.show).mkString("")

  given [K, V](using sk: Show[K], sv: Show[V]): Show[LHMap[K, V]] = 
    _.map { (k, v) => s"${sk.show(k)}: ${sv.show(v)}" }.mkString("\n")

  // --- 1. Basic AST nodes ---
  
  given Show[NL_STATEMENT] = _.name
  
  given Show[QU] = qu => qu.query.mkString("")

  // Individual reference format: "!chf" or "?chf"
  given Show[QuReference] = r => s"${r.qu.show}${r.refName}"

  given Show[QuReferences] = q =>
    if (q.refs.isEmpty) "" 
    else "(" + q.refs.map(_.show).mkString(", ") + ")"

  // --- 2. Coordinates (Items) ---
  
  given Show[OrderCoordinate] = o =>
    val refs = o.refs.show
    val narr = if (o.narratives.isEmpty) "" else s" ${o.narratives.map(_.show).mkString(" ")}"
    s"${o.name}$refs$narr"

  given Show[IssueCoordinate] = i =>
    val refs = if (i.refs.refs.isEmpty) "" else s" ${i.refs.show}"
    val narr = if (i.narratives.isEmpty) "" else s" ${i.narratives.map(_.show).mkString(" ")}"
    s"${i.name}$refs$narr"

  given Show[ClinicalCoordinate] = c =>
    val refs = if (c.refs.refs.isEmpty) "" else s" ${c.refs.show}"
    val narr = if (c.narratives.isEmpty) "" else s" ${c.narratives.map(_.show).mkString(" ")}"
    s"${c.name}$refs$narr"

  // --- 3. Groups ---

  given Show[NGO] = g =>
    val leadQus = g.qu.map(_.show).mkString("")
    val narr = if (g.narratives.isEmpty) "" else s" ${g.narratives.map(_.show).mkString(" ")}"
    val orders = g.orders.map(_.show).mkString("\n  ")
    s"$leadQus${g.name}$narr\n  $orders"

  given Show[NGC] = g =>
    val narr = if (g.narratives.isEmpty) "" else s" ${g.narratives.map(_.show).mkString(" ")}"
    val coords = g.coordinates.map(_.show).mkString("\n  ")
    s"${g.name}$narr\n  $coords"

  // --- 4. Sections (CIO) ---
  
  given Show[Orders] = o =>
    val narr = if (o.narratives.isEmpty) "" else o.narratives.map(_.show).mkString("\n") + "\n"
    val groups = o.namedGroups.map(_.show).mkString("\n\n")
    s"${o.name}:\n$narr$groups"

  given Show[Clinical] = c =>
    val narr = if (c.narratives.isEmpty) "" else c.narratives.map(_.show).mkString("\n") + "\n"
    val groups = c.namedGroups.map(_.show).mkString("\n\n")
    s"${c.name}:\n$narr$groups"

  given Show[Issues] = i =>
    val narr = if (i.narratives.isEmpty) "" else i.narratives.map(_.show).mkString("\n") + "\n"
    val coords = i.coordinates.map(_.show).mkString("\n")
    s"${i.name}:\n$narr$coords"

  // Dispatch for the sealed trait
  given Show[CIO] = 
    case o: Orders => o.show
    case c: Clinical => c.show
    case i: Issues => i.show

  // --- 5. Modules ---
  
  given Show[Module] = m =>
    val sections = m.cio.values.map(_.show).mkString("\n\n")
    s"module: ${m.name}\n\n$sections"

  given Show[PCM] = p =>
    p.cio.values.map(_.show).mkString("\n\n")

  // --- Magnolia Derivation Hooks (Fallback for unknown types) ---
  def join[T](ctx: CaseClass[Show, T]): Show[T] = t =>
    val params = ctx.params.map { param =>
      s"${param.label}=${param.typeclass.show(param.deref(t))}"
    }
    s"${ctx.typeInfo.short}(${params.mkString(", ")})"

  def split[T](ctx: SealedTrait[Show, T]): Show[T] = t =>
    ctx.choose(t) { sub => sub.typeclass.show(sub.value) }