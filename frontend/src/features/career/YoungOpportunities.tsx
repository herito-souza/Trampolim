import { careerPublic } from './careerApi';
import { useState } from 'react';
import { radarButton as button, radarField as field } from '../radar/radarUi';
import { Result } from './CareerResult';
import type { Json } from './CareerResult';
type Job = {
    id: number;
    title: string;
    company_name: string;
    description: string;
    location: string;
    modality: string;
    company_slug: string;
};
export function YoungOpportunities({ token }: {
    token?: string;
}) { const [regime, setRegime] = useState('INTERNSHIP'), [region, setRegion] = useState(''), [first, setFirst] = useState(false), [rows, setRows] = useState<Job[]>([]), [next, setNext] = useState<number | null>(null), [loaded, setLoaded] = useState(false), [data, setData] = useState<Json>(null), [error, setError] = useState(''), [busy, setBusy] = useState(false); async function load(cursor?: number) { setBusy(true); setError(''); try {
    const p = await careerPublic<{
        items: Job[];
        nextCursor: number | null;
    }>('/public/career/opportunities?regime=' + regime + '&region=' + encodeURIComponent(region) + '&first=' + first + (cursor ? '&before=' + cursor : ''));
    setRows(p.items);
    setNext(p.nextCursor);
    setLoaded(true);
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Consulta indisponível.');
}
finally {
    setBusy(false);
} } return <section className="space-y-4"><h2 className="text-2xl font-black">Estágios, aprendizagem e primeiro emprego</h2><p>Regimes distintos exigem análise documental, educacional e jurídica. O sistema não determina automaticamente sua elegibilidade.</p><label>Regime<select className={field} value={regime} onChange={e => setRegime(e.target.value)}><option value="INTERNSHIP">Estágio</option><option value="APPRENTICESHIP">Aprendizagem profissional</option><option value="EMPLOYMENT">Emprego</option></select></label><label className="block">UF<input className={field} maxLength={2} value={region} onChange={e => setRegion(e.target.value.toUpperCase())}/></label><label className="flex gap-3"><input type="checkbox" checked={first} onChange={e => setFirst(e.target.checked)}/>Somente oportunidades sinalizadas para primeira experiência</label><button className={button} disabled={busy} onClick={() => load()}>Buscar oportunidades publicadas</button> <button className={button} disabled={busy} onClick={async () => { setError(''); try {
    setData(await careerPublic('/public/career/legal-guidance'));
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Orientações indisponíveis.');
} }}>Consultar requisitos gerais e fontes legais</button>{loaded && !rows.length && <p>Nenhuma oportunidade publicada disponível para estes filtros.</p>}{rows.map(j => <article key={j.id} className="space-y-3 rounded-2xl border border-white/15 p-5"><h3 className="text-xl font-bold">{j.title}</h3><p>{j.company_name} · {j.location} · {j.modality}</p><p>{j.description}</p><a className="underline" href={'/?empresa=' + encodeURIComponent(j.company_slug) + '#oportunidades'}>Ver empresa e processo de candidatura</a></article>)}{next && <button className={button} disabled={busy} onClick={() => load(next)}>Próxima página</button>}{token && <p>Use seu perfil para conferir as exigências da vaga antes de se candidatar.</p>}{error && <p role="alert">{error}</p>}{data && <Result value={data}/>}</section>; }
