package org.aurora.sjsast.scoring.gcs

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GcsEnumTest extends AnyWordSpec with Matchers:

  "GcsEnum" should {
    "carry scores and descriptions for each GCS component" in {
      Eye.Spontaneous.score.shouldBe(4)
      Eye.Spontaneous.description.shouldBe("eyes open spontaneously")

      Verbal.Oriented.score.shouldBe(5)
      Verbal.Oriented.description.shouldBe("oriented verbal response")

      Motor.ObeysCommands.score.shouldBe(6)
      Motor.ObeysCommands.description.shouldBe("obeys commands")
    }

    "represent component resolution as a typed state" in {
      val resolved: ComponentResolution[Eye] =
        ComponentResolution.Resolved(Eye.ToVoice)

      resolved.shouldBe(ComponentResolution.Resolved(Eye.ToVoice))
      ComponentResolution.Missing.shouldBe(ComponentResolution.Missing)
      ComponentResolution.NotTestable.shouldBe(ComponentResolution.NotTestable)
    }

    "carry stable output values for GCS outputs" in {
      GcsSeverity.Severe.outputValue.shouldBe("severe")
      GcsStatus.NotTestable.outputValue.shouldBe("not_testable")
      GcsTotalSource.Derived.outputValue.shouldBe("derived")
    }

    "find Eye condition by score" in {
      Eye.findByScore(2).shouldBe(Some(Eye.ToPain))
      Eye.findByScore(10).shouldBe(None)
    }

    "parse Eye component inputs" in {
      Eye.parse("spontaneous").shouldBe(Some(Eye.Spontaneous))
      Eye.parse("to voice").shouldBe(Some(Eye.ToVoice))
      Eye.parse("TO_PAIN").shouldBe(Some(Eye.ToPain))
      Eye.parse("no-eye-opening").shouldBe(Some(Eye.None))
      Eye.parse("unknown").shouldBe(None)
    }

    "retain legacy Eye component aliases" in {
      List("opens spontaneously").foreach { input =>
        Eye.parse(input).shouldBe(Some(Eye.Spontaneous))
      }
      List("to sound", "to verbal command", "verbal command").foreach { input =>
        Eye.parse(input).shouldBe(Some(Eye.ToVoice))
      }
      Eye.parse("to pressure").shouldBe(Some(Eye.ToPain))
    }

    "resolve Eye component inputs as typed component resolution" in {
      Eye.resolveInput("2").shouldBe(ComponentResolution.Resolved(Eye.ToPain))
      Eye.resolveInput("to voice").shouldBe(ComponentResolution.Resolved(Eye.ToVoice))
      Eye.resolveInput("NT").shouldBe(ComponentResolution.NotTestable)
      Eye.resolveInput("not testable").shouldBe(ComponentResolution.NotTestable)
      Eye.resolveInput("10").shouldBe(ComponentResolution.Missing)
      Eye.resolveInput("unknown").shouldBe(ComponentResolution.Missing)
    }

    "find Verbal condition by score" in {
      Verbal.findByScore(3).shouldBe(Some(Verbal.Words))
      Verbal.findByScore(7).shouldBe(None)
    }

    "parse Verbal component inputs" in {
      Verbal.parse("oriented").shouldBe(Some(Verbal.Oriented))
      Verbal.parse("CONFUSED").shouldBe(Some(Verbal.Confused))
      Verbal.parse("inappropriate words").shouldBe(Some(Verbal.Words))
      Verbal.parse("incomprehensible-sounds").shouldBe(Some(Verbal.Sounds))
      Verbal.parse("no_verbal_response").shouldBe(Some(Verbal.None))
      Verbal.parse("unknown").shouldBe(None)
    }

    "retain the legacy British-English Verbal alias" in {
      Verbal.parse("orientated").shouldBe(Some(Verbal.Oriented))
    }

    "resolve Verbal component inputs as typed component resolution" in {
      Verbal.resolveInput("3").shouldBe(ComponentResolution.Resolved(Verbal.Words))
      Verbal.resolveInput("confused").shouldBe(ComponentResolution.Resolved(Verbal.Confused))
      Verbal.resolveInput("NT").shouldBe(ComponentResolution.NotTestable)
      Verbal.resolveInput("not testable").shouldBe(ComponentResolution.NotTestable)
      Verbal.resolveInput("7").shouldBe(ComponentResolution.Missing)
      Verbal.resolveInput("unknown").shouldBe(ComponentResolution.Missing)
    }

    "find Motor condition by score" in {
      Motor.findByScore(4).shouldBe(Some(Motor.WithdrawsFromPain))
      Motor.findByScore(10).shouldBe(None)
    }

    "parse Motor component inputs" in {
      Motor.parse("obeys commands").shouldBe(Some(Motor.ObeysCommands))
      Motor.parse("LOCALIZES_PAIN").shouldBe(Some(Motor.LocalizesPain))
      Motor.parse("withdraws").shouldBe(Some(Motor.WithdrawsFromPain))
      Motor.parse("abnormal-flexion").shouldBe(Some(Motor.AbnormalFlexion))
      Motor.parse("extension").shouldBe(Some(Motor.Extension))
      Motor.parse("no_motor_response").shouldBe(Some(Motor.None))
      Motor.parse("unknown").shouldBe(None)
    }

    "retain legacy Motor component aliases" in {
      List("localising", "localizing", "localises").foreach { input =>
        Motor.parse(input).shouldBe(Some(Motor.LocalizesPain))
      }
      List("normal flexion", "withdrawal from pain").foreach { input =>
        Motor.parse(input).shouldBe(Some(Motor.WithdrawsFromPain))
      }
    }

    "resolve Motor component inputs as typed component resolution" in {
      Motor.resolveInput("4").shouldBe(ComponentResolution.Resolved(Motor.WithdrawsFromPain))
      Motor.resolveInput("obeys commands").shouldBe(ComponentResolution.Resolved(Motor.ObeysCommands))
      Motor.resolveInput("NT").shouldBe(ComponentResolution.NotTestable)
      Motor.resolveInput("not testable").shouldBe(ComponentResolution.NotTestable)
      Motor.resolveInput("10").shouldBe(ComponentResolution.Missing)
      Motor.resolveInput("unknown").shouldBe(ComponentResolution.Missing)
    }

    "derive severity from a valid GCS total" in {
      GcsSeverity.fromTotal(3).shouldBe(Some(GcsSeverity.Severe))
      GcsSeverity.fromTotal(8).shouldBe(Some(GcsSeverity.Severe))
      GcsSeverity.fromTotal(9).shouldBe(Some(GcsSeverity.Moderate))
      GcsSeverity.fromTotal(12).shouldBe(Some(GcsSeverity.Moderate))
      GcsSeverity.fromTotal(13).shouldBe(Some(GcsSeverity.Mild))
      GcsSeverity.fromTotal(15).shouldBe(Some(GcsSeverity.Mild))
    }

    "reject invalid GCS totals when deriving severity" in {
      GcsSeverity.fromTotal(2).shouldBe(None)
      GcsSeverity.fromTotal(16).shouldBe(None)
    }

    "parse GCS severity output values" in {
      GcsSeverity.fromOutputValue("severe").shouldBe(Some(GcsSeverity.Severe))
      GcsSeverity.fromOutputValue("MODERATE").shouldBe(Some(GcsSeverity.Moderate))
      GcsSeverity.fromOutputValue("mild").shouldBe(Some(GcsSeverity.Mild))
      GcsSeverity.fromOutputValue("unknown").shouldBe(None)
    }

    "parse GCS status output values" in {
      GcsStatus.fromOutputValue("not_testable").shouldBe(Some(GcsStatus.NotTestable))
      GcsStatus.fromOutputValue("not testable").shouldBe(Some(GcsStatus.NotTestable))
      GcsStatus.fromOutputValue("INCOMPLETE").shouldBe(Some(GcsStatus.Incomplete))
      GcsStatus.fromOutputValue("unknown").shouldBe(None)
    }

    "parse GCS total source output values" in {
      GcsTotalSource.fromOutputValue("derived").shouldBe(Some(GcsTotalSource.Derived))
      GcsTotalSource.fromOutputValue("MANUAL").shouldBe(Some(GcsTotalSource.Manual))
      GcsTotalSource.fromOutputValue("unknown").shouldBe(None)
    }

  }
