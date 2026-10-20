import { test, expect, type Page } from '@playwright/test';

async function example(page: Page, id: string) {
  await page.locator('#scenario-select').selectOption(id);
}

async function run(page: Page, operation: string) {
  await page.locator('#operation-select').selectOption(operation);
  await page.getByRole('button', { name: 'Run', exact: true }).click();
}

test.describe('React', () => {
  test.beforeEach(async ({ page }) => { await page.goto('http://127.0.0.1:5173'); });

  test('evaluation is explicit and edits clear the previous result', async ({ page }) => {
    const result = page.locator('.result');
    await expect(result).toContainText('Run an operation');
    await run(page, 'validate');
    await expect(result).toContainText(/3\s*\/\s*3/);
    const designer = page.locator('#model-b');
    await designer.fill('Clinical:\nNeurologic:\ngcs_eye [4 _]\ngcs_verbal [2 _]');
    await expect(result).toContainText('Run an operation');
    await page.getByRole('button', { name: 'Run', exact: true }).click();
    await expect(result).toContainText('gcs_motor');
    await expect(result).toContainText(/failed|Valid: false/i);
    await designer.fill('Clinical:\nNeurologic:\ngcs_eye [');
    await expect(page.getByRole('alert')).toHaveCount(0);
    await page.getByRole('button', { name: 'Run', exact: true }).click();
    await expect(page.getByRole('alert')).toContainText('Right model');
    await expect(result.locator('table')).toHaveCount(0);
  });

  test('joined inputs produce the existing GCS and AF scores', async ({ page }) => {
    await example(page, 'gcs-composed');
    await run(page, 'score');
    const gcs = page.locator('tr').filter({ hasText: 'GCS Adult' });
    await expect(gcs).toContainText('7');
    await expect(gcs).toContainText('severe');
    await expect(page.locator('.result')).toContainText('Derived');
    await example(page, 'af');
    await expect(page.locator('.result')).toContainText('Run an operation');
    await page.getByRole('button', { name: 'Run', exact: true }).click();
    await expect(page.locator('tr').filter({ hasText: 'CHA2DS2-VASc' })).toContainText('5');
  });

  test('section selection gives full, partial, and empty reconstruction', async ({ page }) => {
    await example(page, 'disjoint');
    await run(page, 'decompose');
    const result = page.locator('.result');
    await expect(result).toContainText(/reconstruct A join B|Full reconstruction/);
    const orders = page.getByRole('checkbox', { name: 'Orders', exact: true });
    await orders.focus();
    await page.keyboard.press('Space');
    await expect(orders).toBeFocused();
    await expect(result).toContainText('Run an operation');
    await page.getByRole('button', { name: 'Run', exact: true }).click();
    await expect(result).toContainText(/part of A join B|Partial reconstruction/);
    await page.getByRole('checkbox', { name: 'Clinical', exact: true }).uncheck();
    await expect(page.getByRole('checkbox', { name: 'Issues', exact: true })).toBeChecked();
    await page.getByRole('button', { name: 'Run', exact: true }).click();
    await expect(result).toContainText('No parts extracted from the selected sections');
  });

  test('reset, local import, and DSL download use the visible inputs', async ({ page }) => {
    await example(page, 'disjoint');
    const left = page.locator('#model-a');
    const right = page.locator('#model-b');
    const baseline = [await left.inputValue(), await right.inputValue()];
    await left.fill('');
    await page.getByRole('button', { name: 'Swap A and B' }).click();
    await page.getByRole('button', { name: 'Reset selected example' }).click();
    await expect(left).toHaveValue(baseline[0]);
    await expect(right).toHaveValue(baseline[1]);
    const upload = 'Clinical:\nVitals:\ntemperature [38 C]';
    await page.getByLabel('Import Model A').setInputFiles({
      name: 'custom.aurora', mimeType: 'text/plain', buffer: Buffer.from(upload)
    });
    await expect(left).toHaveValue(upload);
    await run(page, 'join');
    const downloading = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Download DSL', exact: true }).click();
    const file = await downloading;
    const stream = await file.createReadStream();
    const chunks: Buffer[] = [];
    for await (const chunk of stream!) chunks.push(chunk);
    expect(file.suggestedFilename()).toMatch(/\.aurora$/);
    expect(Buffer.concat(chunks).toString()).toContain('temperature [38 C]');
  });

  test('imports complete independently and later edits supersede old reads', async ({ page }) => {
    type Pending = { completeImports: Record<string, () => Promise<void>> };
    await page.evaluate(() => {
      const pending = window as unknown as Pending;
      pending.completeImports = {};
      const read = Blob.prototype.text;
      File.prototype.text = function(this: File) {
        return new Promise<string>((resolve, reject) => {
          pending.completeImports[this.name] = () => read.call(this).then(resolve, reject);
        });
      };
    });
    const left = 'Clinical:\nNeurologic:\ngcs_eye [2 _]\ngcs_verbal [2 _]';
    const right = 'Clinical:\nNeurologic:\ngcs_motor [3 _]';
    await page.getByLabel('Import Model A').setInputFiles({ name: 'a.aurora', mimeType: 'text/plain', buffer: Buffer.from(left) });
    await page.getByLabel('Import Model B').setInputFiles({ name: 'b.aurora', mimeType: 'text/plain', buffer: Buffer.from(right) });
    await page.locator('#operation-select').selectOption('score');
    await page.evaluate(async () => {
      const pending = window as unknown as Pending;
      await Promise.all(Object.values(pending.completeImports).map(finish => finish()));
    });
    await expect(page.locator('#model-a')).toHaveValue(left);
    await expect(page.locator('#model-b')).toHaveValue(right);
    await page.getByRole('button', { name: 'Run', exact: true }).click();
    await expect(page.locator('tr').filter({ hasText: 'GCS Adult' })).toContainText('7');
    await page.getByLabel('Import Model A').setInputFiles({ name: 'old.aurora', mimeType: 'text/plain', buffer: Buffer.from(left) });
    await page.locator('#model-a').fill('');
    await page.evaluate(async () => { await (window as unknown as Pending).completeImports['old.aurora'](); });
    await expect(page.locator('#model-a')).toHaveValue('');
  });

  test('small screens retain usable controls and no document overflow', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', error => errors.push(error.message));
    for (const operation of ['join', 'meet', 'validate', 'score', 'decompose']) {
      await run(page, operation);
      for (const width of [390, 320]) {
        await page.setViewportSize({ width, height: 844 });
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      }
    }
    expect(errors).toEqual([]);
  });
});
