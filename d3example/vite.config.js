import {defineConfig} from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import rollupPluginSourcemaps from "rollup-plugin-sourcemaps";
import globResolverPlugin from "@raquo/vite-plugin-glob-resolver";
import importSideEffectPlugin from "@raquo/vite-plugin-import-side-effect";

export default defineConfig({
  base: "/",
  publicDir: "public",
  plugins: [
    scalaJSPlugin({
      cwd: "..", // path to build.sbt
      projectID: "d3example" // scala.js project name in build.sbt
    }),
    globResolverPlugin({
      // See https://github.com/raquo/vite-plugin-glob-resolver
      cwd: __dirname,
      ignore: [
        'node_modules/**',
        'target/**'
      ]
    }),
    importSideEffectPlugin({
      // See https://github.com/raquo/vite-plugin-import-side-effect
      defNames: ['importStyle'],
      rewriteModuleIds: ['**/*.less', '**/*.css'],
      // verbose: true
    })
  ],
  build: {
    outDir: "dist",
    assetsDir: "assets", // path relative to outDir
    cssCodeSplit: false,  // false = Load all CSS upfront
    rollupOptions: {
      plugins: [rollupPluginSourcemaps()],
    },
    minify: "terser",
    sourcemap: true
  },
  server: {
    port: 3333,
    strictPort: true,
    logLevel: "debug"
  },
  optimizeDeps: {
    esbuildOptions: {
      external: ['web-worker'],
    },
  },
  // build: {
  //   rollupOptions: {
  //     external: ['web-worker'],
  //   },
  // },
})
