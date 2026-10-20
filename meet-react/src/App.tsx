import { useReducer, useRef } from 'react';
import { defaultSections, downloadDSL, evaluate, operations, originLabel, scenarios } from '@meet/shared';
import type { Operation, Report } from '@meet/shared';
import { initialWorkspace, workspaceReducer } from './state';
import type { Action } from './state';

export function App() {
  const [state, dispatch] = useReducer(workspaceReducer, undefined, initialWorkspace);
  const importVersions = useRef({ left: 0, right: 0 });
  const change = (action: Action) => {
    if (action.type === 'example' || action.type === 'swap') {
      importVersions.current.left += 1;
      importVersions.current.right += 1;
    } else if (action.type === 'change') {
      if (action.value.left !== undefined) importVersions.current.left += 1;
      if (action.value.right !== undefined) importVersions.current.right += 1;
    }
    dispatch(action);
  };
  const scenario = scenarios.find((item) => item.id === state.scenarioId)!;
  const operation = operations.find((item) => item.id === state.operation)!;

  async function importFile(file: File | undefined, side: 'left' | 'right') {
    if (!file) return;
    const version = ++importVersions.current[side];
    dispatch({ type: 'change', value: {} });
    try {
      const text = await file.text();
      if (version === importVersions.current[side]) change({ type: 'change', value: { [side]: text } });
    } catch (error) {
      if (version === importVersions.current[side]) dispatch({ type: 'error', message: `File import: ${String(error)}` });
    }
  }

  function run() {
    try {
      dispatch({ type: 'result', report: evaluate(state.left, state.right, state.operation, state.sections) });
    } catch (error) {
      dispatch({ type: 'error', message: error instanceof Error ? error.message : String(error) });
    }
  }

  return (
    <main className="workbench">
      <header className="page-header"><h1>PCM algebra</h1><span>React</span></header>
      <div className="controls">
        <label htmlFor="scenario-select">Example
          <select id="scenario-select" value={state.scenarioId} onChange={(event) => change({ type: 'example', id: event.target.value })}>
            {scenarios.map((item) => <option key={item.id} value={item.id}>{item.label}</option>)}
          </select>
        </label>
        <label htmlFor="operation-select">Operation
          <select id="operation-select" value={state.operation} onChange={(event) => change({ type: 'change', value: { operation: event.target.value as Operation } })}>
            {operations.map((item) => <option key={item.id} value={item.id}>{item.label}</option>)}
          </select>
        </label>
      </div>
      <p className="scenario-description">{scenario.description}</p>
      <p className="operation-hint"><strong>{operation.label}:</strong> {operation.description}</p>
      <div className="inputs">
        {(['left', 'right'] as const).map((side, index) => (
          <section className="input-panel" key={side}>
            <label htmlFor={`model-${index ? 'b' : 'a'}`}>Model {index ? 'B' : 'A'}{state.operation === 'validate' ? (index ? ' (designer)' : ' (reference)') : ''}</label>
            <textarea id={`model-${index ? 'b' : 'a'}`} className="model-input" spellCheck={false} value={state[side]} onChange={(event) => change({ type: 'change', value: { [side]: event.target.value } })} />
            <label htmlFor={`file-${side}`}>Import Model {index ? 'B' : 'A'}
              <input id={`file-${side}`} type="file" accept=".aurora,.pcm,.txt,text/plain" onChange={(event) => {
                void importFile(event.target.files?.[0], side);
                event.target.value = '';
              }} />
            </label>
          </section>
        ))}
      </div>
      {state.operation === 'decompose' && (
        <fieldset className="section-picker">
          <legend>Sections to extract from A join B</legend>
          {defaultSections.map((section) => (
            <label key={section}>
              <input type="checkbox" checked={state.sections.includes(section)} onChange={(event) => change({ type: 'change', value: { sections: event.target.checked ? [...state.sections, section] : state.sections.filter((name) => name !== section) } })} />
              {section}
            </label>
          ))}
        </fieldset>
      )}
      <div className="actions">
        <button onClick={() => change({ type: 'example', id: state.scenarioId })}>Reset selected example</button>
        <button onClick={() => change({ type: 'swap' })}>Swap A and B</button>
        <button className="primary-action" onClick={run}>Run</button>
      </div>
      <section className="result" aria-live="polite">
        {state.error ? <p className="error" role="alert">{state.error}</p> : state.report ? <Result report={state.report} /> : <p className="empty-result">Run an operation to see the result.</p>}
      </section>
    </main>
  );
}

function Result({ report }: { report: Report }) {
  const { counts, validation, scores, decomposition } = report;
  return (
    <>
      <h2>{operations.find((item) => item.id === report.operation)?.label} result</h2>
      <p>A: {counts.left}; B: {counts.right}; Shared paths: {counts.common}; A only: {counts.leftOnly}; B only: {counts.rightOnly}; Result: {counts.result} elements.</p>
      <p>Shared paths have the same section, group, and element; their values may differ.</p>
      {validation && (
        <section>
          <h3>Reference validation</h3>
          <p>Valid: {String(validation.valid)}. Matched: {validation.matched}/{validation.required}.</p>
          <p>{validation.message}</p>
          {validation.missing.length > 0 && <><p>Missing or differing requirements:</p><ul>{validation.missing.map((name) => <li key={name}>{name}</li>)}</ul></>}
        </section>
      )}
      {scores.length > 0 && (
        <section>
          <h3>Scores</h3>
          <div className="table-scroll" tabIndex={0} role="region" aria-label="Scores">
            <table>
              <thead><tr><th>Score</th><th>Status</th><th>Total</th><th>Band</th><th>Source</th><th>Issues</th></tr></thead>
              <tbody>
                {scores.map((score) => (
                  <tr key={score.name}>
                    <th scope="row">{score.name}</th>
                    <td>{score.status}</td>
                    <td>{score.total ?? '—'}</td>
                    <td>{score.band ?? '—'}</td>
                    <td>{score.source ?? '—'}</td>
                    <td>{score.issues.join('; ') || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
      {decomposition && (
        <section>
          <h3>Decomposition</h3>
          <p>{decomposition.recomposed ? 'The selected parts reconstruct A join B.' : decomposition.parts.length ? 'The selected parts reconstruct part of A join B.' : 'No parts extracted from the selected sections; the result is empty.'}</p>
          {decomposition.parts.map((part, index) => (
            <details key={index} className="dsl-output">
              <summary>{part.sections.join(', ')} ({part.items.length} elements)</summary>
              <pre tabIndex={0}>{part.dsl || '(empty PCM)'}</pre>
              <button onClick={() => downloadDSL(part.dsl, `pcm-part-${index + 1}.aurora`)}>Download part</button>
            </details>
          ))}
        </section>
      )}
      {report.warnings.length > 0 && <details className="notes"><summary>Model notes</summary><ul>{report.warnings.map((warning) => <li key={warning}>{warning}</li>)}</ul></details>}
      <h3>Elements</h3>
      <div className="table-scroll" tabIndex={0} role="region" aria-label="Result elements">
        <table>
          <thead><tr><th>Section</th><th>Group</th><th>Element</th><th>Details</th><th>Origin</th></tr></thead>
          <tbody>
            {report.result.items.map((item, index) => (
              <tr key={`${item.id}-${index}`}>
                <td>{item.section}</td>
                <td>{item.group || '—'}</td>
                <td>{item.name}</td>
                <td>{item.detail || '—'}</td>
                <td>{originLabel(item.origin)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {report.result.items.length === 0 && <p>No elements in the result.</p>}
      <section className="dsl-output">
        <h3>Result Aurora DSL</h3>
        <pre tabIndex={0}>{report.result.dsl || '(empty PCM)'}</pre>
        <button onClick={() => downloadDSL(report.result.dsl, 'pcm-result.aurora')}>Download DSL</button>
      </section>
    </>
  );
}
