import { useState } from 'react';
import { api } from '../account/api';
import { radarButton as button, radarField as field } from '../radar/radarUi';
import { Result } from './CareerResult';
import type { Json } from './CareerResult';
export function OutcomeConfirmation({ token }: {
    token: string;
}) { const [code, setCode] = useState(''), [preview, setPreview] = useState<Json>(null), [consent, setConsent] = useState(false), [metrics, setMetrics] = useState(false), [notice, setNotice] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false); async function run(confirm: boolean) { setBusy(true); setError(''); try {
    const result = await api<Json>('/career/outcomes/' + (confirm ? 'confirm' : 'preview'), token, { method: 'POST', body: JSON.stringify({ code, consent: confirm && consent, publicMetricsConsent: metrics }) });
    if (confirm) {
        setNotice('Sua confirmação foi registrada. A autorização de indicadores segue a opção escolhida.');
        setPreview(null);
        setConsent(false);
    }
    else {
        setPreview(result);
        setNotice('');
        setConsent(false);
    }
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Código indisponível.');
}
finally {
    setBusy(false);
} } return <section className="space-y-4 border-t border-white/15 pt-6"><h2 className="text-2xl font-black">Confirmar resultado de seleção</h2><p>Se a empresa lhe forneceu um código, confira os dados antes de confirmar. Sua conta precisa usar o e-mail da candidatura. Isto não substitui documentos trabalhistas.</p><label>Código fornecido pela empresa<input className={field} maxLength={43} value={code} onChange={e => { setCode(e.target.value); setPreview(null); setConsent(false); }}/></label><button className={button} disabled={busy || code.length !== 43} onClick={() => run(false)}>Conferir empresa e oportunidade</button>{preview && <><Result value={preview}/><label className="flex gap-3"><input type="checkbox" checked={consent} onChange={e => setConsent(e.target.checked)}/>Confirmo que o resultado e a data apresentados correspondem à minha contratação.</label><label className="flex gap-3"><input type="checkbox" checked={metrics} onChange={e => setMetrics(e.target.checked)}/>Autorizo também a inclusão em indicadores públicos agregados protegidos (opcional).</label><button className={button} disabled={busy || !consent} onClick={() => run(true)}>Registrar minha confirmação</button></>}{notice && <p role="status">{notice}</p>}{error && <p role="alert">{error}</p>}</section>; }
export function CareerContext({ token }: {
    token: string;
}) { const [listing, setListing] = useState(''), [regime, setRegime] = useState('EMPLOYMENT'), [region, setRegion] = useState(''), [school, setSchool] = useState(''), [institution, setInstitution] = useState(''), [docs, setDocs] = useState(''), [first, setFirst] = useState(false), [legal, setLegal] = useState(false), [notice, setNotice] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false); return <fieldset className="space-y-4 rounded-2xl border border-white/15 p-5"><legend className="text-xl font-bold">Requisitos de emprego, estágio e aprendizagem</legend><p>Use o identificador de uma vaga da sua empresa. Esta configuração complementa o cadastro e preserva a cobrança de R$ 50,00 para divulgação de estágio.</p><label className="block">Identificador da vaga<input className={field} type="number" min={1} value={listing} onChange={e => setListing(e.target.value)}/></label><label className="block">Regime<select className={field} value={regime} onChange={e => setRegime(e.target.value)}><option value="EMPLOYMENT">Emprego</option><option value="INTERNSHIP">Estágio</option><option value="APPRENTICESHIP">Aprendizagem profissional</option></select></label><label className="block">UF<input className={field} maxLength={2} value={region} onChange={e => setRegion(e.target.value.toUpperCase())}/></label><label className="block">Requisitos escolares<textarea className={field} maxLength={1000} value={school} onChange={e => setSchool(e.target.value)}/></label><label className="block">Instituição envolvida (quando aplicável)<input className={field} maxLength={180} value={institution} onChange={e => setInstitution(e.target.value)}/></label><label className="block">Documentação e validações necessárias<textarea className={field} maxLength={2000} value={docs} onChange={e => setDocs(e.target.value)}/></label><label className="flex gap-3"><input type="checkbox" checked={first} onChange={e => setFirst(e.target.checked)}/>Oportunidade apropriada à primeira experiência</label><label className="flex gap-3"><input type="checkbox" checked={legal} onChange={e => setLegal(e.target.checked)}/>A empresa declara ter conferido as exigências legais e educacionais do regime, sujeitas a revisão humana.</label><button className={button} disabled={busy || !listing || region.length !== 2} onClick={async () => { setBusy(true); setError(''); setNotice(''); try {
    await api('/company/listings/' + listing + '/career-context', token, { method: 'PUT', body: JSON.stringify({ regime, regionCode: region, schoolRequirements: school, institution, documentation: docs, firstExperience: first, employerLegalConfirmation: legal }) });
    setNotice('Requisitos complementares salvos. As regras de pagamento e publicação continuam sendo verificadas.');
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Falha ao salvar.');
}
finally {
    setBusy(false);
} }}>Salvar requisitos complementares</button>{notice && <p role="status">{notice}</p>}{error && <p role="alert">{error}</p>}</fieldset>; }
export function CareerCostForm({ token }: {
    token: string;
}) { const [company, setCompany] = useState(''), [month, setMonth] = useState(''), [amount, setAmount] = useState(''), [source, setSource] = useState(''), [message, setMessage] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false); return <fieldset className="space-y-3 rounded-2xl border border-white/15 p-5"><legend className="text-xl font-bold">Custo operacional por empresa</legend><p>Registro administrativo com origem verificável. Não cria cobrança ou pagamento.</p><label className="block">ID da empresa<input type="number" min={1} className={field} value={company} onChange={e => setCompany(e.target.value)}/></label><label className="block">Mês<input type="month" className={field} value={month} onChange={e => setMonth(e.target.value)}/></label><label className="block">Valor (R$)<input type="number" min={0} step="0.01" className={field} value={amount} onChange={e => setAmount(e.target.value)}/></label><label className="block">Origem verificável do valor<textarea className={field} maxLength={500} value={source} onChange={e => setSource(e.target.value)}/></label><button className={button} disabled={busy || !company || !month || !amount || source.length < 10} onClick={async () => { setBusy(true); setError(''); setMessage(''); try {
    await api('/admin/career/costs', token, { method: 'POST', body: JSON.stringify({ companyId: Number(company), month, amountCents: Math.round(Number(amount) * 100), source }) });
    setMessage('Custo informado registrado; nenhuma cobrança foi criada.');
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Falha no registro.');
}
finally {
    setBusy(false);
} }}>Registrar custo revisado</button>{message && <p role="status">{message}</p>}{error && <p role="alert">{error}</p>}</fieldset>; }
export function CareerDataDownload({ token }: {
    token: string;
}) { const [busy, setBusy] = useState(false), [error, setError] = useState(''); return <div><button className={button} disabled={busy} onClick={async () => { setBusy(true); setError(''); try {
    const data = await api('/career/account/export', token);
    const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = 'trampolim-meus-dados-de-carreira.json';
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Exportação indisponível.');
}
finally {
    setBusy(false);
} }}>Baixar meus dados de carreira</button>{error && <p role="alert">{error}</p>}</div>; }
