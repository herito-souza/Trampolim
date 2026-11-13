import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = path => readFile(new URL(path, import.meta.url), 'utf8');

test('manifesto define instalação do Trampolim e ícones da marca', async () => {
  const manifest = JSON.parse(await read('../public/manifest.webmanifest'));
  assert.equal(manifest.name, 'Trampolim — Oportunidades, Empresas e Capacita');
  assert.equal(manifest.display, 'standalone');
  assert.ok(manifest.icons.some(icon => icon.sizes === '192x192'));
  assert.ok(manifest.icons.some(icon => icon.sizes === '512x512'));
  assert.ok(manifest.icons.some(icon => icon.purpose === 'maskable'));
});

test('service worker exclui APIs privadas e mídia pesada do cache', async () => {
  const worker = await read('../public/sw.js');
  assert.match(worker, /request\.headers\.has\('Authorization'\)/);
  assert.match(worker, /request\.headers\.has\('Range'\)/);
  assert.match(worker, /isPublicLearningResource/);
  assert.match(worker, /MAX_PUBLIC_BYTES = 1024 \* 1024/);
  assert.match(worker, /Cache-Control/);
  assert.match(worker, /\/assets\//);
  assert.match(worker, /ignoreVary: true/);
  assert.doesNotMatch(worker, /\/api\/company|\/api\/profile|\/api\/applications/);
});

test('interface informa ausência de conexão e não simula confirmação de ações', async () => {
  const component = await read('../src/components/ui/PwaRuntime.tsx');
  const offline = await read('../public/offline.html');
  assert.match(component, /Sem conexão/);
  assert.match(component, /só serão confirmadas quando o servidor responder/);
  assert.match(component, /Preparar atualização/);
  assert.match(offline, /não enviará candidaturas/);
});
