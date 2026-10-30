import { useEffect, useState } from 'react';
import { api } from '../account/api';
import { radarButton as button, radarField as field } from '../radar/radarUi';
import { Result } from './CareerResult';
import type { Json } from './CareerResult';
type Item = {
    id: string;
    title: string;
    kind: string;
    description: string;
};
type Evidence = {
    receipt_id: string;
    name: string;
    course_title: string;
};
type Share = {
    id: string;
    recipient_email: string;
    expires_at: string;
    revoked_at: string | null;
};
export function PortfolioPanel({ token }: {
    token: string;
}) { const [items, setItems] = useState<Item[]>([]), [evidence, setEvidence] = useState<Evidence[]>([]), [shares, setShares] = useState<Share[]>([]), [chosen, setChosen] = useState<string[]>([]), [receipts, setReceipts] = useState<string[]>([]), [kind, setKind] = useState('PROJECT'), [title, setTitle] = useState(''), [description, setDescription] = useState(''), [url, setUrl] = useState(''), [recipient, setRecipient] = useState(''), [alias, setAlias] = useState(''), [days, setDays] = useState(7), [consent, setConsent] = useState(false), [link, setLink] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false); async function refresh() { const [i, e, s] = await Promise.all([api<Item[]>('/career/portfolio/items', token), api<Evidence[]>('/career/portfolio/evidence', token), api<Share[]>('/career/portfolio/shares', token)]); setItems(i); setEvidence(e); setShares(s); } useEffect(() => { Promise.all([api<Item[]>('/career/portfolio/items', token), api<Evidence[]>('/career/portfolio/evidence', token), api<Share[]>('/career/portfolio/shares', token)]).then(([i, e, s]) => { setItems(i); setEvidence(e); setShares(s); }).catch(e => setError(e.message)); }, [token]); async function run(action: () => Promise<void>) { setBusy(true); setError(''); try {
    await action();
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Falha no portfólio.');
}
finally {
    setBusy(false);
} } function toggle(id: string, list: string[], set: (v: string[]) => void) { set(list.includes(id) ? list.filter(x => x !== id) : [...list, id]); setConsent(false); } return <section className="space-y-5"><h2 className="text-2xl font-black">Meu portfólio profissional</h2><p>Experiências, cursos e projetos manuais são declarações. Evidências educacionais importadas são revalidadas no Capacita. Você escolhe os itens, destinatário e validade de cada compartilhamento.</p><fieldset className="space-y-3 rounded-2xl border border-white/15 p-5"><legend>Adicionar item declarado</legend><label>Tipo<select className={field} value={kind} onChange={e => setKind(e.target.value)}><option value="PROJECT">Projeto</option><option value="EXPERIENCE">Experiência</option><option value="COURSE">Curso declarado</option></select></label><label className="block">Título<input className={field} maxLength={180} value={title} onChange={e => setTitle(e.target.value)}/></label><label className="block">Descrição<textarea className={field} maxLength={4000} value={description} onChange={e => setDescription(e.target.value)}/></label><label className="block">Link HTTPS (opcional)<input type="url" className={field} value={url} onChange={e => setUrl(e.target.value)}/></label><button className={button} disabled={busy || !title || !description} onClick={() => run(async () => { await api('/career/portfolio/items', token, { method: 'POST', body: JSON.stringify({ kind, title, description, url }) }); setTitle(''); setDescription(''); setUrl(''); await refresh(); })}>Salvar item</button></fieldset><fieldset className="space-y-3"><legend className="text-xl font-bold">Selecionar o que compartilhar</legend>{items.map(i => <div key={i.id} className="rounded-xl border border-white/15 p-4"><label className="flex gap-3"><input type="checkbox" checked={chosen.includes(i.id)} onChange={() => toggle(i.id, chosen, setChosen)}/>{i.title} · {i.kind === 'CHALLENGE' ? 'referência de desafio (evidência revalidada ao compartilhar)' : 'declarado'}</label><p>{i.description}</p><button className={button} disabled={busy} onClick={() => run(async () => { await api('/career/portfolio/items/' + i.id, token, { method: 'DELETE' }); setChosen(chosen.filter(x => x !== i.id)); await refresh(); })}>Excluir item</button></div>)}{evidence.map(e => <label key={e.receipt_id} className="flex gap-3"><input type="checkbox" checked={receipts.includes(e.receipt_id)} onChange={() => toggle(e.receipt_id, receipts, setReceipts)}/>{e.name} · {e.course_title} · evidência importada</label>)}{!evidence.length && <p>Sem evidências importadas. Utilize o Radar e a autorização de competências do Capacita para vincular comprovantes.</p>}</fieldset><label className="block">Apresentação escolhida<input className={field} maxLength={80} value={alias} onChange={e => { setAlias(e.target.value); setConsent(false); }}/></label><label className="block">E-mail da conta empresarial do recrutador<input type="email" className={field} value={recipient} onChange={e => { setRecipient(e.target.value); setConsent(false); }}/></label><label className="block">Validade em dias (1 a 30)<input type="number" min={1} max={30} className={field} value={days} onChange={e => { setDays(Number(e.target.value)); setConsent(false); }}/></label><label className="flex gap-3"><input type="checkbox" checked={consent} onChange={e => setConsent(e.target.checked)}/>Autorizo compartilhar apenas os itens selecionados com este destinatário pelo prazo informado.</label><button className={button} disabled={busy || !consent || !recipient || !chosen.length && !receipts.length} onClick={() => run(async () => { const s = await api<{
    token: string;
}>('/career/portfolio/shares', token, { method: 'POST', body: JSON.stringify({ items: chosen, evidenceReceipts: receipts, recipientEmail: recipient, alias, days, consent }) }); setLink(window.location.origin + '/?portfolio=' + s.token + '#empresas'); setConsent(false); await refresh(); })}>Gerar acesso para o recrutador</button>{link && <label className="block">Link criado (guarde agora)<input readOnly className={field} value={link} onFocus={e => e.target.select()}/></label>}<h3 className="text-xl font-bold">Meus compartilhamentos</h3>{shares.map(s => <div key={s.id} className="rounded-xl border border-white/15 p-4"><p>{s.recipient_email} · até {new Date(s.expires_at).toLocaleString('pt-BR')} · {s.revoked_at ? 'Revogado' : 'Consulte a validade'}</p><button className={button} disabled={busy || !!s.revoked_at} onClick={() => run(async () => { await api('/career/portfolio/shares/' + s.id, token, { method: 'DELETE' }); setLink(''); await refresh(); })}>Revogar acesso</button></div>)}{error && <p role="alert">{error}</p>}</section>; }
export function RecruiterPortfolio({ token, initial = '', onDone }: {
    token: string;
    initial?: string;
    onDone?: () => void;
}) { const [code, setCode] = useState(initial), [result, setResult] = useState<Json>(null), [error, setError] = useState(''), [busy, setBusy] = useState(false); return <section className="mx-auto max-w-5xl space-y-5 p-6"><h1 className="text-3xl font-black">Portfólio autorizado</h1><p>Somente a conta empresarial indicada pelo candidato pode consultar os itens selecionados.</p><label>Código de acesso<input className={field} value={code} onChange={e => setCode(e.target.value)}/></label><button className={button} disabled={busy || !code} onClick={async () => { setBusy(true); setError(''); setResult(null); try {
    setResult(await api('/company/career/portfolio/verify', token, { method: 'POST', body: JSON.stringify({ token: code }) }));
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Acesso indisponível.');
}
finally {
    setBusy(false);
} }}>Consultar com minha conta empresarial</button>{onDone && <button className={button} onClick={onDone}>Voltar à empresa</button>}{error && <p role="alert">{error}</p>}{result && <Result value={result}/>}</section>; }
