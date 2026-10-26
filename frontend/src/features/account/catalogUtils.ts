export type OpportunityDeadlineStatus = 'no-deadline' | 'expired' | 'today' | 'closing-soon' | 'open';

export function getOpportunityDaysRemaining(expiresAt: string, today = new Date()): number | null {
  const deadline = new Date(`${expiresAt}T00:00:00`);
  const normalizedToday = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  if (Number.isNaN(deadline.getTime())) return null;
  return Math.round((deadline.getTime() - normalizedToday.getTime()) / 86400000);
}

export function getOpportunityDeadlineStatus(expiresAt: string | null, today = new Date()): OpportunityDeadlineStatus {
  if (!expiresAt) return 'no-deadline';
  const daysLeft = getOpportunityDaysRemaining(expiresAt, today);
  if (daysLeft === null) return 'no-deadline';
  if (daysLeft < 0) return 'expired';
  if (daysLeft === 0) return 'today';
  if (daysLeft <= 7) return 'closing-soon';
  return 'open';
}

export function getLinkReviewMessage(linkStatus: string | null | undefined, lastCheckedAt: string | null | undefined, now = Date.now()): string | null {
  if (linkStatus === 'BROKEN') return 'Este link foi reportado como indisponível. Confira antes de continuar.';
  if (!lastCheckedAt || linkStatus === 'UNKNOWN') return 'Este link ainda não foi conferido pela plataforma. Confira no site parceiro antes de continuar.';
  const lastChecked = new Date(lastCheckedAt).getTime();
  if (!Number.isFinite(lastChecked) || now - lastChecked > 30 * 86400000) return 'O link não é revisado há mais de 30 dias. Confira antes de usar.';
  return null;
}
