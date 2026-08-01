package org.aurora.sjsast

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import scala.collection.mutable.LinkedHashSet

class ParametricModelingTest extends AnyWordSpec with Matchers {

    "ParametricModeling.applyAgeConstraint" should {

        "return the PCM unchanged if age is defined" in {
            val ageValue = ClinicalItem(
                name = "age",
                values = List(SingleValueUnit(IntValue(25), "yr"))
            )
            val ngc = NGC(name = "Demographics:", coordinates = LHSet(ageValue))
            val clinical = Clinical(ngc = LHSet(ngc))
            val pcm = PCM(cio = LHMap("Clinical" -> clinical))

            val result = ParametricModeling.applyAgeConstraint(pcm)

            result shouldBe pcm
            // Updated casting to ClinicalItem[cite: 3]
            val resultAge = result.cio("Clinical").asInstanceOf[Clinical]
                .ngc.head.coordinates.head.asInstanceOf[ClinicalItem]
            
            resultAge.values.head.value shouldBe IntValue(25)
        }

        "inject age as '???' if the age coordinate is missing from Clinical section" in {
            val weightValue = ClinicalItem(
                name = "weight",
                values = List(SingleValueUnit(IntValue(70), "kg"))
            )
            val ngc = NGC(name = "Demographics:", coordinates = LHSet(weightValue))
            val clinical = Clinical(ngc = LHSet(ngc))
            val pcm = PCM(cio = LHMap("Clinical" -> clinical))

            val result = ParametricModeling.applyAgeConstraint(pcm)

            val resultClinical = result.cio("Clinical").asInstanceOf[Clinical]
            val coords = resultClinical.ngc.head.coordinates
            
            coords.exists(_.name == "age") shouldBe true
            val ageCoord = coords.find(_.name == "age").get.asInstanceOf[ClinicalItem]
            ageCoord.values.head.value shouldBe StringValue("???")
        }

        "create a Clinical section with placeholder age if Clinical is entirely missing" in {
            val pcm = PCM(cio = LHMap("Issues" -> Issues(name = "Issues:")))

            val result = ParametricModeling.applyAgeConstraint(pcm)

            result.cio.contains("Clinical") shouldBe true
            val resultClinical = result.cio("Clinical").asInstanceOf[Clinical]
            val ageCoord = resultClinical.ngc.head.coordinates.head.asInstanceOf[ClinicalItem]
            
            ageCoord.name shouldBe "age"
            ageCoord.values.head.value shouldBe StringValue("???")
        }
    }
}