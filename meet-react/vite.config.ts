import { defineConfig } from 'vite';
import { fileURLToPath } from 'node:url';

export default defineConfig({
  base: './',
  resolve: {
    alias: {
      '@meet/shared': new URL('./src/shared/browser.ts', import.meta.url).pathname,
      '@meet/engine': new URL('./vendor/pcm-engine.js', import.meta.url).pathname,
      '@meet/grammar': fileURLToPath(new URL('./node_modules/aurora-langium/src/language/generated', import.meta.url)),
    },
  },
  build: { target: 'es2022' },
});
