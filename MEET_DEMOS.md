# PCM algebra and scoring workbench

The React and TypeScript workbench uses the Aurora grammar, PCM algebra, and scoring code:

| App | Local URL | UI source |
| --- | --- | --- |
| React | http://127.0.0.1:5173 | `meet-react/src/` |

The app contains its own `src/shared` browser sources, examples, styles, and `vendor/pcm-engine.js`. It imports generated grammar from its pinned `aurora-langium` 0.4.2 dependency. The bundled engine is compiled from the real Scala PCM algebra and scorers. The app runs independently of the root build.

Choose an example and operation, edit or import A and B, then press **Run**.
Changes clear the previous result. **Reset selected example** restores the selected inputs.
There is no automatic evaluation or saved browser state.

## Run

React needs Node 22.12+ (or 20.19+):

```sh
cd meet-react
npm ci
npm run dev
```

For a production build:

```sh
npm run build
```

To install, build, and run the app from the repository root:

```sh
npm run demos:setup
npm run demos:build
npm run demos:dev
```

These commands install, build, and launch the app through `meet-react/scripts/meet-demos.mjs`. They use the bundled PCM engine; they do not compile it from the root project. Ctrl+C stops the server.

If you change the core algebra, scorers, or presentation adapter, install the repository's root npm dependencies and refresh the bundled engine from the repository root:

```sh
npm install
npm run demos:engine
```

This optional step needs Java and sbt. Its source is `pcmalgebra/demo-engine`, the root sbt project `meetEngine`. React's grammar comes from its dependency; update that pin and lockfile to change versions. Ordinary React development needs neither Java nor sbt. Do not edit the generated `vendor/pcm-engine.js` file by hand.

## Read the code

The frontend uses this flow:

```text
input text -> local parser -> bundled Scala PCM operation -> report -> tables and DSL
```

1. `meet-react/src/App.tsx` contains inputs, explicit evaluation, and result rendering; `src/state.ts` contains the pure workspace reducer.
2. `src/shared/browser.ts` is the short computation entry point and download/origin helper. `parser.ts` rejects unsupported or lossy inputs. The `@meet/grammar` alias reads generated files from the app-local dependency.
3. `src/shared/types.ts` defines the report and operation labels; `scenarios.ts` supplies sample DSL, and `styles.css` supplies the styles.
4. For engine internals, read [PcmEvaluator.scala](pcmalgebra/demo-engine/src/main/scala/org/aurora/meetdemo/PcmEvaluator.scala). `evaluate` selects join, meet, validation, decomposition, or scoring. The remaining methods format results; the actual algebra and scorers live in `pcmalgebra/src/main/scala`.

Use `demos:engine` to refresh the engine bundle after changing the core implementation. Vite and TypeScript resolve `@meet/grammar` into the app's npm dependency because Aurora 0.4.2 does not publicly export generated-file paths.

The UI contains native selects, two textareas, file imports, three action buttons, section checkboxes, and result tables/DSL. File reads are the only asynchronous input path; each input ignores reads superseded by a later edit.

## Meeting walkthrough

Click **Run** after each change.

1. **GCS: complete designer / Validate:** all three reference items survive the meet. B's extra pupil response is allowed.
2. **GCS: missing response / Validate:** motor response is missing. **Score** shows incomplete inputs and no total.
3. **GCS: facts across files / Join**, then **Score:** the combined facts produce GCS 7, severe.
4. **Different values / Join**, **Meet**, **Validate:** join keeps both eye values; meet keeps the item without either value; the specified reference value is not preserved.
5. **Disjoint sections / Decompose:** Clinical and Orders reconstruct the full join. Deselect Orders for a partial result. Selecting an absent section extracts no part.
6. **AF: stroke risk / Score:** the existing CHA2DS2-VASc scorer produces 5.

## Semantics and limits

- Validation uses `(A |&| B) == A`, with A as the reference. Group paths, specified values, and represented annotations matter. Scoring readiness is a separate check.
- Shared counts and origin labels compare section/group/item paths. Shared paths can have different values. Refreshed scoring outputs are labelled Derived.
- Decomposition projects selected sections of `A |+| B` and rejoins them. References between sections can require another part.
- Scoring uses the repository's existing rules, including first facts in joined group/item order. The UI reports input issues and conditional notes rather than changing those rules.

Paste resolved Clinical, Issues, and Orders sections. Imports, modules/templates, arithmetic, and other unsupported/lossy constructs are rejected. The existing DSL formatter can omit some annotations; model notes identify affected exports. Derived scoring output can refer to guidance modules that this demo does not expand.

## Checks

```sh
npm --prefix meet-react test
npm --prefix meet-react run test:integration
npm run demos:test
```

The Chromium suite runs independently through either app test command. Root `demos:test` also runs Scala engine regressions. Coverage includes the real grammar, all eight examples through all five operations, explicit Run, stale-result clearing, imports, downloads, reset, and decomposition. Install Chromium if needed with `cd meet-react && npx playwright install chromium`.

Current previews: [React desktop](meet-react/previews/react-desktop.png), [React mobile](meet-react/previews/react-mobile.png).
