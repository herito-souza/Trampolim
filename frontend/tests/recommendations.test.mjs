import test from 'node:test';
import assert from 'node:assert/strict';
import { createDiagnosticResult, getRecommendationReason, getRecommendationScore, getRecommendations, opportunities } from '../src/features/diagnostic/recommendations.ts';

test('diagnóstico gera trilha e recomendações compatíveis com as respostas', () => {
  const result = createDiagnosticResult([0, 0, 0, 0, 0, 0, 0, 0, 0, 0]);
  assert.equal(result.trackId, 'people');
  assert.equal(result.answers.length, 10);
  assert.ok(getRecommendations(result, 'course').some(item => item.tags.includes('service')));
  assert.ok(getRecommendations(result, 'job').every(item => item.tags.some(tag => result.scores[tag] > 0)));
});

test('diagnóstico muda a trilha conforme a primeira resposta', () => {
  assert.equal(createDiagnosticResult([1, ...Array(9).fill(0)]).trackId, 'organization');
  assert.equal(createDiagnosticResult([2, ...Array(9).fill(0)]).trackId, 'handsOn');
});

test('motivo da recomendação informa competências e pontuação', () => {
  const result = createDiagnosticResult([0, ...Array(9).fill(0)]);
  const reason = getRecommendationReason(result, ['service', 'sales']);
  assert.ok(reason);
  assert.ok(reason.points > 0);
  assert.ok(reason.skills.length > 0);
});

test('pontuação personalizada não duplica competências repetidas', () => {
  const result = createDiagnosticResult([0, ...Array(9).fill(0)]);
  assert.equal(getRecommendationScore(result, ['service', 'service']), result.scores.service);
});

test('sem diagnóstico, catálogo mantém todas as opções cadastradas de cada tipo', () => {
  assert.deepEqual(getRecommendations(null, 'course').map(item => item.id), opportunities.filter(item => item.kind === 'course').map(item => item.id));
  assert.deepEqual(getRecommendations(null, 'job').map(item => item.id), opportunities.filter(item => item.kind === 'job').map(item => item.id));
});
