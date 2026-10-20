import { defaultSections, scenarios, type Operation, type Report } from '@meet/shared';

export type Inputs = {
  left: string;
  right: string;
  scenarioId: string;
  operation: Operation;
  sections: string[];
};

export type Workspace = Inputs & { report: Report | null; error: string | null };
export type Action =
  | { type: 'change'; value: Partial<Inputs> }
  | { type: 'example'; id: string }
  | { type: 'swap' }
  | { type: 'result'; report: Report }
  | { type: 'error'; message: string };

function example(id: string, operation: Operation): Workspace {
  const scenario = scenarios.find((item) => item.id === id) ?? scenarios[0];
  return {
    left: scenario.left,
    right: scenario.right,
    scenarioId: scenario.id,
    operation,
    sections: [...defaultSections],
    report: null,
    error: null,
  };
}

export function initialWorkspace(): Workspace {
  return example(scenarios[0].id, 'join');
}

export function workspaceReducer(state: Workspace, action: Action): Workspace {
  const cleared = { ...state, report: null, error: null };
  switch (action.type) {
    case 'change': return { ...cleared, ...action.value };
    case 'example': return example(action.id, state.operation);
    case 'swap': return { ...cleared, left: state.right, right: state.left };
    case 'result': return { ...state, report: action.report, error: null };
    case 'error': return { ...cleared, error: action.message };
  }
}
