import { evaluateAst } from '@meet/engine';
import { parseAurora } from './parser';
import { defaultSections, type Operation, type Origin, type Report } from './types';

export * from './types';
export { scenarios } from './scenarios';


export function evaluate(
  left: string,
  right: string,
  operation: Operation,
  selectedSections: string[] = defaultSections
): Report {
  const leftAst = parseAurora(left, 'Left model');
  const rightAst = parseAurora(right, 'Right model');
  return evaluateAst(leftAst, rightAst, operation, selectedSections) as Report;
}

const origins: Record<Origin, string> = {
  left: 'A', right: 'B', both: 'Both', derived: 'Derived'
};

export function originLabel(origin: Origin): string {
  return origins[origin];
}

export function downloadDSL(dsl: string, filename = 'result.aurora'): void {
  const url = URL.createObjectURL(new Blob([dsl], { type: 'text/plain;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}
