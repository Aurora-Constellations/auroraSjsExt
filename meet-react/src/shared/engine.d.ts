declare module '@meet/engine' {
  export function evaluateAst(left: unknown, right: unknown, operation: string, selectedSections: string[]): unknown;
}
