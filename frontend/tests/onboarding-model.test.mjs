import test from 'node:test';
import assert from 'node:assert/strict';
import { onboardingStorageKey, readOnboardingState, saveOnboardingState, scheduleOnboarding } from '../src/components/workspace/onboardingModel.ts';

function withStorage(run) {
  const values = new Map();
  globalThis.localStorage = { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, String(value)), removeItem: key => values.delete(key) };
  try { run(values); } finally { delete globalThis.localStorage; }
}

test('onboarding fica separado por trilha e identidade sem compartilhar login', () => withStorage(() => {
  const candidate = onboardingStorageKey('oportunidades', 'Pessoa@Example.com');
  const student = onboardingStorageKey('capacita', 'pessoa@example.com');
  assert.notEqual(candidate, student);
  scheduleOnboarding('oportunidades', 'Pessoa@Example.com');
  assert.deepEqual(readOnboardingState(candidate), { status: 'pending', step: 0 });
  assert.equal(readOnboardingState(student), null);
}));

test('retomar, adiar, pular e concluir podem ser persistidos sem repetir o estado inicial', () => withStorage(() => {
  const key = onboardingStorageKey('empresas', 'owner@example.com');
  scheduleOnboarding('empresas', 'owner@example.com');
  saveOnboardingState(key, { status: 'paused', step: 1 });
  scheduleOnboarding('empresas', 'owner@example.com');
  assert.deepEqual(readOnboardingState(key), { status: 'paused', step: 1 });
  saveOnboardingState(key, { status: 'skipped', step: 1 });
  assert.deepEqual(readOnboardingState(key), { status: 'skipped', step: 1 });
  saveOnboardingState(key, { status: 'completed', step: 2 });
  assert.deepEqual(readOnboardingState(key), { status: 'completed', step: 2 });
}));

test('dados locais inválidos não interrompem o acesso', () => withStorage(values => {
  const key = onboardingStorageKey('capacita', 'student@example.com');
  values.set(key, '{bad json');
  assert.equal(readOnboardingState(key), null);
  values.set(key, JSON.stringify({ status: 'unexpected', step: -1 }));
  assert.equal(readOnboardingState(key), null);
  scheduleOnboarding('capacita', '   ');
  assert.equal(values.size, 1);
}));
