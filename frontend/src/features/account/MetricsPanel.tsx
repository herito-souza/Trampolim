import { useEffect, useMemo, useState } from 'react';
import { api } from './api';

interface Metric { day: string; kind: 'course' | 'job'; eventType: 'open' | 'apply'; itemTitle: string; total: number }

export function MetricsPanel({ token }: { token: string }) {
  const [rows, setRows] = useState<Metric[]>([]);
  const [error, setError] = useState('');
  useEffect(() => { api<Metric[]>('/admin/metrics', token).then(setRows).catch(cause => setError(cause.message)); }, [token]);
  const totals = useMemo(() => rows.reduce((result, row) => {
    const key = `${row.kind}-${row.eventType}`;
    result[key] = (result[key] || 0) + row.total;
    return result;
  }, {} as Record<string, number>), [rows]);
  const popularItems = useMemo(() => Object.values(rows.reduce((result, row) => {
    if (row.eventType !== 'open') return result;
    const key = `${row.kind}:${row.itemTitle}`;
    result[key] ??= { kind: row.kind, title: row.itemTitle, total: 0 };
    result[key].total += row.total;
    return result;
  }, {} as Record<string, { kind: string; title: string; total: number }>)).sort((a, b) => b.total - a.total).slice(0, 5), [rows]);
  return <section aria-labelledby="metrics-title" className="mb-8 rounded-3xl border border-ink/10 bg-brand-white p-5 shadow-sm md:p-7">
    <div className="mb-5"><p className="text-xs font-extrabold uppercase tracking-widest text-brand-green">Visão agregada · últimos 30 dias</p><h2 id="metrics-title" className="mt-1 text-2xl font-black">Interesse no catálogo</h2><p className="mt-1 text-sm text-ink/60">Contagens de aberturas e ações. Nenhum identificador de usuário é armazenado.</p></div>
    {error ? <p role="status" className="text-sm text-ink/60">Métricas indisponíveis: {error}</p> : <><div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">{[['Aberturas de cursos', 'course-open'], ['Acompanhamentos de vagas', 'job-apply'], ['Aberturas de vagas', 'job-open']].map(([label, key]) => <div key={key} className="rounded-2xl bg-paper p-4"><p className="text-xs font-bold text-ink/60">{label}</p><p className="mt-1 text-3xl font-black">{totals[key] || 0}</p></div>)}<div className="rounded-2xl bg-paper p-4"><p className="text-xs font-bold text-ink/60">Eventos registrados</p><p className="mt-1 text-3xl font-black">{rows.reduce((sum, row) => sum + row.total, 0)}</p></div></div><div className="mt-5 grid gap-5 lg:grid-cols-2"><section><h3 className="mb-2 font-extrabold">Itens mais abertos</h3>{popularItems.length ? <ol className="space-y-2">{popularItems.map((item, index) => <li key={`${item.kind}-${item.title}`} className="flex items-center justify-between gap-3 rounded-xl bg-paper px-4 py-3 text-sm"><span><strong className="mr-2 text-brand-red">{index + 1}.</strong>{item.title} <span className="text-ink/50">· {item.kind === 'job' ? 'vaga' : 'curso'}</span></span><strong>{item.total}</strong></li>)}</ol> : <p className="rounded-xl bg-paper p-4 text-sm text-ink/60">Ainda não há aberturas registradas.</p>}</section><section className="overflow-x-auto"><table className="w-full min-w-[520px] text-left text-sm"><caption className="sr-only">Atividade diária de cursos e vagas</caption><thead><tr className="border-b border-ink/10 text-xs uppercase tracking-wide text-ink/55"><th className="py-2 pr-3">Dia</th><th className="py-2 pr-3">Item</th><th className="py-2 pr-3">Ação</th><th className="py-2 text-right">Total</th></tr></thead><tbody>{rows.slice(-10).reverse().map((row, index) => <tr key={`${row.day}-${row.kind}-${row.eventType}-${row.itemTitle}-${index}`} className="border-b border-ink/5"><td className="py-2 pr-3">{new Date(`${row.day}T12:00:00`).toLocaleDateString('pt-BR')}</td><td className="max-w-48 truncate py-2 pr-3" title={row.itemTitle}>{row.itemTitle}</td><td className="py-2 pr-3">{row.eventType === 'open' ? 'Aberturas' : 'Acompanhamentos'}</td><td className="py-2 text-right font-bold">{row.total}</td></tr>)}</tbody></table></section></div></>}
  </section>;
}
