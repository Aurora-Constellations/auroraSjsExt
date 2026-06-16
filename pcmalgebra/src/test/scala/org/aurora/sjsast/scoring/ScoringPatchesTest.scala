// package org.aurora.sjsast.scoring

// import org.aurora.sjsast.*
// import org.scalatest.matchers.should.Matchers
// import org.scalatest.wordspec.AnyWordSpec

// class ScoringPatchesTest extends AnyWordSpec with Matchers:

//   "ScoringPatches" should {
//     "compile as a safe join-based scoring skeleton" in {
//       val pcm = PCM(
//         LHMap(
//           "Clinical" -> Clinical(
//             ngc = LHSet(
//               NGC(
//                 name = "Neurologic:",
//                 coordinates = LHSet(
//                   clinicalValue("gcs_eye", 2),
//                   clinicalValue("gcs_verbal", 2),
//                   clinicalValue("gcs_motor", 3)
//                 )
//               )
//             )
//           )
//         )
//       )

//       val scored = ScoringPatches.applyDerivedScores(pcm)

//       scored shouldBe pcm
//     }
//   }

//   private def clinicalValue(name: String, value: Int): RefCoordinate =
//     ClinicalValue(
//       name = name,
//       values = List(SingleValueUnit(IntValue(value), ScoringConstants.PlaceholderUnit))
//     )
