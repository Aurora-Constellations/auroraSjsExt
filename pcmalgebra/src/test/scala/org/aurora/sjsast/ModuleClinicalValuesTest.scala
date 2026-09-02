package org.aurora.sjsast

class ModuleClinicalValuesTest extends BaseAsyncTest:

  "Module conversion" should {
    "preserve clinical item values used by downstream scoring" in {
      parse(0).map { ast =>
        val module = Module(ast)
        val values = module.cio
          .get("Clinical")
          .collect { case clinical: Clinical => clinical }
          .flatMap(_.ngc.find(_.name == "Facts:"))
          .flatMap(_.coordinates.find(_.name == "age"))
          .map(_.values)

        values shouldBe Some(List(SingleValueUnit(IntValue(76), "years")))
      }
    }
  }
