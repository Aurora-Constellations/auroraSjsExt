# Clinical Scoring In This Branch

This document is meant to help someone pick up the clinical scoring work in this branch without having to reverse-engineer the flow from tests and build files.

At a high level, the scoring pipeline does four things:

1. read facts from a patient PCM
2. calculate supported scores from those facts
3. write the derived score values back into the PCM
4. pull in follow-up Aurora modules when a derived score band should trigger guidance

The scope is intentionally small right now.

- Implemented:
  - `GCS_Adult`
  - `CHA2DS2_VASc`

## Where The Code Actually Lives

The core scoring logic is now centered in `pcmalgebra`.

- Public entrypoints:
  - `pcmalgebra/src/main/scala/org/aurora/sjsast/ClinicalScoring.scala`
  - `pcmalgebra/src/main/scala/org/aurora/sjsast/ClinicalScoringConsoleSummary.scala`
- Internal implementation:
  - `pcmalgebra/src/main/scala/org/aurora/sjsast/scoring/`
- Bundled score-driven modules:
  - `pcmalgebra/score-modules/`
- CLI:
  - `pcmalgebra/cli/src/main/scala/com/axiom/scorecli/`

The extension-side orchestration is still at the repo root:

- `src/main/scala/MergePCM/MergePCM.scala`

That split is deliberate. `pcmalgebra` owns the scoring logic and score-specific support code. The root project only owns VS Code / merge orchestration.

## Runtime Flow

The easiest way to understand the runtime behavior is to follow the data in order.

1. Parse the current Aurora PCM.
2. Merge any local issue-driven modules already referenced by the PCM.
3. Run `ParametricModeling.applyAgeConstraint`.
4. Run `ClinicalScoring.apply`.
5. Write score values into `Clinical -> Scores:`.
6. Add derived `score_*` issues into `Issues`.
7. Resolve any score-driven modules.
   - local sibling `.aurora` file first
   - bundled module under `pcmalgebra/score-modules` second
8. Merge those score modules into the final DSL output.

That is the full loop used by the root merge flow.

## How The Scorer Is Structured

The scorer is split by responsibility so you can read or change one part without carrying the whole feature in your head.

- `scoring/ClinicalScoring.scala`
  - top-level scorer orchestration
- `scoring/ClinicalFacts.scala`
  - extracts normalized inputs from `Clinical` and active issue names from `Issues`
- `scoring/Parsing.scala`
  - shared name normalization and primitive value parsing
- `scoring/gcs/GcsScorer.scala`
  - GCS logic only
- `scoring/af/AfScorer.scala`
  - CHA2DS2-VASc logic only
- `scoring/ScoreWriteback.scala`
  - writes scores and derived issues back into the PCM
- `scoring/ScoreModuleResolver.scala`
  - resolves local or bundled score modules
- `scoring/summary/ScoreSummary.scala`
  - machine-readable score summary used by the CLI

## What Inputs The Scorer Looks For

### GCS

The GCS scorer looks for:

- eye response
- verbal response
- motor response
- optional manual total

It accepts both numeric and common text forms.

Examples:

- `gcs_eye [2 _]`
- `gcs_verbal [confused _]`
- `gcs_motor [withdraws _]`
- `gcs_total [7 _]`

Important rule:

- if any GCS component is `NT` or `not_testable`, the scorer does not emit a total or severity, even if a manual total exists

### CHA2DS2-VASc

The AF scorer only runs if AF is present.

After that, it requires explicit values for:

- age
- sex
- heart failure
- hypertension
- diabetes
- prior stroke / TIA / thromboembolism
- vascular disease

Those values can come from either:

- explicit `Clinical` values
- or active `Issues` as positive evidence

Important rule:

- missing negative risk factors are not silently treated as `false`
- if AF is present but the required inputs are incomplete, the result status becomes `insufficient_data`

## What The Scorer Writes Back

The scorer writes two kinds of output.

### Score values

These go into a `Scores:` named group under `Clinical`.

Current fields include:

- `gcs_total`
- `gcs_total_source`
- `gcs_severity`
- `gcs_status`
- `cha2ds2_vasc_total`
- `cha2ds2_vasc_risk_band`
- `cha2ds2_vasc_status`

### Derived issues

These go into `Issues`.

Current score-driven issue names are:

- `score_gcs_severe`
- `score_gcs_moderate`
- `score_af_stroke_risk_high`
- `score_af_stroke_risk_intermediate`

Those derived issues are what drive the score-module merge step.

## Bundled Score Modules

Bundled score modules now live under `pcmalgebra/score-modules`.

At the moment they provide simple follow-up guidance for:

- severe GCS
- moderate GCS
- high AF stroke risk
- intermediate AF stroke risk

The resolver is intentionally conservative:

- if a local module exists beside the patient PCM, use that
- otherwise use the bundled default

That gives local authors an override path without having to edit the library-owned defaults.

## CLI Usage

From the repo root:

```bash
npm run score-pcm -- pcmalgebra/cli/src/test/resources/scorepcmcli/gcs-severe.aurora
```

Other fixtures:

```bash
npm run score-pcm -- pcmalgebra/cli/src/test/resources/scorepcmcli/af-high.aurora
npm run score-pcm -- pcmalgebra/cli/src/test/resources/scorepcmcli/af-insufficient.aurora
```

The CLI prints a JSON summary of the currently supported scores.

## Test Commands

From the repo root:

```bash
sbt "pcmalgebra/testOnly org.aurora.sjsast.ClinicalScoringTest"
sbt "scorepcmcli/testOnly com.axiom.scorecli.ScorePcmCliTest"
sbt "testOnly com.axiom.MergePCM.MergePCMClinicalScoringTest"
```

If you want the main scoring-related suites in one run:

```bash
sbt "pcmalgebra/testOnly org.aurora.sjsast.ClinicalScoringTest; scorepcmcli/testOnly com.axiom.scorecli.ScorePcmCliTest; testOnly com.axiom.MergePCM.MergePCMClinicalScoringTest"
```

## If You Want To Read The Code In Order

If your goal is to understand the flow rather than jump straight into one scorer, this is the order that makes the most sense:

1. `src/main/scala/MergePCM/MergePCM.scala`
2. `pcmalgebra/src/main/scala/org/aurora/sjsast/ClinicalScoring.scala`
3. `pcmalgebra/src/main/scala/org/aurora/sjsast/scoring/ClinicalScoring.scala`
4. `pcmalgebra/src/main/scala/org/aurora/sjsast/scoring/ClinicalFacts.scala`
5. `pcmalgebra/src/main/scala/org/aurora/sjsast/scoring/Parsing.scala`
6. `pcmalgebra/src/main/scala/org/aurora/sjsast/scoring/gcs/GcsScorer.scala`
7. `pcmalgebra/src/main/scala/org/aurora/sjsast/scoring/af/AfScorer.scala`
8. `pcmalgebra/src/main/scala/org/aurora/sjsast/scoring/ScoreWriteback.scala`
9. `pcmalgebra/src/main/scala/org/aurora/sjsast/scoring/ScoreModuleResolver.scala`
10. `pcmalgebra/src/main/scala/org/aurora/sjsast/scoring/summary/ScoreSummary.scala`

Then read the tests. They are the best compact description of what the code is expected to do.

## Dependency

This repo uses the published `aurora-langium@0.4.0` package.

If you want to check the installed version from the repo root:

```bash
npm ls aurora-langium --depth=0
```
