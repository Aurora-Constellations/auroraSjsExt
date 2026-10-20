package org.aurora.meetdemo

import org.aurora.sjsast.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.scalajs.js

class PcmEvaluatorTest extends AnyWordSpec with Matchers:
  private def item(name: String, value: Int): ClinicalItem = ClinicalItem(name, values = List(SingleValueUnit(IntValue(value), "_")))
  private def clinical(group: String, items: ClinicalItem*): PCM =
    PCM(LHMap("Clinical" -> Clinical(ngc = LHSet(NGC(group, coordinates = LHSet(items*))))))
  private def field(report: js.Dynamic, name: String): js.Dynamic = report.selectDynamic(name).asInstanceOf[js.Dynamic]
  private def validation(reference: PCM, designer: PCM): js.Dynamic = field(PcmEvaluator.evaluate(reference, designer, "validate"), "validation")

  "PCM demo evaluator" should {
    "preserve a bare reference against valued designer fields and ignore additions" in {
      val reference = clinical("Neurologic:", ClinicalItem("gcs_eye"), ClinicalItem("gcs_verbal"), ClinicalItem("gcs_motor"))
      val designer = clinical("Neurologic:", item("gcs_eye", 4), item("gcs_verbal", 2), item("gcs_motor", 6), ClinicalItem("pupil_response"))
      val check = validation(reference, designer)
      check.valid.asInstanceOf[Boolean] shouldBe true
      check.matched.asInstanceOf[Int] shouldBe 3
      check.required.asInstanceOf[Int] shouldBe 3
    }

    "require the same group path and every specified value" in {
      val reference = clinical("Neurologic:", item("gcs_eye", 2))
      validation(reference, clinical("Observation:", item("gcs_eye", 2))).valid.asInstanceOf[Boolean] shouldBe false
      val changed = validation(reference, clinical("Neurologic:", item("gcs_eye", 4)))
      changed.valid.asInstanceOf[Boolean] shouldBe false
      changed.matched.asInstanceOf[Int] shouldBe 0
      changed.missing.asInstanceOf[js.Array[String]].toList.head should include("gcs_eye")
    }

    "require reference group and section annotations even when all item names match" in {
      val reference = PCM(LHMap("Clinical" -> Clinical(
        narratives = LHSet(NL_STATEMENT("-- required section note;")),
        ngc = LHSet(NGC("Neurologic:", narratives = LHSet(NL_STATEMENT("-- required group note;")), coordinates = LHSet(ClinicalItem("gcs_eye"))))
      )))
      val check = validation(reference, clinical("Neurologic:", item("gcs_eye", 2)))
      check.valid.asInstanceOf[Boolean] shouldBe false
      check.matched.asInstanceOf[Int] shouldBe 1
      check.missing.asInstanceOf[js.Array[String]].length shouldBe 2
    }

    "preserve qualifier and reference constraints while warning about formatter omissions" in {
      val requiredItem = ClinicalItem("gcs_eye", qu = QU(LHSet('!')))
      val reference = PCM(LHMap("Clinical" -> Clinical(ngc = LHSet(NGC(
        "Neurologic:", coordinates = LHSet(requiredItem),
        refs = LHSet(QuReferences(LHSet(QuReference(refName = "gcs_eye"))))
      )))))
      val report = PcmEvaluator.evaluate(reference, reference, "validate")
      report.validation.valid.asInstanceOf[Boolean] shouldBe true
      report.warnings.asInstanceOf[js.Array[String]].length shouldBe 2
      validation(reference, clinical("Neurologic:", ClinicalItem("gcs_eye"))).valid.asInstanceOf[Boolean] shouldBe false
    }

    "derive GCS from facts composed across two inputs" in {
      val report = PcmEvaluator.evaluate(
        clinical("Neurologic:", item("gcs_eye", 2), item("gcs_verbal", 2)),
        clinical("Neurologic:", item("gcs_motor", 3)), "score"
      )
      val gcs = report.scores.asInstanceOf[js.Array[js.Dynamic]](0)
      gcs.status.asInstanceOf[String] shouldBe "ready"
      gcs.total.asInstanceOf[Int] shouldBe 7
      gcs.band.asInstanceOf[String] shouldBe "severe"
      gcs.source.asInstanceOf[String] shouldBe "derived"
      report.result.items.asInstanceOf[js.Array[js.Dynamic]].exists(_.origin.asInstanceOf[String] == "derived") shouldBe true
    }

    "report missing scoring inputs without inventing a total" in {
      val report = PcmEvaluator.evaluate(clinical("Neurologic:", item("gcs_eye", 4)), PCM(), "score")
      val gcs = report.scores.asInstanceOf[js.Array[js.Dynamic]](0)
      gcs.status.asInstanceOf[String] shouldBe "incomplete"
      gcs.total shouldBe null
      gcs.issues.asInstanceOf[js.Array[String]].exists(_.contains("gcs_motor")) shouldBe true
    }

    "mark refreshed scoring outputs as derived while preserving unrelated source origins" in {
      val source = PCM(LHMap(
        "Clinical" -> Clinical(ngc = LHSet(
          NGC("Neurologic:", coordinates = LHSet(item("gcs_eye", 2), item("gcs_verbal", 2), item("gcs_motor", 3))),
          NGC("Scores:", coordinates = LHSet(item("gcs_total", 7), item("local_metric", 3)))
        )),
        "Issues" -> Issues(ic = LHSet(
          IssueCoordinate("score_gcs_severe", fromMods = List("gcs_severe")),
          IssueCoordinate("source_issue")
        ))
      ))
      def resultItems(right: PCM): Map[String, js.Dynamic] =
        PcmEvaluator.evaluate(source, right, "score").result.items.asInstanceOf[js.Array[js.Dynamic]]
          .toList.map(row => row.name.asInstanceOf[String] -> row).toMap
      // A scoring refresh remains derived even if the previous total and issue
      // already contain exactly the values that the scorer will produce.
      val unchangedValues = resultItems(PCM())
      unchangedValues("gcs_total").detail.asInstanceOf[String] shouldBe "gcs_total [7 _]"
      unchangedValues("gcs_total").origin.asInstanceOf[String] shouldBe "derived"
      unchangedValues("score_gcs_severe").origin.asInstanceOf[String] shouldBe "derived"
      unchangedValues("local_metric").origin.asInstanceOf[String] shouldBe "left"
      unchangedValues("source_issue").origin.asInstanceOf[String] shouldBe "left"
      val conflictingValues = resultItems(clinical("Scores:", item("gcs_total", 99), item("local_metric", 6)))
      conflictingValues("gcs_total").detail.asInstanceOf[String] shouldBe "gcs_total [7 _]"
      conflictingValues("gcs_total").origin.asInstanceOf[String] shouldBe "derived"
      conflictingValues("local_metric").detail.asInstanceOf[String] shouldBe "local_metric [3 _, 6 _]"
      conflictingValues("local_metric").origin.asInstanceOf[String] shouldBe "both"
    }

    "retain conflicting values in join and intersect them in meet" in {
      val left = clinical("Neurologic:", item("gcs_eye", 2))
      val right = clinical("Neurologic:", item("gcs_eye", 4))
      val joined = PcmEvaluator.evaluate(left, right, "join")
      joined.result.dsl.asInstanceOf[String] should include("[2 _, 4 _]")
      val met = PcmEvaluator.evaluate(left, right, "meet")
      met.result.items.asInstanceOf[js.Array[js.Dynamic]](0).detail.asInstanceOf[String] shouldBe "gcs_eye"
      met.counts.common.asInstanceOf[Int] shouldBe 1
      val scored = PcmEvaluator.evaluate(left, right, "score")
      scored.warnings.asInstanceOf[js.Array[String]].exists(_.contains("first value")) shouldBe true
    }

    "warn when scoring aggregates same-name values across different groups" in {
      val report = PcmEvaluator.evaluate(
        clinical("Neurologic:", item("gcs_eye", 2), item("gcs_verbal", 2), item("gcs_motor", 3)),
        clinical("Observation:", item("gcs_eye", 4)), "score"
      )
      report.scores.asInstanceOf[js.Array[js.Dynamic]](0).total.asInstanceOf[Int] shouldBe 7
      report.warnings.asInstanceOf[js.Array[String]].exists(_.contains("first value")) shouldBe true
    }

    "explain joined group order when a right fact precedes a left fact in another group" in {
      val left = PCM(LHMap("Clinical" -> Clinical(ngc = LHSet(
        NGC("Observation:", coordinates = LHSet(item("temperature", 37))),
        NGC("Neurologic:", coordinates = LHSet(item("gcs_eye", 2), item("gcs_verbal", 2), item("gcs_motor", 3)))
      ))))
      val report = PcmEvaluator.evaluate(left, clinical("Observation:", item("gcs_eye", 4)), "score")
      report.scores.asInstanceOf[js.Array[js.Dynamic]](0).total.asInstanceOf[Int] shouldBe 9
      val warning = report.warnings.asInstanceOf[js.Array[String]].find(_.contains("multiple values")).get
      warning should include("joined group and item order")
      warning should include("same-name facts in different groups")
      warning should not include "left input order comes first"
    }

    "explain current qualified-issue scoring semantics only during scoring" in {
      val issues = PCM(LHMap("Issues" -> Issues(ic = LHSet(IssueCoordinate("atrial_fibrillation", qu = QU(LHSet('?')))))))
      val scored = PcmEvaluator.evaluate(PCM(), issues, "score")
      scored.warnings.asInstanceOf[js.Array[String]].exists(_.contains("regardless of their qualifier markers")) shouldBe true
      val joined = PcmEvaluator.evaluate(PCM(), issues, "join")
      joined.warnings.asInstanceOf[js.Array[String]].exists(_.contains("regardless of their qualifier markers")) shouldBe false
    }

    "warn about fractional inputs and negative markers without changing the actual scorer" in {
      val fractionalEye = ClinicalItem("gcs_eye", values = List(SingleValueUnit(DoubleValue(3.9), "_", negative = true)))
      val report = PcmEvaluator.evaluate(clinical("Neurologic:", fractionalEye, item("gcs_verbal", 2), item("gcs_motor", 3)), PCM(), "score")
      report.scores.asInstanceOf[js.Array[js.Dynamic]](0).total.asInstanceOf[Int] shouldBe 8
      val warnings = report.warnings.asInstanceOf[js.Array[String]].toList
      warnings.exists(_.contains("truncates")) shouldBe true
      warnings.exists(_.contains("(-) marker")) shouldBe true
    }

    "distinguish full recomposition from an omitted section" in {
      val left = clinical("Vitals:", item("temperature", 37))
      val right = PCM(LHMap("Orders" -> Orders(ngo = LHSet(NGO("Care:", ordercoord = LHSet(OrderCoordinate("observations")))))))
      PcmEvaluator.evaluate(left, right, "decompose").decomposition.recomposed.asInstanceOf[Boolean] shouldBe true
      val projected = PcmEvaluator.evaluate(left, right, "decompose", List("Clinical"))
      projected.decomposition.recomposed.asInstanceOf[Boolean] shouldBe false
      projected.result.sections.asInstanceOf[js.Array[String]].toList shouldBe List("Clinical")
      PcmEvaluator.evaluate(left, right, "decompose", Nil).counts.result.asInstanceOf[Int] shouldBe 0
      PcmEvaluator.evaluate(left, right, "meet").counts.result.asInstanceOf[Int] shouldBe 0
    }

    "handle empty reference coverage and serialize every field as native JSON" in {
      val report = PcmEvaluator.evaluate(PCM(), PCM(), "validate")
      report.validation.valid.asInstanceOf[Boolean] shouldBe true
      val serialized = js.JSON.stringify(report)
      serialized should include("\"required\":0")
      serialized should include("\"scores\":[]")
      serialized should include("\"decomposition\":null")
    }

    "reject unsupported operations and section selections" in {
      an[IllegalArgumentException] should be thrownBy PcmEvaluator.evaluate(PCM(), PCM(), "subtract")
      an[IllegalArgumentException] should be thrownBy PcmEvaluator.evaluate(PCM(), PCM(), "decompose", List("Unknown"))
    }
  }
