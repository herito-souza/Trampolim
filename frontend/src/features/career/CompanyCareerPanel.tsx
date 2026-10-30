import { CareerContext } from './CareerExtras';
import { useEffect, useState } from 'react';
import { api } from '../account/api';
import { radarButton as button, radarField as field } from '../radar/radarUi';
import { Result } from './CareerResult';
import type { Json } from './CareerResult';
import { RecruiterPortfolio } from './PortfolioPanel';
import { ObservatoryPanel } from './ObservatoryPanel';
type Criterion = {
    key: string;
    name: string;
    weight: number;
};
type Challenge = {
    id: number;
    title: string;
    description: string;
    skills: {
        skillCode: string;
        level: number;
    }[];
    rubric: {
        criteria: Criterion[];
        passPercent: number;
    };
    materials: string;
    material_url: string;
    terms: string;
    deadline: string;
    status: string;
    revision: number;
};
type Submission = {
    receipt_id: string;
    alias: string;
    body: string;
    project_url: string;
    status: string;
    rubric: {
        criteria: Criterion[];
    };
    feedback: string | null;
};
export function CompanyCareerPanel({ token, canEdit }: {
    token: string;
    canEdit: boolean;
}) { const [tab, setTab] = useState('challenges'); return <section className="space-y-5"><h2 className="text-3xl font-black">Desafios e impacto</h2><nav aria-label="Módulos empresariais de carreira" className="flex flex-wrap gap-2">{[['challenges', 'Desafios práticos'], ['outcomes', 'Resultados de seleção'], ['impact', 'Impacto observado'], ['portfolio', 'Portfólios autorizados'], ['observatory', 'Observatório regional']].map(([id, name]) => <button className={button} key={id} aria-current={tab === id ? 'page' : undefined} onClick={() => setTab(id)}>{name}</button>)}</nav>{tab === 'challenges' && <ChallengeManager token={token} canEdit={canEdit}/>} {tab === 'outcomes' && canEdit && <><Outcomes token={token}/><CareerContext token={token}/></>} {tab === 'impact' && <Impact token={token}/>} {tab === 'portfolio' && canEdit && <RecruiterPortfolio token={token}/>} {tab === 'observatory' && <ObservatoryPanel />}</section>; }
function Impact({ token }: {
    token: string;
}) { const [data, setData] = useState<Json>(null), [error, setError] = useState(''); useEffect(() => { api<Json>('/company/career/impact', token).then(setData).catch(e => setError(e.message)); }, [token]); return <section><h3 className="text-2xl font-bold">Resultados registrados da empresa</h3>{error && <p role="alert">{error}</p>}{data ? <Result value={data}/> : !error && <p role="status">Consultando registros…</p>}</section>; }
function ChallengeManager({ token, canEdit }: {
    token: string;
    canEdit: boolean;
}) { const [rows, setRows] = useState<{
    id: number;
    title: string;
    status: string;
}[]>([]), [next, setNext] = useState<number | null>(null), [current, setCurrent] = useState<Challenge | null>(null), [editing, setEditing] = useState(false), [submissionNext, setSubmissionNext] = useState<string | null>(null), [submissions, setSubmissions] = useState<Submission[]>([]), [selected, setSelected] = useState<Submission | null>(null), [scores, setScores] = useState<Record<string, number>>({}), [feedback, setFeedback] = useState(''), [title, setTitle] = useState(''), [description, setDescription] = useState(''), [skills, setSkills] = useState<{
    code: string;
    name: string;
}[]>([]), [skill, setSkill] = useState(''), [terms, setTerms] = useState(''), [materials, setMaterials] = useState(''), [url, setUrl] = useState(''), [deadline, setDeadline] = useState(''), [criteria, setCriteria] = useState<Criterion[]>([{ key: 'quality', name: 'Qualidade da solução', weight: 100 }]), [status, setStatus] = useState('DRAFT'), [error, setError] = useState(''), [notice, setNotice] = useState(''), [busy, setBusy] = useState(false), [reviewKey, setReviewKey] = useState(() => crypto.randomUUID()); async function load(cursor?: number) { const p = await api<{
    items: {
        id: number;
        title: string;
        status: string;
    }[];
    nextCursor: number | null;
}>('/company/challenges?limit=20' + (cursor ? '&before=' + cursor : ''), token); setRows(p.items); setNext(p.nextCursor); } useEffect(() => { Promise.all([api<{
        items: {
            id: number;
            title: string;
            status: string;
        }[];
        nextCursor: number | null;
    }>('/company/challenges?limit=20', token), api<{
        items: {
            code: string;
            name: string;
        }[];
    }>('/public/skills?limit=50', token)]).then(([p, s]) => { setRows(p.items); setNext(p.nextCursor); setSkills(s.items); }).catch(e => setError(e.message)); }, [token]); async function run(action: () => Promise<void>) { setBusy(true); setError(''); setNotice(''); try {
    await action();
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Operação indisponível.');
}
finally {
    setBusy(false);
} } function edit(c: Challenge | null) { setCurrent(c); setTitle(c?.title || ''); setDescription(c?.description || ''); setSkill(c?.skills[0]?.skillCode || skills[0]?.code || ''); setTerms(c?.terms || ''); setMaterials(c?.materials || ''); setUrl(c?.material_url || ''); setDeadline(c?.deadline ? new Date(c.deadline).toISOString().slice(0, 16) : ''); setCriteria(c?.rubric.criteria || [{ key: 'quality', name: 'Qualidade da solução', weight: 100 }]); setStatus(c?.status || 'DRAFT'); setEditing(true); setSubmissions([]); setSelected(null); } return <section className="space-y-4"><p>Desafios possuem critérios transparentes e participação voluntária. Informe finalidade, uso do projeto e condições; não use entregas como trabalho produtivo não remunerado.</p>{canEdit && <button className={button} onClick={() => edit(null)}>Novo desafio</button>}{rows.map(c => <div key={c.id} className="flex flex-wrap items-center gap-3 rounded-xl border border-white/15 p-4"><strong>{c.title} · <Result value={c.status}/></strong><button className={button} disabled={busy} onClick={() => run(async () => edit(await api('/company/challenges/' + c.id, token)))}>Detalhes e edição</button>{canEdit && <button className={button} disabled={busy} onClick={() => run(async () => { setCurrent(await api('/company/challenges/' + c.id, token)); const page = await api<{
    items: Submission[];
    nextCursor: string | null;
}>('/company/challenges/' + c.id + '/submissions', token); setSubmissions(page.items); setSubmissionNext(page.nextCursor); setEditing(false); setSelected(null); })}>Entregas privadas</button>}</div>)}{!rows.length && <p>Nenhum desafio cadastrado.</p>}{next && <button className={button} onClick={() => run(() => load(next))}>Próxima página</button>}{editing && <fieldset disabled={!canEdit || busy} className="space-y-4 rounded-2xl border border-white/15 p-5"><legend>{current ? 'Editar desafio' : 'Criar desafio'}</legend><label className="block">Título<input className={field} value={title} maxLength={180} onChange={e => setTitle(e.target.value)}/></label><label className="block">Descrição<textarea className={field} rows={4} value={description} maxLength={5000} onChange={e => setDescription(e.target.value)}/></label><label className="block">Competência principal<select className={field} value={skill} onChange={e => setSkill(e.target.value)}><option value="">Selecione</option>{skills.map(s => <option key={s.code} value={s.code}>{s.name}</option>)}</select></label><h3 className="font-bold">Critérios e pesos (soma 100)</h3>{criteria.map((c, i) => <div key={c.key} className="grid gap-3 sm:grid-cols-2"><label>Critério {i + 1}<input className={field} value={c.name} onChange={e => setCriteria(criteria.map((x, n) => n === i ? { ...x, name: e.target.value } : x))}/></label><label>Peso (%)<input className={field} type="number" min={1} max={100} value={c.weight} onChange={e => setCriteria(criteria.map((x, n) => n === i ? { ...x, weight: Number(e.target.value) } : x))}/></label></div>)}<button type="button" className={button} disabled={criteria.length >= 10} onClick={() => setCriteria([...criteria, { key: 'criterion-' + (criteria.length + 1), name: '', weight: 1 }])}>Adicionar critério</button><label className="block">Condições de participação e uso das entregas<textarea className={field} rows={4} value={terms} maxLength={3000} onChange={e => setTerms(e.target.value)}/></label><label className="block">Materiais e instruções<textarea className={field} value={materials} maxLength={3000} onChange={e => setMaterials(e.target.value)}/></label><label className="block">Link HTTPS de material (opcional)<input className={field} type="url" value={url} onChange={e => setUrl(e.target.value)}/></label><label className="block">Prazo (UTC)<input className={field} type="datetime-local" value={deadline} onChange={e => setDeadline(e.target.value)}/></label><label className="block">Situação<select aria-label="Situação do desafio" className={field} value={status} onChange={e => setStatus(e.target.value)}><option value="DRAFT">Rascunho</option><option value="PUBLISHED">Publicado</option><option value="CLOSED">Encerrado</option></select></label><button className={button} disabled={!skill || !deadline || !title || terms.length < 20} onClick={() => run(async () => { const saved = await api<Challenge>('/company/challenges' + (current ? '/' + current.id : ''), token, { method: current ? 'PUT' : 'POST', body: JSON.stringify({ title, description, skills: current?.skills?.length ? current.skills : [{ skillCode: skill, level: 2 }], criteria, passPercent: current?.rubric.passPercent || 70, materials, materialUrl: url, terms, deadline: deadline + ':00Z', status, revision: current?.revision || 0 }) }); edit(saved); await load(); setNotice('Desafio salvo com os critérios informados.'); })}>Salvar desafio</button></fieldset>}{!editing && current && <h3 className="text-xl font-bold">Entregas: {current.title}</h3>}{submissions.map(s => <article key={s.receipt_id} className="space-y-3 rounded-xl border border-white/15 p-5"><h4 className="font-bold">{s.alias || 'Apresentação retirada'} · <Result value={s.status}/></h4><p className="whitespace-pre-wrap">{s.body}</p>{s.project_url && <a className="underline" href={s.project_url} target="_blank" rel="noopener noreferrer">Abrir projeto externo</a>}{s.feedback && <p>Feedback: {s.feedback}</p>}{s.status === 'RECEIVED' && <button className={button} onClick={() => { setSelected(s); setFeedback(''); setScores(Object.fromEntries(s.rubric.criteria.map(c => [c.key, 0]))); setReviewKey(crypto.randomUUID()); }}>Avaliar entrega</button>}</article>)}{submissionNext && current && <button className={button} disabled={busy} onClick={() => run(async () => { const page = await api<{
    items: Submission[];
    nextCursor: string | null;
}>('/company/challenges/' + current.id + '/submissions?before=' + submissionNext, token); setSubmissions(page.items); setSubmissionNext(page.nextCursor); setSelected(null); })}>Próxima página de entregas</button>}{selected && current && <fieldset className="space-y-4 rounded-xl border border-white/15 p-5"><legend>Avaliação de {selected.alias}</legend>{selected.rubric.criteria.map(c => <label key={c.key} className="block">{c.name} · peso {c.weight}%<input className={field} type="number" min={0} max={100} value={scores[c.key] ?? 0} onChange={e => setScores({ ...scores, [c.key]: Number(e.target.value) })}/></label>)}<label className="block">Feedback<textarea className={field} maxLength={3000} value={feedback} onChange={e => setFeedback(e.target.value)}/></label><p>A avaliação registrada será preservada. Ela não constitui certificado educacional.</p><button className={button} disabled={busy || feedback.trim().length < 10} onClick={() => run(async () => { await api('/company/challenges/' + current.id + '/submissions/' + selected.receipt_id + '/review', token, { method: 'POST', body: JSON.stringify({ clientKey: reviewKey, scores, feedback }) }); const page = await api<{
    items: Submission[];
    nextCursor: string | null;
}>('/company/challenges/' + current.id + '/submissions', token); setSubmissions(page.items); setSubmissionNext(page.nextCursor); setSelected(null); setNotice('Avaliação registrada.'); })}>Confirmar avaliação revisada</button></fieldset>}{notice && <p role="status">{notice}</p>}{error && <p role="alert">{error}</p>}</section>; }
function Outcomes({ token }: {
    token: string;
}) { const [rows, setRows] = useState<{
    id: number;
    candidate_name: string;
    title: string;
}[]>([]), [id, setId] = useState(''), [interview, setInterview] = useState(''), [held, setHeld] = useState(false), [hire, setHire] = useState(''), [code, setCode] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false), [key, setKey] = useState(() => crypto.randomUUID()); useEffect(() => { api<typeof rows>('/company/career/applications', token).then(setRows).catch(e => setError(e.message)); }, [token]); return <section className="space-y-4"><h3 className="text-2xl font-bold">Resultados de seleção</h3><p>Até 100 candidaturas recentes da sua empresa. Agendamento, entrevista realizada, relato de contratação e confirmação do candidato são registros distintos.</p><label>Candidatura<select className={field} value={id} onChange={e => { setId(e.target.value); setCode(''); setKey(crypto.randomUUID()); }}><option value="">Selecione</option>{rows.map(a => <option key={a.id} value={a.id}>{a.candidate_name} · {a.title}</option>)}</select></label><label className="block">Data/hora da entrevista (UTC, opcional)<input className={field} type="datetime-local" value={interview} onChange={e => { setInterview(e.target.value); setKey(crypto.randomUUID()); }}/></label><label className="flex gap-3"><input type="checkbox" checked={held} onChange={e => { setHeld(e.target.checked); setKey(crypto.randomUUID()); }}/>Confirmo que a entrevista ocorreu (relato empresarial)</label><label className="block">Data de contratação ocorrida (opcional)<input className={field} type="date" value={hire} onChange={e => { setHire(e.target.value); setKey(crypto.randomUUID()); }}/></label><button className={button} disabled={busy || !id || !interview && !hire} onClick={async () => { setBusy(true); setError(''); try {
    const result = await api<{
        confirmationCode?: string;
    }>('/company/career/applications/' + id + '/outcome', token, { method: 'POST', body: JSON.stringify({ requestKey: key, interviewAt: interview ? interview + ':00Z' : null, interviewHeld: held, hireDate: hire || null }) });
    setCode(result.confirmationCode || '');
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Falha no registro.');
}
finally {
    setBusy(false);
} }}>Confirmar relato revisado</button>{code && <label className="block">Código para o candidato confirmar na própria conta (guarde agora)<input readOnly className={field} value={code} onFocus={e => e.target.select()}/></label>}{error && <p role="alert">{error}</p>}</section>; }
