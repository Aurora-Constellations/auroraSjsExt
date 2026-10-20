export type Operation = 'join' | 'meet' | 'validate' | 'decompose' | 'score';
export type Origin = 'left' | 'right' | 'both' | 'derived';
export interface Item {
  id: string;
  name: string;
  section: string;
  group: string;
  detail: string;
  origin: Origin;
}
export interface Model { dsl: string; items: Item[]; sections: string[] }
export interface Validation {
  valid: boolean;
  matched: number;
  required: number;
  missing: string[];
  message: string;
}
export interface Score {
  name: string;
  status: string;
  total: number | null;
  band: string | null;
  source: string | null;
  issues: string[];
}
export interface Report {
  operation: Operation;
  left: Model;
  right: Model;
  result: Model;
  counts: { left: number; right: number; common: number; leftOnly: number; rightOnly: number; result: number };
  validation: Validation | null;
  scores: Score[];
  warnings: string[];
  decomposition: { parts: Model[]; recomposed: boolean } | null;
}
export interface Scenario { id: string; label: string; description: string; left: string; right: string }
export interface OperationMetadata { id: Operation; label: string; description: string }

export const defaultSections = ['Clinical', 'Issues', 'Orders'];
export const operations: OperationMetadata[] = [
  { id: 'join', label: 'Join', description: 'Combine A and B. Distinct values are kept.' },
  { id: 'meet', label: 'Meet', description: 'Keep the shared paths, values, and annotations of A and B.' },
  { id: 'validate', label: 'Validate', description: 'Check whether meet(A, B) equals A. A is the reference; its specified values and annotations must also survive.' },
  { id: 'decompose', label: 'Decompose', description: 'Split the join into selected sections, then join those parts. Cross-section references may need another part.' },
  { id: 'score', label: 'Score', description: 'Join A and B, check the inputs, and run the existing GCS and CHA2DS2-VASc scorers.' }
];
