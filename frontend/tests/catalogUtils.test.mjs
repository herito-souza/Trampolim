import test from 'node:test';
import assert from 'node:assert/strict';
import { getLinkReviewMessage, getOpportunityDeadlineStatus, getOpportunityDaysRemaining } from '../src/features/account/catalogUtils.ts';

const today = new Date(2026, 0, 10, 12);

test('prazo classifica vagas vencidas, encerrando, hoje e abertas', () => {
  assert.equal(getOpportunityDeadlineStatus('2026-01-09', today), 'expired');
  assert.equal(getOpportunityDeadlineStatus('2026-01-10', today), 'today');
  assert.equal(getOpportunityDeadlineStatus('2026-01-17', today), 'closing-soon');
  assert.equal(getOpportunityDeadlineStatus('2026-01-18', today), 'open');
  assert.equal(getOpportunityDeadlineStatus(null, today), 'no-deadline');
  assert.equal(getOpportunityDaysRemaining('2026-01-17', today), 7);
});

test('links desconhecidos, quebrados ou antigos recebem aviso, link atual não', () => {
  const now = Date.parse('2026-01-31T12:00:00Z');
  assert.match(getLinkReviewMessage('UNKNOWN', null, now), /ainda não foi conferido/);
  assert.match(getLinkReviewMessage('BROKEN', '2026-01-30T12:00:00Z', now), /indisponível/);
  assert.match(getLinkReviewMessage('ACTIVE', '2025-12-01T12:00:00Z', now), /mais de 30 dias/);
  assert.equal(getLinkReviewMessage('ACTIVE', '2026-01-20T12:00:00Z', now), null);
});
