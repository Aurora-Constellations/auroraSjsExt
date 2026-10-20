import { defineConfig } from '@playwright/test';
import { fileURLToPath } from 'node:url';

const app = fileURLToPath(new URL('./', import.meta.url));
export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  fullyParallel: false,
  workers: 1,
  reporter: 'list',
  use: { browserName: 'chromium', headless: true, viewport: { width: 1440, height: 1000 } },
  webServer: { command: 'npm run dev -- --strictPort', cwd: app, url: 'http://127.0.0.1:5173', reuseExistingServer: !process.env.CI, timeout: 60_000 }
});
