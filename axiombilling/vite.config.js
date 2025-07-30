import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

export default defineConfig({
  plugins: [scalaJSPlugin({
    cwd: "..", // path to build.sbt
    projectID: "axiombilling" // scala.js project name in build.sbt
  })],
});