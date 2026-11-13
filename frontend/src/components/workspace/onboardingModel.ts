export type OnboardingStatus = 'pending' | 'in_progress' | 'paused' | 'skipped' | 'completed';
export type OnboardingState = { status: OnboardingStatus; step: number };

export function onboardingStorageKey(trail: string, identity: string) {
  return `trampolim_onboarding:${trail}:${identity.trim().toLowerCase()}`;
}

export function readOnboardingState(key: string): OnboardingState | null {
  try {
    const value = localStorage.getItem(key);
    if (!value) return null;
    const parsed = JSON.parse(value) as Partial<OnboardingState>;
    if (!['pending', 'in_progress', 'paused', 'skipped', 'completed'].includes(String(parsed.status))) return null;
    return { status: parsed.status as OnboardingStatus, step: Number.isSafeInteger(parsed.step) && Number(parsed.step) >= 0 ? Number(parsed.step) : 0 };
  } catch { return null; }
}

export function saveOnboardingState(key: string, state: OnboardingState) {
  try { localStorage.setItem(key, JSON.stringify(state)); } catch { /* The tutorial remains usable when storage is unavailable. */ }
}

/** Called only after a successful new account registration; never marks login as a new account. */
export function scheduleOnboarding(trail: string, identity: string) {
  if (!identity.trim()) return;
  const key = onboardingStorageKey(trail, identity);
  if (!readOnboardingState(key)) saveOnboardingState(key, { status: 'pending', step: 0 });
}
