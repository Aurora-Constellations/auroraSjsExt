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
  
  // --- Generic Collections (Fallback) ---
  given [T](using s: Show[T]): Show[Option[T]] = 
    case Some(v) => s.show(v)
    case None => ""

  given [T](using s: Show[T]): Show[LHSet[T]] = 
    _.map(s.show).mkString(", ")

  given [K, V](using sk: Show[K], sv: Show[V]): Show[LHMap[K, V]] = 
    _.map { (k, v) => s"${sk.show(k)}: ${sv.show(v)}" }.mkString("\n")

  // --- 1. Basic AST nodes ---
  
  // Just print the punctuation/text, e.g. "??q4h;"
  given Show[NL_STATEMENT] = _.name 

  // Individual reference format: "!chf"
  given Show[QuReference] = r => 
    val symbol = if (r.qu == "undefined" || r.qu == "null") "" else r.qu
    s"$symbol${r.refName}"

  given Show[QuReferences] = q =>
    if (q.refs.isEmpty) "" 
    else "(" + q.refs.map(r => summon[Show[QuReference]].show(r)).mkString(", ") + ")"  

  // --- 2. Coordinates (Items) ---
  
  // Coordinate format: "!iv"
  given Show[OrderCoordinate] = o =>
    val refs = o.refs.show
    val narr = if (o.narratives.isEmpty) "" else s" ${o.narratives.map(_.show).mkString(" ")}"
    // Format: NAS(?ahah, !fsdnf)
    s"${o.name}$refs$narr"

  given Show[IssueCoordinate] = i =>
    val refs = if i.refs.refs.isEmpty then "" else s" ${i.refs.show}"
    val narr = if i.narratives.isEmpty then "" else s" ${i.narratives.map(_.show).mkString(" ")}"
    s"${i.name}$refs$narr"

  given Show[ClinicalCoordinate] = c =>
    val refs = if c.refs.refs.isEmpty then "" else s" ${c.refs.show}"
    val narr = if c.narratives.isEmpty then "" else s" ${c.narratives.map(_.show).mkString(" ")}"
    s"${c.name}$refs$narr"

  // --- 3. Groups ---

  // Format: "GroupName: narrative \n  Order1 \n  Order2"
  given Show[NGO] = g =>
    val leadQus = g.qu.map(_.query).mkString("")
    val narr = if g.narratives.isEmpty then "" else s" ${g.narratives.map(_.show).mkString(" ")}"
    val orders = g.orders.map(_.show).mkString("\n  ")
    s"$leadQus${g.name}$narr\n  $orders"

  given Show[NGC] = g =>
    val narr = if g.narratives.isEmpty then "" else s" ${g.narratives.map(_.show).mkString(" ")}"
    val coords = g.coordinates.map(_.show).mkString("\n  ")
    s"${g.name}$narr\n  $coords"

  // --- 4. Sections (CIO) ---
  
  given Show[Orders] = o =>
    val narr = if o.narratives.isEmpty then "" else o.narratives.map(_.show).mkString("\n") + "\n"
    val groups = o.namedGroups.map(summon[Show[NGO]].show).mkString("\n\n")
    s"${o.name}:\n$narr$groups"

  given Show[Clinical] = c =>
    val narr = if c.narratives.isEmpty then "" else c.narratives.map(_.show).mkString("\n") + "\n"
    val groups = c.namedGroups.map(summon[Show[NGC]].show).mkString("\n\n")
    s"${c.name}:\n$narr$groups"

  given Show[Issues] = i =>
    val narr = if i.narratives.isEmpty then "" else i.narratives.map(_.show).mkString("\n") + "\n"
    val coords = i.coordinates.map(summon[Show[IssueCoordinate]].show).mkString("\n")
    s"${i.name}:\n$narr$coords"

  // Dispatch for the sealed trait
  given Show[CIO] = 
    case o: Orders => o.show
    case c: Clinical => c.show
    case i: Issues => i.show

  // --- 5. Modules ---
  
  given Show[ModulePCM] = m =>
    val sections = m.cio.values.map(_.show).mkString("\n\n")
    s"module: ${m.name}\n\n$sections"

  given Show[PCM] = p =>
    p.modules.values.map(_.show).mkString("\n\n")

  // --- Magnolia Derivation Hooks (Fallback for unknown types) ---
  def join[T](ctx: CaseClass[Show, T]): Show[T] = t =>
    val params = ctx.params.map { param =>
      s"${param.label}=${param.typeclass.show(param.deref(t))}"
    }
    s"${ctx.typeInfo.short}(${params.mkString(", ")})"

  def split[T](ctx: SealedTrait[Show, T]): Show[T] = t =>
    ctx.choose(t) { sub => sub.typeclass.show(sub.value) }