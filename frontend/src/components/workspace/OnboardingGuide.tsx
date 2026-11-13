import { useEffect, useMemo, useState } from 'react';
import { onboardingStorageKey, readOnboardingState, saveOnboardingState } from './onboardingModel';
import type { OnboardingState } from './onboardingModel';

export type OnboardingStep = { title: string; description: string; actionLabel?: string; onAction?: () => void };

export function OnboardingGuide({ trail, identity, role, steps, openRequest, onOpenRequestHandled }: {
  trail: string; identity: string; role: string; steps: OnboardingStep[]; openRequest: number; onOpenRequestHandled: () => void;
}) {
  const storageKey = useMemo(() => onboardingStorageKey(trail, identity), [trail, identity]);
  const [state, setState] = useState<OnboardingState>(() => readOnboardingState(storageKey) || { status: 'paused', step: 0 });
  const [visible, setVisible] = useState(() => ['pending', 'in_progress'].includes(readOnboardingState(storageKey)?.status || ''));
  const step = steps[Math.min(state.step, Math.max(steps.length - 1, 0))];

  useEffect(() => {
    const saved = readOnboardingState(storageKey);
    const initial = saved || { status: 'paused' as const, step: 0 };
    setState(initial);
    setVisible(initial.status === 'pending' || initial.status === 'in_progress');
  }, [storageKey]);

  useEffect(() => {
    if (!openRequest) return;
    const next = { status: 'in_progress' as const, step: 0 };
    setState(next); setVisible(true); saveOnboardingState(storageKey, next); onOpenRequestHandled();
  }, [openRequest, storageKey, onOpenRequestHandled]);

  function update(next: OnboardingState) { setState(next); saveOnboardingState(storageKey, next); }
  if (!identity || !step || !visible) return null;

  return <aside role="region" aria-live="polite" aria-label={`Guia inicial: ${role}`} className="fixed inset-x-3 bottom-3 z-[80] mx-auto w-auto max-w-lg rounded-2xl border border-brand-blue/40 bg-[#101a29] p-5 text-ink shadow-2xl sm:inset-x-auto sm:bottom-5 sm:right-5 sm:w-[min(28rem,calc(100vw-2rem))]" data-testid="onboarding-guide">
    <div className="flex items-start justify-between gap-3">
      <div><p className="text-xs font-extrabold uppercase tracking-widest text-brand-blue">Primeiros passos · {trail}</p><p className="mt-1 text-xs text-ink/60">Passo {state.step + 1} de {steps.length}</p></div>
      <button type="button" aria-label="Adiar tutorial" onClick={() => { update({ ...state, status: 'paused' }); setVisible(false); }} className="min-h-11 rounded-full border border-white/20 px-3 text-sm font-bold">Agora não</button>
    </div>
    <progress className="mt-3 h-1.5 w-full accent-[#91b8dc]" value={state.step + 1} max={steps.length} aria-label={`Progresso do tutorial: passo ${state.step + 1} de ${steps.length}`} />
    <h2 className="mt-3 text-xl font-black" tabIndex={-1}>{step.title}</h2>
    <p className="mt-2 text-sm leading-6 text-ink/75">{step.description}</p>
    {step.actionLabel && step.onAction && <button type="button" onClick={step.onAction} className="mt-4 min-h-11 rounded-full border border-brand-blue/50 px-4 py-2 text-sm font-bold text-brand-blue">{step.actionLabel}</button>}
    <div className="mt-4 flex flex-wrap items-center justify-between gap-2">
      <button type="button" disabled={state.step === 0} onClick={() => update({ status: 'in_progress', step: Math.max(0, state.step - 1) })} className="min-h-11 rounded-full border border-white/20 px-4 py-2 text-sm font-bold disabled:opacity-40">Voltar</button>
      <button type="button" onClick={() => { update({ status: 'skipped', step: state.step }); setVisible(false); }} className="min-h-11 border-0 bg-transparent px-3 text-sm font-semibold text-ink/65 underline">Pular tutorial</button>
      <button type="button" onClick={() => {
        if (state.step + 1 >= steps.length) { update({ status: 'completed', step: steps.length - 1 }); setVisible(false); }
        else update({ status: 'in_progress', step: state.step + 1 });
      }} className="min-h-11 rounded-full bg-brand-blue px-5 py-2 text-sm font-extrabold text-[#101725]">{state.step + 1 >= steps.length ? 'Concluir' : 'Próximo'}</button>
    </div>
  </aside>;
}
