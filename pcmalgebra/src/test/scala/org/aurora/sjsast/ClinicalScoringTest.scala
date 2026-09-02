package org.aurora.sjsast

import org.aurora.sjsast.scoring.ScoreWriteback
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ClinicalScoringTest extends AnyWordSpec with Matchers:

  "ClinicalScoring" should {
    "derive a severe adult GCS score and its module import" in {
      val update = ClinicalScoring(
        pcmWithGroups(
          NGC(
            name = "Neurologic:",
            coordinates = LHSet(
              intItem("gcs_eye", 2),
              intItem("gcs_verbal", 2),
              intItem("gcs_motor", 3)
            )
          )
        )
      )

      scoreValue(update.pcm, "gcs_total") shouldBe Some(IntValue(7))
      scoreValue(update.pcm, "gcs_total_source") shouldBe Some(StringValue("derived"))
      scoreValue(update.pcm, "gcs_severity") shouldBe Some(StringValue("severe"))
      derivedIssue(update.pcm, "score_gcs_severe").map(_.fromMods) shouldBe
        Some(List("gcs_severe"))
      update.derivedModuleImports shouldBe Map("gcs_severe" -> "score_gcs_severe")
    }

    "derive CHA2DS2-VASc from demographics and active issues" in {
      val pcm = pcmWithGroupsAndIssues(
        Seq(
          NGC(
            name = "Demographics:",
            coordinates = LHSet(
              intItem("age", 76, "yr"),
              textItem("sex", "female"),
              textItem("cha2ds2_vasc_diabetes", "absent"),
              textItem("prior_stroke_tia_te", "absent"),
              textItem("vascular_disease", "absent")
            )
          )
        ),
        List("atrial_fibrillation", "heart_failure", "hypertension")
      )

      val update = ClinicalScoring(pcm)

      scoreValue(update.pcm, "cha2ds2_vasc_total") shouldBe Some(IntValue(5))
      scoreValue(update.pcm, "cha2ds2_vasc_risk_band") shouldBe Some(StringValue("high"))
      derivedIssue(update.pcm, "score_af_stroke_risk_high").map(_.fromMods) shouldBe
        Some(List("af_stroke_risk_high"))
      update.derivedModuleImports shouldBe
        Map("af_stroke_risk_high" -> "score_af_stroke_risk_high")
    }

    "prefer explicit negative AF risk factors over issue-derived positives" in {
      val pcm = pcmWithGroupsAndIssues(
        Seq(
          NGC(
            name = "Demographics:",
            coordinates = LHSet(
              intItem("age", 70, "yr"),
              textItem("sex", "male"),
              textItem("cha2ds2_vasc_heart_failure", "absent"),
              textItem("cha2ds2_vasc_hypertension", "absent"),
              textItem("cha2ds2_vasc_diabetes", "absent"),
              textItem("prior_stroke_tia_te", "absent"),
              textItem("vascular_disease", "absent")
            )
          )
        ),
        List("atrial_fibrillation", "hypertension")
      )

      val update = ClinicalScoring(pcm)

      scoreValue(update.pcm, "cha2ds2_vasc_total") shouldBe Some(IntValue(1))
      scoreValue(update.pcm, "cha2ds2_vasc_risk_band") shouldBe
        Some(StringValue("intermediate"))
      derivedIssue(update.pcm, "score_af_stroke_risk_high") shouldBe None
      derivedIssue(update.pcm, "score_af_stroke_risk_intermediate").map(_.fromMods) shouldBe
        Some(List("af_stroke_risk_intermediate"))
    }

    "replace stale GCS values and issues when source facts change" in {
      val source = NGC(
        name = "Neurologic:",
        coordinates = LHSet(
          intItem("gcs_eye", 2),
          intItem("gcs_verbal", 2),
          intItem("gcs_motor", 3)
        )
      )
      val initial = pcmWithGroupsAndIssues(
        Seq(
          source,
          NGC(
            name = "Scores:",
            coordinates = LHSet(textItem("custom_score", "keep_me"))
          )
        ),
        List("score_custom")
      )
      val first = ClinicalScoring(initial).pcm
      val changed = replaceGroup(
        first,
        "Neurologic:",
        source.copy(
          coordinates = LHSet(
            intItem("gcs_eye", 4),
            intItem("gcs_verbal", 5),
            intItem("gcs_motor", 6)
          )
        )
      )

      val refreshed = ClinicalScoring(changed)

      scoreValues(refreshed.pcm, "gcs_total") shouldBe List(IntValue(15))
      scoreValues(refreshed.pcm, "gcs_severity") shouldBe List(StringValue("mild"))
      scoreValue(refreshed.pcm, "gcs_status") shouldBe None
      scoreValue(refreshed.pcm, "custom_score") shouldBe Some(StringValue("keep_me"))
      derivedIssue(refreshed.pcm, "score_gcs_severe") shouldBe None
      derivedIssue(refreshed.pcm, "score_custom") shouldBe defined
      refreshed.derivedModuleImports shouldBe empty
      scoresGroupCount(refreshed.pcm) shouldBe 1
    }

    "remove a prior total, severity, source, and issue when GCS becomes not testable" in {
      val source = NGC(
        name = "Neurologic:",
        coordinates = LHSet(
          intItem("gcs_eye", 2),
          intItem("gcs_verbal", 2),
          intItem("gcs_motor", 3)
        )
      )
      val first = ClinicalScoring(pcmWithGroups(source)).pcm
      val changed = replaceGroup(
        first,
        "Neurologic:",
        source.copy(
          coordinates = LHSet(
            intItem("gcs_eye", 2),
            textItem("gcs_verbal", "NT"),
            intItem("gcs_motor", 5)
          )
        )
      )

      val refreshed = ClinicalScoring(changed).pcm

      scoreValue(refreshed, "gcs_total") shouldBe None
      scoreValue(refreshed, "gcs_total_source") shouldBe None
      scoreValue(refreshed, "gcs_severity") shouldBe None
      scoreValue(refreshed, "gcs_status") shouldBe Some(StringValue("not_testable"))
      derivedIssue(refreshed, "score_gcs_severe") shouldBe None
    }

    "clear owned outputs when their source facts disappear" in {
      val scored = ClinicalScoring(
        pcmWithGroups(
          NGC(
            name = "Neurologic:",
            coordinates = LHSet(
              intItem("gcs_eye", 3),
              intItem("gcs_verbal", 4),
              intItem("gcs_motor", 4)
            )
          )
        )
      ).pcm
      val withoutFacts = removeGroup(scored, "Neurologic:")

      val refreshed = ClinicalScoring(withoutFacts).pcm

      scoreValue(refreshed, "gcs_total") shouldBe None
      scoreValue(refreshed, "gcs_total_source") shouldBe None
      scoreValue(refreshed, "gcs_severity") shouldBe None
      derivedIssue(refreshed, "score_gcs_moderate") shouldBe None
      scoresGroupCount(refreshed) shouldBe 0
    }

    "be idempotent across repeated scoring passes" in {
      val pcm = pcmWithGroups(
        NGC(
          name = "Neurologic:",
          coordinates = LHSet(
            intItem("gcs_eye", 4),
            intItem("gcs_verbal", 4),
            intItem("gcs_motor", 5)
          )
        )
      )

      val once = ClinicalScoring(pcm)
      val twice = ClinicalScoring(once.pcm)

      once shouldBe twice
      scoresGroupCount(twice.pcm) shouldBe 1
    }
  }

  private def pcmWithGroups(groups: NGC*): PCM =
    pcmWithGroupsAndIssues(groups, Nil)

  private def pcmWithGroupsAndIssues(groups: Seq[NGC], issues: List[String]): PCM =
    PCM(
      cio = LHMap(
        "Clinical" -> Clinical(ngc = LHSet.from(groups)),
        "Issues" -> Issues(ic = LHSet.from(issues.map(IssueCoordinate(_))))
      )
    )

  private def intItem(name: String, value: Int, unit: String = "_"): ClinicalItem =
    ClinicalItem(name = name, values = List(SingleValueUnit(IntValue(value), unit)))

  private def textItem(name: String, value: String): ClinicalItem =
    ClinicalItem(name = name, values = List(SingleValueUnit(StringValue(value), "_")))

  private def scoreValue(pcm: PCM, name: String): Option[Value] =
    ScoreWriteback.scoreValue(pcm, name)

  private def scoreValues(pcm: PCM, name: String): List[Value] =
    pcm.cio
      .get("Clinical")
      .collect { case clinical: Clinical => clinical }
      .toList
      .flatMap(_.ngc.filter(_.name == "Scores:"))
      .flatMap(_.coordinates.filter(_.name == name))
      .flatMap(_.values.map(_.value))

  private def derivedIssue(pcm: PCM, name: String): Option[IssueCoordinate] =
    pcm.cio
      .get("Issues")
      .collect { case issues: Issues => issues }
      .flatMap(_.ic.find(_.name == name))

  private def scoresGroupCount(pcm: PCM): Int =
    pcm.cio
      .get("Clinical")
      .collect { case clinical: Clinical => clinical.ngc.count(_.name == "Scores:") }
      .getOrElse(0)

  private def replaceGroup(pcm: PCM, name: String, replacement: NGC): PCM =
    updateClinicalGroups(pcm)(_.map(group => if group.name == name then replacement else group))

  private def removeGroup(pcm: PCM, name: String): PCM =
    updateClinicalGroups(pcm)(_.filterNot(_.name == name))

  private def updateClinicalGroups(pcm: PCM)(update: List[NGC] => List[NGC]): PCM =
    val clinical = pcm.cio("Clinical").asInstanceOf[Clinical]
    val updatedClinical = clinical.copy(ngc = LHSet.from(update(clinical.ngc.toList)))
    val cio = LHMap.from(pcm.cio)
    cio.update("Clinical", updatedClinical)
    pcm.copy(cio = cio)
