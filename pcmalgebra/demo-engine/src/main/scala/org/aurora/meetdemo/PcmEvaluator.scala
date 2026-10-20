package org.aurora.meetdemo

import org.aurora.sjsast.*
import org.aurora.sjsast.JoinMeet.*
import org.aurora.sjsast.meet.separate.Meet.*
import org.aurora.sjsast.Show.*
import org.aurora.sjsast.scoring.{ClinicalFacts, ClinicalScoring, ScoringPrecheck, ScoringScoreCheck}
import org.aurora.sjsast.scoring.summary.ScoreSummary
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

/** Browser presentation adapter. All operations delegate to the PCM algebra. */
object PcmEvaluator:
  private val sectionNames = List("Clinical", "Issues", "Orders")
  private val operationNames = Set("join", "meet", "validate", "decompose", "score")

  @JSExportTopLevel("evaluateAst")
  def evaluateAst(left: js.Any, right: js.Any, operation: String, selectedSections: js.Array[String]): js.Any =
    evaluate(fromAst(left), fromAst(right), operation, selectedSections.toList)

  private def fromAst(ast: js.Any): PCM =
    val root = ast.asInstanceOf[js.Dynamic]
    if root.selectDynamic("$type").asInstanceOf[String] != "PCM" then
      throw IllegalArgumentException("Expected an Aurora PCM syntax tree")
    val imports = root.selectDynamic("imports")
    if !js.isUndefined(imports) && imports.asInstanceOf[js.Array[js.Any]].nonEmpty then
      throw IllegalArgumentException("Resolve imports before evaluating this browser PCM")
    if !js.isUndefined(root.selectDynamic("module")) || !js.isUndefined(root.selectDynamic("template")) then
      throw IllegalArgumentException("Expand modules and templates into PCM sections before evaluating")
    root.selectDynamic("elements").asInstanceOf[js.Array[js.Dynamic]].foreach { element =>
      if !sectionNames.contains(element.selectDynamic("$type").asInstanceOf[String]) then
        throw IllegalArgumentException("Only Clinical, Issues, and Orders sections are supported; arithmetic is not evaluated")
    }
    PCM(ast.asInstanceOf[GenAst.PCM])

  /** Also used by Scala tests, without a second parser or algebra implementation. */
  def evaluate(left: PCM, right: PCM, operation: String, selectedSections: List[String] = sectionNames): js.Dynamic =
    require(operationNames.contains(operation), s"Unknown operation: $operation")
    require(selectedSections.forall(sectionNames.contains), "Select only Clinical, Issues, and Orders sections")
    val leftRows = rows(left)
    val rightRows = rows(right)
    val leftIds = leftRows.map(_.id).toSet
    val rightIds = rightRows.map(_.id).toSet
    val common = leftIds intersect rightIds
    val overlap = if operation == "meet" || operation == "validate" then left |&| right else PCM()
    val joined = if operation == "join" || operation == "decompose" || operation == "score" then left |+| right else PCM()
    val parts =
      if operation == "decompose" then selectedSections.distinct.flatMap { key => joined.cio.get(key).map(section => PCM(LHMap(key -> section))) }
      else Nil
    val recomposed = parts.foldLeft(PCM())(_ |+| _)
    val result = operation match
      case "meet" | "validate" => overlap
      case "score"              => ClinicalScoring(joined).pcm
      case "decompose"          => recomposed
      case _                    => joined
    val derivedIds = if operation == "score" then refreshedPaths(joined, result) else Set.empty[String]
    val validation: js.Any =
      if operation == "validate" then coverage(left, overlap)
      else null
    val decomposition: js.Any =
      if operation == "decompose" then
        js.Dynamic.literal(
          parts = js.Array(parts.map(part => model(part, leftIds, rightIds))*),
          recomposed = (recomposed == joined)
        )
      else null
    val scores = if operation == "score" then scoreReport(joined, result) else js.Array[js.Any]()
    js.Dynamic.literal(
      operation = operation,
      left = model(left, leftIds, Set.empty),
      right = model(right, Set.empty, rightIds),
      result = model(result, leftIds, rightIds, derivedIds),
      counts = js.Dynamic.literal(
        left = leftRows.size, right = rightRows.size, common = common.size,
        leftOnly = (leftIds -- rightIds).size, rightOnly = (rightIds -- leftIds).size,
        result = rows(result).size
      ),
      validation = validation,
      scores = scores,
      warnings = js.Array(exportWarnings(List(left, right, result), operation == "score", joined)*),
      decomposition = decomposition
    )

  private case class Row(section: String, group: String, node: RefCoordinate, detail: String):
    def id: String = s"$section/$group/${node.name}"

  private def rows(pcm: PCM): List[Row] =
    sectionNames.flatMap { key => pcm.cio.get(key).toList.flatMap {
      case c: Clinical => c.ngc.toList.flatMap(g => g.coordinates.toList.map(n => Row(key, g.name, n, n.show)))
      case i: Issues   => i.ic.toList.map(n => Row(key, "", n, n.show))
      case o: Orders   => o.ngo.toList.flatMap(g => g.ordercoord.toList.map(n => Row(key, g.name, n, n.show)))
    }}

  private def refreshedPaths(source: PCM, scored: PCM): Set[String] =
    val sourceNodes = rows(source).map(row => row.id -> row.node).toMap
    // ScoreWriteback preserves unowned coordinate instances and creates fresh
    // nodes for owned outputs, including refreshes whose values did not change.
    // This follows the scorer's result without duplicating its ownership rules.
    rows(scored).filterNot(row => sourceNodes.get(row.id).exists(_ eq row.node)).map(_.id).toSet

  private def model(pcm: PCM, leftIds: Set[String], rightIds: Set[String], derivedIds: Set[String] = Set.empty): js.Dynamic =
    val items = rows(pcm).map { row =>
      val origin =
        if derivedIds.contains(row.id) then "derived"
        else (leftIds.contains(row.id), rightIds.contains(row.id)) match
          case (true, true)   => "both"
          case (true, false)  => "left"
          case (false, true)  => "right"
          case (false, false) => "derived"
      js.Dynamic.literal(id = row.id, name = row.node.name, section = row.section, group = row.group, detail = row.detail, origin = origin)
    }
    js.Dynamic.literal(
      dsl = pcm.show,
      items = js.Array(items*),
      sections = js.Array(sectionNames.filter(pcm.cio.contains)*)
    )

  private def coverage(reference: PCM, overlap: PCM): js.Dynamic =
    val actualRows = rows(overlap).map(row => row.id -> row.node).toMap
    val requiredRows = rows(reference)
    val matched = requiredRows.count(row => actualRows.get(row.id).contains(row.node))
    val missing = collection.mutable.ListBuffer.empty[String]
    def itemChecks(section: String, group: String, required: Iterable[RefCoordinate], actual: Iterable[RefCoordinate]): Unit =
      val byName = actual.map(n => n.name -> n).toMap
      required.foreach { node =>
        val path = List(section, group, node.name).filter(_.nonEmpty).mkString(" / ")
        byName.get(node.name) match
          case None => missing += path
          case Some(found) if found != node => missing += s"$path (required values or annotations differ)"
          case _ => ()
      }
    reference.cio.foreach { (key, required) =>
      overlap.cio.get(key) match
        case None => missing += key
        case Some(actual) => (required, actual) match
          case (r: Clinical, a: Clinical) =>
            if r.copy(ngc = LHSet()) != a.copy(ngc = LHSet()) then missing += s"$key (section annotations differ)"
            r.ngc.foreach { group => a.ngc.find(_.name == group.name) match
              case None => missing += s"$key / ${group.name}"
              case Some(found) =>
                if group.copy(coordinates = LHSet()) != found.copy(coordinates = LHSet()) then missing += s"$key / ${group.name} (group annotations differ)"
                itemChecks(key, group.name, group.coordinates, found.coordinates)
            }
          case (r: Orders, a: Orders) =>
            if r.copy(ngo = LHSet()) != a.copy(ngo = LHSet()) then missing += s"$key (section annotations differ)"
            r.ngo.foreach { group => a.ngo.find(_.name == group.name) match
              case None => missing += s"$key / ${group.name}"
              case Some(found) =>
                if group.copy(ordercoord = LHSet()) != found.copy(ordercoord = LHSet()) then missing += s"$key / ${group.name} (group annotations differ)"
                itemChecks(key, group.name, group.ordercoord, found.ordercoord)
            }
          case (r: Issues, a: Issues) =>
            if r.copy(ic = LHSet()) != a.copy(ic = LHSet()) then missing += s"$key (section annotations differ)"
            itemChecks(key, "", r.ic, a.ic)
          case _ => missing += s"$key (section variant differs)"
    }
    val valid = overlap == reference
    val message =
      if valid then "The meet preserves the complete reference structure."
      else "The meet loses required reference structure. Check the missing paths or differing required annotations."
    js.Dynamic.literal(valid = valid, matched = matched, required = requiredRows.size, missing = js.Array(missing.toList*), message = message)

  private def scoreReport(source: PCM, scored: PCM): js.Array[js.Any] =
    val precheck = ScoringPrecheck.validate(source)
    val summary = ScoreSummary.fromScoredPcm(scored)
    def nullable[A](value: Option[A]): js.Any = value.fold[js.Any](null)(_.asInstanceOf[js.Any])
    def entry(check: ScoringScoreCheck, total: Option[Int], band: Option[String], source: Option[String]): js.Any =
      js.Dynamic.literal(
        name = check.score,
        status = check.status.label,
        total = nullable(total), band = nullable(band), source = nullable(source),
        issues = js.Array(check.issues.map(issue => s"${issue.field}: ${issue.problem.label}; expected ${issue.expected}")*)
      )
    js.Array(
      entry(precheck.gcs, summary.GCS_Adult.gcs_total, summary.GCS_Adult.gcs_severity, summary.GCS_Adult.gcs_total_source),
      entry(precheck.cha2ds2Vasc, summary.CHA2DS2_VASc.cha2ds2_vasc_total, summary.CHA2DS2_VASc.cha2ds2_vasc_risk_band, None)
    )

  private def exportWarnings(models: List[PCM], scoring: Boolean, source: PCM): List[String] =
    val sections = models.flatMap(_.cio.values)
    val clinicalGroups = sections.collect { case c: Clinical => c }.flatMap(_.ngc)
    val orderGroups = sections.collect { case o: Orders => o }.flatMap(_.ngo)
    val issueItems = sections.collect { case i: Issues => i }.flatMap(_.ic)
    val clinicalItems = clinicalGroups.flatMap(_.coordinates)
    val warnings = collection.mutable.ListBuffer.empty[String]
    if clinicalGroups.exists(_.refs.nonEmpty) || orderGroups.exists(_.qurefs.nonEmpty) then
      warnings += "The existing Aurora DSL formatter omits group references from displayed DSL. Algebra and validation still retain them."
    if clinicalItems.exists(_.qu.query.nonEmpty) || issueItems.exists(_.qu.query.nonEmpty) then
      warnings += "The existing Aurora DSL formatter omits leading clinical and issue item qualifiers from displayed DSL. Algebra and validation still retain them."
    if orderGroups.exists(group => !group.name.endsWith(":")) then
      warnings += "The existing Aurora DSL formatter does not add a colon to order group names; this output may need a colon before parsing again."
    if scoring then
      val facts = ClinicalFacts.from(source)
      val sourceValues = facts.values.values.flatten.toList
      if facts.values.values.exists(_.size > 1) then
        warnings += "A composed field has multiple values. Current scorers use the first value in joined group and item order, including same-name facts in different groups. Within a matching item path, left values precede distinct right values."
      if sourceValues.exists(_.value match
        case DoubleValue(value) => value % 1 != 0
        case _ => false
      ) then
        warnings += "Some source facts contain fractional numbers. Current scoring truncates numeric inputs when interpreting integer scoring fields."
      if sourceValues.exists(_.negative) then
        warnings += "Some source facts contain the (-) marker. Current scoring ignores this marker when interpreting source values."
    if scoring && source.cio.get("Issues").collect { case i: Issues => i.ic.exists(_.qu.query.nonEmpty) }.getOrElse(false) then
      warnings += "Current scoring interprets declared issue names regardless of their qualifier markers. Review qualified issues before interpreting derived scores."
    if issueItems.exists(_.fromMods.nonEmpty) then
      warnings += "Derived issue module references are displayed, but this demo does not load their order modules. Resolve those references before using this result as an input."
    warnings.toList
