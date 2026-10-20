import { test, expect, type Page } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import type { Operation, Report } from '../src/shared/types';

const moduleUrl = `/@fs${fileURLToPath(new URL('../src/shared/browser.ts', import.meta.url))}`;
async function evaluate(page: Page, scenario: string, operation: Operation, sections?: string[]): Promise<Report> {
  return page.evaluate(async ({ moduleUrl, scenario, operation, sections }) => {
    const api = await import(moduleUrl);
    const preset = api.scenarios.find((item: { id: string }) => item.id === scenario);
    return api.evaluate(preset.left, preset.right, operation, sections);
  }, { moduleUrl, scenario, operation, sections });
}

test.beforeEach(async ({ page }) => { await page.goto('http://127.0.0.1:5173'); });

test('join preserves values and extras; meet preserves the bare reference', async ({ page }) => {
  const join = await evaluate(page, 'gcs-reference', 'join');
  expect(join.result.items).toHaveLength(4);
  expect(join.result.dsl).toContain('gcs_eye [4 _]');
  expect(join.result.items.find(item => item.name === 'pupil_response')?.origin).toBe('right');
  const meet = await evaluate(page, 'gcs-reference', 'meet');
  expect(meet.result.items.map(item => item.name)).toEqual(['gcs_eye', 'gcs_verbal', 'gcs_motor']);
  expect(meet.result.dsl).not.toContain('[');
  expect(meet.result.dsl).not.toContain('pupil_response');
});

test('reference coverage detects missing fields, wrong groups, and value constraints', async ({ page }) => {
  const complete = await evaluate(page, 'gcs-reference', 'validate');
  expect(complete.validation).toMatchObject({ valid: true, matched: 3, required: 3 });
  const missing = await evaluate(page, 'gcs-missing', 'validate');
  expect(missing.validation?.valid).toBe(false);
  expect(missing.validation?.missing.join(' ')).toContain('gcs_motor');
  const wrongGroup = await evaluate(page, 'wrong-group', 'validate');
  expect(wrongGroup.validation?.valid).toBe(false);
  expect(wrongGroup.validation?.matched).toBe(0);
  const conflict = await evaluate(page, 'conflicting-values', 'validate');
  expect(conflict.validation?.valid).toBe(false);
  expect(conflict.validation?.missing.join(' ')).toContain('gcs_eye');
});

test('composition supplies incomplete score inputs and keeps the real scorer outputs', async ({ page }) => {
  const gcs = await evaluate(page, 'gcs-composed', 'score');
  expect(gcs.scores.find(score => score.name.includes('GCS'))).toMatchObject({ total: 7, band: 'severe' });
  expect(gcs.result.dsl).toContain('gcs_total [7 _]');
  expect(gcs.result.dsl).toContain('score_gcs_severe');
  const af = await evaluate(page, 'af', 'score');
  expect(af.scores.find(score => score.name.includes('VASc'))).toMatchObject({ total: 5, band: 'high' });
  const partial = await evaluate(page, 'gcs-missing', 'score');
  const check = partial.scores.find(score => score.name.includes('GCS'));
  expect(check?.total).toBeNull();
  expect(check?.issues.join(' ')).toContain('gcs_motor');
});

test('decomposition reconstructs the whole model only when all parts are selected', async ({ page }) => {
  const all = await evaluate(page, 'disjoint', 'decompose');
  expect(all.decomposition?.recomposed).toBe(true);
  expect(all.decomposition?.parts).toHaveLength(2);
  const clinicalOnly = await evaluate(page, 'disjoint', 'decompose', ['Clinical']);
  expect(clinicalOnly.decomposition?.recomposed).toBe(false);
  expect(clinicalOnly.result.sections).toEqual(['Clinical']);
  const none = await evaluate(page, 'disjoint', 'decompose', []);
  expect(none.decomposition?.recomposed).toBe(false);
  expect(none.result.items).toHaveLength(0);
});

test('empty identity and disjoint meet behave as the algebra specifies', async ({ page }) => {
  const emptyJoin = await evaluate(page, 'empty', 'join');
  expect(emptyJoin.result.dsl).toBe(emptyJoin.right.dsl);
  const emptyMeet = await evaluate(page, 'empty', 'meet');
  expect(emptyMeet.result.dsl).toBe('');
  const emptyReference = await evaluate(page, 'empty', 'validate');
  expect(emptyReference.validation?.valid).toBe(true);
  const disjoint = await evaluate(page, 'disjoint', 'meet');
  expect(disjoint.result.sections).toHaveLength(0);
});

test('malformed DSL and unsupported imports reject without presenting a partial result', async ({ page }) => {
  const errors = await page.evaluate(async ({ moduleUrl }) => {
    const api = await import(moduleUrl);
    const inputs = ['Clinical:\nNeurologic:\ngcs_eye [', 'import other.*;\nClinical:\nNeurologic:\ngcs_eye'];
    return Promise.all(inputs.map(async left => {
      try { await api.evaluate(left, '', 'join'); return ''; }
      catch (error) { return String(error); }
    }));
  }, { moduleUrl });
  expect(errors[0]).toContain('Left model');
  expect(errors[1]).toContain('import');
});

test('coordinate references survive parse-only conversion and unknown targets reject', async ({ page }) => {
  const result = await page.evaluate(async ({ moduleUrl }) => {
    const api = await import(moduleUrl);
    const left = 'Clinical:\nNeurologic:\ngcs_eye [4 _]\ngcs_verbal (gcs_eye)';
    const report = await api.evaluate(left, left, 'validate');
    let error = '';
    try { await api.evaluate(left.replace('(gcs_eye)', '(unknown)'), '', 'join'); }
    catch (cause) { error = String(cause); }
    return { report, error };
  }, { moduleUrl });
  expect(result.report.validation.valid).toBe(true);
  expect(result.report.result.dsl).toContain('(gcs_eye)');
  expect(result.error).toContain('unresolved coordinate reference');
});

test('all bundled examples execute through every operation', async ({ page }) => {
  const outcomes = await page.evaluate(async ({ moduleUrl }) => {
    const api = await import(moduleUrl);
    const outcomes: string[] = [];
    for (const scenario of api.scenarios) for (const operation of api.operations) {
      const report = await api.evaluate(scenario.left, scenario.right, operation.id);
      outcomes.push(`${scenario.id}/${report.operation}`);
    }
    return outcomes;
  }, { moduleUrl });
  expect(outcomes).toHaveLength(40);
});
