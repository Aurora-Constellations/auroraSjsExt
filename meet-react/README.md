# PCM algebra · React

A small React + TypeScript workbench for the existing PCM algebra and clinical scorers. Choose an example and operation, edit or import either model, then click **Run**. Edits clear the previous result; **Reset selected example** restores the preset.

Run from this app directory:

```sh
npm ci
npm run dev
```

Open <http://127.0.0.1:5173>. This app builds and runs independently with npm. Its own pinned `aurora-langium` 0.4.2 dependency supplies the grammar; `vendor/pcm-engine.js` supplies the precompiled Scala.js engine. No copied grammar or Java/sbt build is needed.

```sh
npm run typecheck
npm run build
npm run preview
```

Preview runs at <http://127.0.0.1:4173>. Browser checks also run independently:

```sh
npx playwright install chromium
npm test
```

`npm run test:integration` runs the same browser and engine integration checks. Repository commands and engine refresh instructions are in [MEET_DEMOS.md](../MEET_DEMOS.md).

Read the code in this order:

1. `src/main.tsx`: mounts React and imports the shared stylesheet.
2. `src/App.tsx`: editable inputs, explicit Run, and result tables/DSL.
3. `src/state.ts`: immutable changes and result/error clearing.
4. `src/shared/types.ts`: the typed engine report and operation metadata.
5. `src/shared/scenarios.ts`: editable example PCMs.
6. `src/shared/browser.ts`: parser/engine boundary and download helpers.
7. `src/shared/parser.ts`: validation using generated grammar and AST types from the app's own `aurora-langium` dependency.
8. [PcmEvaluator.scala](../pcmalgebra/demo-engine/src/main/scala/org/aurora/meetdemo/PcmEvaluator.scala): the source adapter over the existing Scala algebra and scorers; `vendor/pcm-engine.js` is its precompiled browser library.

`@meet/grammar` maps to `node_modules/aurora-langium/src/language/generated` in Vite and TypeScript. The narrow alias is needed because 0.4.2 does not export those package paths; it loads generated grammar/reflection without the full extension services. Update the dependency pin and lockfile to change React's grammar version.

Decomposition extracts selected sections from A join B. Validation tests reference structure; scoring checks clinical inputs separately. Conditional model notes describe formatter and scoring limitations. `tests`, `playwright.config.ts`, and `previews` contain browser checks and screenshots.
