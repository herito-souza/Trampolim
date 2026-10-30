import { useEffect, useState } from 'react';
import { api } from '../account/api';
import { radarButton as button, radarField as field } from '../radar/radarUi';
import { Result } from './CareerResult';
import type { Json } from './CareerResult';
type Challenge = {
    id: number;
    title: string;
    description: string;
    terms: string;
    materials: string;
    material_url: string;
    rubric: Json;
};
type Submission = {
    receipt_id: string;
    challenge_id: number;
    body: string;
    project_url: string;
    delivery_status: string; withdrawn_at: string | null;
};
export function ProjectsPanel({ token }: {
    token: string;
}) { const [rows, setRows] = useState<Challenge[]>([]), [next, setNext] = useState<number | null>(null), [selected, setSelected] = useState<Challenge | null>(null), [mine, setMine] = useState<Submission[]>([]), [alias, setAlias] = useState(''), [body, setBody] = useState(''), [url, setUrl] = useState(''), [consent, setConsent] = useState(false), [result, setResult] = useState<Json>(null), [error, setError] = useState(''), [busy, setBusy] = useState(false), [receipt, setReceipt] = useState(() => crypto.randomUUID()); async function run(action: () => Promise<void>) { setBusy(true); setError(''); try {
    await action();
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Falha no desafio.');
}
finally {
    setBusy(false);
} } async function page(cursor?: number) { const p = await api<{
    items: Challenge[];
    nextCursor: number | null;
}>('/career/challenges?limit=20' + (cursor ? '&before=' + cursor : ''), token); setRows(p.items); setNext(p.nextCursor); } async function refresh() { setMine(await api('/career/challenges/submissions/mine', token)); } useEffect(() => { let live = true; Promise.all([api<{
        items: Challenge[];
        nextCursor: number | null;
    }>('/career/challenges?limit=20', token), api<Submission[]>('/career/challenges/submissions/mine', token)]).then(([p, m]) => { if (live) {
    setRows(p.items);
    setNext(p.nextCursor);
    setMine(m);
} }).catch(e => { if (live)
    setError(e.message); }); return () => { live = false; }; }, [token]); return <section className="space-y-4"><h2 className="text-2xl font-black">Desafios práticos</h2><p>Participação voluntária com condições e critérios definidos pela empresa. Avaliação empresarial não equivale a certificado ou oferta de emprego. Não envie dados confidenciais de terceiros.</p><div className="grid gap-4 md:grid-cols-2">{rows.map(c => <article key={c.id} className="rounded-2xl border border-white/15 p-5"><h3 className="text-xl font-bold">{c.title}</h3><p>{c.description}</p><button className={button} disabled={busy} onClick={() => run(async () => { setSelected(await api('/career/challenges/' + c.id, token)); setConsent(false); setReceipt(crypto.randomUUID()); setResult(null); })}>Ver condições</button></article>)}</div>{!rows.length && <p>Nenhum desafio publicado disponível.</p>}{next && <button className={button} onClick={() => run(() => page(next))}>Próxima página</button>}{selected && <section className="space-y-4 rounded-2xl border border-white/15 p-5"><h3 className="text-xl font-bold">{selected.title}</h3><p>{selected.description}</p><h4 className="font-bold">Condições de participação</h4><p className="whitespace-pre-wrap">{selected.terms}</p><h4 className="font-bold">Critérios de avaliação</h4><Result value={selected.rubric}/><p>{selected.materials}</p>{selected.material_url && <a href={selected.material_url} target="_blank" rel="noopener noreferrer" className="underline">Abrir material externo</a>}<label className="block">Como deseja se apresentar à empresa<input className={field} maxLength={80} value={alias} onChange={e => setAlias(e.target.value)}/></label><label className="block">Projeto ou resposta<textarea rows={6} className={field} maxLength={12000} value={body} onChange={e => setBody(e.target.value)}/></label><label className="block">Link HTTPS do projeto (opcional)<input className={field} type="url" value={url} onChange={e => setUrl(e.target.value)}/></label><label className="flex gap-3"><input type="checkbox" checked={consent} onChange={e => setConsent(e.target.checked)}/>Li as condições e autorizo enviar somente esta apresentação, resposta e link à empresa responsável.</label><button className={button} disabled={busy || !consent || body.trim().length < 20 || !alias.trim()} onClick={() => run(async () => { setResult(await api('/career/challenges/submissions', token, { method: 'POST', body: JSON.stringify({ receiptId: receipt, challengeId: selected.id, alias, body, projectUrl: url, consent }) })); await refresh(); })}>Revisado: enviar projeto</button></section>}<h3 className="text-xl font-bold">Minhas entregas</h3>{mine.map(s => <div key={s.receipt_id} className="flex flex-wrap gap-3 rounded-xl border border-white/15 p-4"><span>Desafio {s.challenge_id} · {s.withdrawn_at ? 'Retirado' : s.delivery_status === 'DELIVERED' ? 'Entregue à empresa' : s.delivery_status === 'REJECTED' ? 'Entrega recusada; consulte as condições ou solicite suporte' : 'Aguardando entrega à empresa'}</span>{!s.withdrawn_at && <><button className={button} disabled={busy} onClick={() => run(async () => setResult(await api('/career/challenges/submissions/' + s.receipt_id, token)))}>Consultar feedback</button><button className={button} disabled={busy} onClick={() => run(async () => { await api('/career/portfolio/items', token, { method: 'POST', body: JSON.stringify({ kind: 'CHALLENGE', title: 'Projeto do desafio ' + s.challenge_id, description: s.body, url: s.project_url, submissionReceipt: s.receipt_id }) }); setResult({ notice: 'Referência adicionada ao portfólio. Somente uma avaliação ativa da empresa será identificada como evidência ao compartilhar.' }); })}>Adicionar referência ao portfólio</button><button className={button} disabled={busy} onClick={() => run(async () => { await api('/career/challenges/submissions/' + s.receipt_id, token, { method: 'DELETE' }); await refresh(); setResult({ notice: 'Retirada registrada. A empresa receberá a remoção por integração controlada.' }); })}>Retirar entrega</button></>}</div>)}{error && <p role="alert">{error}</p>}{result && <Result value={result}/>}</section>; }
