import { InterviewAudioPractice } from './InterviewAudioPractice';
import { CareerDataDownload, OutcomeConfirmation } from './CareerExtras';
import { useEffect, useState } from 'react';
import { api } from '../account/api';
import { AccountNavigation } from '../account/AccountNavigation';
import type { AccountDestination } from '../account/AccountNavigation';
import { AccountHeaderActions } from '../account/AccountHeaderActions';
import { radarButton as button, radarField as field } from '../radar/radarUi';
import { ObservatoryPanel } from './ObservatoryPanel';
import { ProjectsPanel } from './ProjectsPanel';
import { PortfolioPanel } from './PortfolioPanel';
import { YoungOpportunities } from './YoungOpportunities';
import { Result } from './CareerResult';
import type { Json } from './CareerResult';
export function CareerPanel({ token, onNavigate, onLogout, canManage = false }: {
    token: string;
    onNavigate: (d: AccountDestination) => void;
    onLogout: () => void;
    canManage?: boolean;
}) { const [tab, setTab] = useState('mentor'); return <><header className="flex flex-wrap items-center justify-between gap-3 border-b border-white/15 px-[6vw] py-5"><p className="text-xl font-black">Trampolim Oportunidades</p><AccountHeaderActions onProfile={() => onNavigate('profile')} onLogout={onLogout}/></header><AccountNavigation active="career" onNavigate={onNavigate} canManage={canManage}/><main className="mx-auto min-h-screen max-w-7xl space-y-6 px-[6vw] py-10"><h1 className="text-4xl font-black">Carreira e projetos</h1><p>Prepare seu próximo passo, acompanhe evidências e escolha o que compartilhar.</p><CareerDataDownload token={token}/><nav aria-label="Módulos de carreira" className="flex flex-wrap gap-2">{[['mentor', 'Mentor e entrevistas'], ['first', 'Primeiro emprego'], ['projects', 'Desafios práticos'], ['portfolio', 'Meu portfólio'], ['young', 'Estágios e aprendizagem'], ['observatory', 'Observatório regional']].map(([id, name]) => <button key={id} className={button} aria-current={tab === id ? 'page' : undefined} onClick={() => setTab(id)}>{name}</button>)}</nav>{tab === 'mentor' && <MentorPanel token={token}/>} {tab === 'first' && <><FirstJob token={token}/><OutcomeConfirmation token={token}/></>} {tab === 'projects' && <ProjectsPanel token={token}/>} {tab === 'portfolio' && <PortfolioPanel token={token}/>} {tab === 'young' && <YoungOpportunities token={token}/>} {tab === 'observatory' && <ObservatoryPanel token={token}/>}</main></>; }
function MentorPanel({ token }: {
    token: string;
}) { const [topic, setTopic] = useState('RESUME'), [text, setText] = useState(''), [ai, setAi] = useState(false), [consent, setConsent] = useState(false), [requestKey, setRequestKey] = useState(() => crypto.randomUUID()), [available, setAvailable] = useState(false), [result, setResult] = useState<Json>(null), [error, setError] = useState(''), [busy, setBusy] = useState(false); useEffect(() => { let live = true; api<{
    available: boolean;
}>('/career/mentor/status', token).then(s => { if (live)
    setAvailable(s.available); }).catch(e => { if (live)
    setError(e.message); }); return () => { live = false; }; }, [token]); async function ask() { setBusy(true); setError(''); try {
    setResult(await api<Json>('/career/mentor', token, { method: 'POST', body: JSON.stringify({ clientKey: requestKey, topic, text, useAi: ai, consent }) }));
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Falha na consulta.');
}
finally {
    setBusy(false);
} } return <section className="space-y-5"><h2 className="text-2xl font-black">Mentor de carreira</h2><p>Revise apenas o texto que deseja enviar. Não inclua documentos, senhas ou dados sensíveis. A orientação requer revisão humana e não garante contratação.</p><label>Tema<select className={field} value={topic} onChange={e => { setTopic(e.target.value); setRequestKey(crypto.randomUUID()); setConsent(false); }}>{[['RESUME', 'Currículo'], ['INTERVIEW', 'Entrevista'], ['PLAN', 'Planejamento'], ['JOB', 'Requisitos de vaga'], ['TRAINING', 'Capacitação'], ['SKILLS', 'Competências']].map(([id, name]) => <option key={id} value={id}>{name}</option>)}</select></label><label className="block">Contexto selecionado<textarea className={field} rows={5} maxLength={6000} value={text} onChange={e => { setText(e.target.value); setRequestKey(crypto.randomUUID()); setConsent(false); }}/></label><label className="flex gap-3"><input type="checkbox" disabled={!available} checked={ai} onChange={e => { setAi(e.target.checked); setConsent(false); }}/>Usar IA externa {available ? '' : '(não habilitada; orientação por regras disponível)'}</label>{ai && <label className="flex gap-3"><input type="checkbox" checked={consent} onChange={e => setConsent(e.target.checked)}/>Autorizo enviar somente este texto ao provedor OpenAI. A política de retenção do provedor também se aplica.</label>}<button className={button} disabled={busy || ai && !consent} onClick={ask}>{busy ? 'Consultando…' : 'Receber orientação'}</button>{error && <p role="alert">{error}</p>}{result && <section aria-label="Orientação recebida" aria-live="polite" className="rounded-2xl border border-white/15 p-5"><Result value={result}/></section>}<InterviewPanel token={token}/></section>; }
type Session = {
    id: string;
    area: string;
    occupation: string;
    questions: string[];
    answers: Record<string, string>;
    feedback: Record<string, Json>;
    revision: number;
};
function InterviewPanel({ token }: {
    token: string;
}) { const [area, setArea] = useState(''), [occupation, setOccupation] = useState(''), [sessions, setSessions] = useState<{
    id: string;
    occupation: string;
}[]>([]), [session, setSession] = useState<Session | null>(null), [answer, setAnswer] = useState(''), [question, setQuestion] = useState(0), [error, setError] = useState(''), [busy, setBusy] = useState(false); async function load() { setSessions(await api('/career/interviews', token)); } useEffect(() => { api<{
    id: string;
    occupation: string;
}[]>('/career/interviews', token).then(setSessions).catch(e => setError(e.message)); }, [token]); async function run(action: () => Promise<void>) { setBusy(true); setError(''); try {
    await action();
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Falha na simulação.');
}
finally {
    setBusy(false);
} } return <section className="space-y-4 border-t border-white/15 pt-6"><h2 className="text-2xl font-black">Simulador de entrevista</h2><p>Prática textual sem análise de voz, emoções ou decisão de contratação. O feedback por regras funciona sem provedor externo.</p><label className="block">Área<input className={field} maxLength={80} value={area} onChange={e => setArea(e.target.value)}/></label><label className="block">Ocupação<input className={field} maxLength={180} value={occupation} onChange={e => setOccupation(e.target.value)}/></label><button className={button} disabled={busy || !area || !occupation} onClick={() => run(async () => { setSession(await api('/career/interviews', token, { method: 'POST', body: JSON.stringify({ area, occupation }) })); setQuestion(0); setAnswer(''); await load(); })}>Iniciar prática</button><div className="flex flex-wrap gap-2">{sessions.map(s => <button key={s.id} className={button} onClick={() => run(async () => { setSession(await api('/career/interviews/' + s.id, token)); setQuestion(0); setAnswer(''); })}>Retomar: {s.occupation}</button>)}</div>{session && <div className="space-y-4"><label>Pergunta<select className={field} value={question} onChange={e => { setQuestion(Number(e.target.value)); setAnswer(session.answers[e.target.value] || ''); }}>{session.questions.map((q, i) => <option key={q} value={i}>{i + 1}. {q}</option>)}</select></label><p>{session.questions[question]}</p><InterviewAudioPractice /><label className="block">Sua resposta<textarea rows={5} className={field} maxLength={2500} value={answer} onChange={e => setAnswer(e.target.value)}/></label><button className={button} disabled={busy || !answer.trim()} onClick={() => run(async () => { setSession(await api('/career/interviews/' + session.id + '/answers', token, { method: 'POST', body: JSON.stringify({ question, text: answer, revision: session.revision, clientKey: crypto.randomUUID(), useAi: false, consent: false }) })); await load(); })}>Salvar e receber feedback</button>{session.feedback[String(question)] && <Result value={session.feedback[String(question)]}/>}</div>}{error && <p role="alert">{error}</p>}</section>; }
function FirstJob({ token }: {
    token: string;
}) { const [data, setData] = useState<Json>(null), [step, setStep] = useState('RESUME_PLAN'), [notes, setNotes] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false); useEffect(() => { api<Json>('/career/first-job', token).then(setData).catch(e => setError(e.message)); }, [token]); return <section className="space-y-4"><h2 className="text-2xl font-black">Jornada do primeiro emprego</h2><p>Use o Radar para analisar lacunas e o Capacita para estudar. As contas de cada trilha continuam independentes.</p>{data && <Result value={data}/>}<label>Plano<select className={field} value={step} onChange={e => setStep(e.target.value)}><option value="RESUME_PLAN">Currículo</option><option value="TRAINING_PLAN">Capacitação</option><option value="APPLICATION_PLAN">Candidaturas</option></select></label><label className="block">Meu próximo passo<textarea className={field} maxLength={1000} value={notes} onChange={e => setNotes(e.target.value)}/></label><button className={button} disabled={busy} onClick={async () => { setBusy(true); setError(''); try {
    await api('/career/first-job/' + step, token, { method: 'PUT', body: JSON.stringify({ notes }) });
    setData(await api('/career/first-job', token));
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Falha ao salvar.');
}
finally {
    setBusy(false);
} }}>Salvar plano declarado</button>{error && <p role="alert">{error}</p>}</section>; }
