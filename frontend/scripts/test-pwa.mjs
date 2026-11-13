import { spawn } from 'node:child_process';

const host = '127.0.0.1';
const port = 4174;
const origin = `http://${host}:${port}`;
const preview = spawn(process.execPath, ['./node_modules/vite/bin/vite.js', 'preview', '--host', host, '--port', String(port), '--strictPort'], { stdio: 'inherit' });

function waitForExit(child) {
  return new Promise(resolve => child.once('close', (code, signal) => resolve({ code, signal })));
}

async function waitForPreview() {
  const expires = Date.now() + 30_000;
  while (Date.now() < expires) {
    if (preview.exitCode !== null) throw new Error('O servidor de prévia encerrou antes de ficar pronto.');
    try { if ((await fetch(origin)).ok) return; } catch { /* Vite is still starting. */ }
    await new Promise(resolve => setTimeout(resolve, 250));
  }
  throw new Error('Tempo esgotado ao iniciar a prévia do build.');
}

let testCode = 1;
try {
  await waitForPreview();
  const tests = spawn(process.execPath, ['--import', './playwright-env.mjs', './node_modules/@playwright/test/cli.js', 'test', '--config=playwright.pwa.config.ts'], { stdio: 'inherit' });
  testCode = (await waitForExit(tests)).code ?? 1;
} finally {
  if (preview.exitCode === null) {
    preview.kill();
    await Promise.race([waitForExit(preview), new Promise(resolve => setTimeout(resolve, 1500))]);
  }
}
process.exitCode = testCode;
