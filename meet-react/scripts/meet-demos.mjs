import { spawn, spawnSync } from 'node:child_process';
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const npm = process.platform === 'win32' ? 'npm.cmd' : 'npm';
const sbt = process.platform === 'win32' ? 'sbt.bat' : 'sbt';
const app = 'meet-react';

function run(command, args) {
  const result = spawnSync(command, args, { cwd: root, stdio: 'inherit', shell: process.platform === 'win32' });
  if (result.error) {
    console.error(result.error.message);
    process.exit(1);
  }
  if (result.status !== 0) process.exit(result.status ?? 1);
}

function refreshEngine() {
  run(sbt, ['-batch', 'meetEngine/fullLinkJS']);
  const source = new URL('../../pcmalgebra/demo-engine/target/scala-3.5.1/meetengine-opt/main.js', import.meta.url);
  const engine = readFileSync(source, 'utf8')
    .replace(/^\/\*\* @constructor \*\/\r?\n?/gm, '')
    .replace(/^\/\/# sourceMappingURL=.*$/gm, '');
  writeFileSync(new URL('../../meet-react/vendor/pcm-engine.js', import.meta.url), engine.trimEnd() + '\n');
}

const action = process.argv[2] ?? 'dev';
if (action === 'setup') {
  run(npm, ['--prefix', app, 'install', '--workspaces=false']);
} else if (action === 'build') {
  run(npm, ['--prefix', app, 'run', 'build']);
} else if (action === 'test') {
  run(sbt, ['-batch', 'meetEngine/test']);
  run(npm, ['--prefix', app, 'run', 'test:integration']);
} else if (action === 'engine') {
  refreshEngine();
} else if (action === 'dev') {
  console.log('\nReact: http://127.0.0.1:5173\n');
  const child = spawn(npm, ['--prefix', app, 'run', 'dev', '--', '--strictPort'], {
    cwd: root, stdio: 'inherit', shell: process.platform === 'win32', detached: process.platform !== 'win32'
  });
  let stopping = false;
  function stop(code = 0) {
    if (stopping) return;
    stopping = true;
    if (child.pid) {
      if (process.platform === 'win32') spawnSync('taskkill', ['/pid', String(child.pid), '/T', '/F'], { stdio: 'ignore' });
      else {
        try { process.kill(-child.pid, 'SIGTERM'); }
        catch (error) { if (error.code !== 'ESRCH') console.error(error.message); }
      }
    }
    process.exitCode = code;
  }
  for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => stop());
  child.on('error', error => { console.error(error.message); stop(1); });
  child.on('exit', code => stop(code ?? 0));
} else {
  console.error('Usage: node meet-react/scripts/meet-demos.mjs setup|build|test|dev|engine');
  process.exit(1);
}
