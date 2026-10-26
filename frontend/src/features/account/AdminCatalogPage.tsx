import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import type { CatalogItem } from './api';
import { api } from './api';
import { MetricsPanel } from './MetricsPanel';
import type { ReactNode } from 'react';
import { AccountHeaderActions } from './AccountHeaderActions';
import { RibbonStrip } from '../../components/ui/RibbonStrip';

interface Props { token: string; onBack: () => void; onLogout: () => void; onProfile: () => void; navigation: ReactNode }
interface CatalogReport { id: number; itemId: number; itemTitle: string; kind: Kind; reason: string; details: string | null; status: string; createdAt: string }
type Kind = 'job' | 'course';
type CatalogForm = { kind: Kind; title: string; provider: string; description: string; url: string; area: string; location: string; latitude: string; longitude: string; modality: string; experienceLevel: string; contractType: string; workSchedule: string; salary: string; accessibility: string; expiresAt: string | null; active: boolean };
const empty = (kind: Kind): CatalogForm => ({ kind, title: '', provider: '', description: '', url: '', area: 'service', location: '', latitude: '', longitude: '', modality: '', experienceLevel: '', contractType: '', workSchedule: '', salary: '', accessibility: '', expiresAt: null, active: true });
const areas = [['service','Atendimento'],['sales','Vendas'],['admin','Administração'],['numbers','Finanças e números'],['logistics','Logística'],['maintenance','Manutenção'],['production','Produção'],['technology','Tecnologia e informática'],['health','Saúde e bem-estar'],['marketing','Marketing e comunicação']];
const fieldClass = 'mt-2 w-full rounded-xl border border-ink/15 bg-white px-4 py-3 font-normal outline-none focus:border-brand-blue';

export function AdminCatalogPage({ token, onBack, onLogout, onProfile, navigation }: Props) {
  const [kind, setKind] = useState<Kind>('job');
  const [items, setItems] = useState<CatalogItem[]>([]);
  const [form, setForm] = useState<CatalogForm>(empty('job'));
  const [editing, setEditing] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [reports, setReports] = useState<CatalogReport[]>([]);

  const load = useCallback(async () => {
    try { setItems(await api<CatalogItem[]>(`/admin/catalog/${kind}`, token)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Acesso administrativo indisponível.'); }
  }, [kind, token]);
  useEffect(() => { void load(); }, [load]);
  const loadReports = useCallback(async () => {
    try { setReports(await api<CatalogReport[]>('/admin/catalog-reports', token)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível carregar os avisos dos usuários.'); }
  }, [token]);
  useEffect(() => { void loadReports(); }, [loadReports]);

  async function reviewReport(report: CatalogReport, status: 'RESOLVED' | 'DISMISSED') {
    try {
      await api(`/admin/catalog-reports/${report.id}`, token, { method: 'PUT', body: JSON.stringify({ status }) });
      setReports(current => current.filter(item => item.id !== report.id));
      setNotice(status === 'RESOLVED' ? 'Aviso marcado como resolvido.' : 'Aviso descartado.');
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível atualizar o aviso.'); }
  }

  async function save(event: FormEvent) {
    event.preventDefault(); setError(''); setNotice('');
    try {
      const path = editing ? `/admin/catalog/${editing}` : '/admin/catalog';
      await api(path, token, { method: editing ? 'PUT' : 'POST', body: JSON.stringify({ ...form, latitude: form.latitude ? Number(form.latitude) : null, longitude: form.longitude ? Number(form.longitude) : null }) });
      setForm(empty(kind)); setEditing(null); setNotice('Enviado para revisão. A publicação só ficará visível após aprovação.'); await load();
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível salvar.'); }
  }
  function edit(item: CatalogItem) {
    setEditing(item.id);
    setForm({ kind: item.kind, title: item.title, provider: item.provider, description: item.description, url: item.url, area: item.area, location: item.location || '', latitude: item.latitude?.toString() || '', longitude: item.longitude?.toString() || '', modality: item.modality || '', experienceLevel: item.experienceLevel || '', contractType: item.contractType || '', workSchedule: item.workSchedule || '', salary: item.salary || '', accessibility: item.accessibility || '', expiresAt: item.expiresAt, active: item.active });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  async function review(item: CatalogItem, status: 'APPROVED' | 'REJECTED') {
    try { await api(`/admin/catalog/${item.id}/review`, token, { method: 'PUT', body: JSON.stringify({ status }) }); setNotice(status === 'APPROVED' ? 'Publicação aprovada e disponível no catálogo.' : 'Publicação rejeitada e mantida fora do catálogo.'); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível atualizar a revisão.'); }
  }
  async function remove(item: CatalogItem) {
    if (!window.confirm(`Excluir “${item.title}” do catálogo?`)) return;
    try { await api(`/admin/catalog/${item.id}`, token, { method: 'DELETE' }); setNotice('Publicação removida ou arquivada.'); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível remover a publicação.'); }
  }

  const date = (value: string) => new Date(value).toLocaleDateString('pt-BR');
  const expiring = (item: CatalogItem) => item.expiresAt ? Math.ceil((new Date(`${item.expiresAt}T12:00:00`).getTime() - Date.now()) / 86400000) : null;
  const linkCheckDue = (item: CatalogItem) => !item.lastLinkCheckAt || Date.now() - new Date(item.lastLinkCheckAt).getTime() > 30 * 86400000;

  return <div className="min-h-screen bg-paper pb-16">
    <RibbonStrip />
    <a href="#admin-main" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand-white focus:px-4 focus:py-3 focus:font-bold">Pular para o conteúdo</a>
    <header className="flex items-center justify-between border-b border-ink/10 px-[6vw] py-5"><button onClick={onBack} className="border-0 bg-transparent font-serif text-2xl font-black">Trampol<span className="text-brand-red">i</span>m · Gestão</button><AccountHeaderActions onProfile={onProfile} onLogout={onLogout} /></header>
    {navigation}
    <main id="admin-main" className="mx-auto max-w-7xl px-[6vw]">
      <div className="py-10"><p className="text-sm font-extrabold uppercase tracking-widest text-brand-green">Painel administrativo</p><h1 className="mt-2 text-4xl font-black">Gerenciar catálogo</h1><p className="mt-2 text-ink/65">Cada publicação ou alteração passa por revisão antes de aparecer para as pessoas.</p></div>
      <MetricsPanel token={token} />
      {error && <p role="alert" className="mb-4 rounded-xl bg-red-50 p-4 text-brand-red">{error}</p>}{notice && <p role="status" className="mb-4 rounded-xl bg-green-50 p-4 text-brand-green">{notice}</p>}
      <section aria-labelledby="reports-title" className="mb-8 rounded-3xl border border-brand-red/20 bg-brand-white p-6"><div className="mb-4 flex flex-wrap items-center justify-between gap-3"><div><h2 id="reports-title" className="text-2xl font-black">Avisos da comunidade</h2><p className="text-sm text-ink/60">Publicações sinalizadas para conferência: {reports.length} em aberto.</p></div><button type="button" onClick={() => void loadReports()} className="rounded-full border border-ink/20 px-4 py-2 text-sm font-bold">Atualizar</button></div>{reports.length ? <div className="space-y-3">{reports.map(report => <article key={report.id} className="rounded-2xl border border-ink/10 bg-paper p-4"><p className="text-xs font-extrabold uppercase tracking-wider text-brand-red">{report.kind === 'job' ? 'Vaga' : 'Curso'} · {report.reason.replaceAll('_', ' ').toLocaleLowerCase('pt-BR')}</p><h3 className="mt-1 font-bold">{report.itemTitle}</h3>{report.details && <p className="mt-2 text-sm text-ink/70">{report.details}</p>}<div className="mt-3 flex flex-wrap gap-2"><button type="button" onClick={() => reviewReport(report, 'RESOLVED')} className="rounded-full bg-brand-green px-4 py-2 text-sm font-bold text-ink">Marcar resolvido</button><button type="button" onClick={() => reviewReport(report, 'DISMISSED')} className="rounded-full border border-ink/20 px-4 py-2 text-sm font-bold">Descartar aviso</button><button type="button" onClick={() => { const item = items.find(entry => entry.id === report.itemId); if (item) edit(item); else { setKind(report.kind); setError('Alterne para o catálogo de ' + (report.kind === 'job' ? 'vagas' : 'cursos') + ' para editar esta publicação.'); } }} className="rounded-full border border-brand-blue/30 px-4 py-2 text-sm font-bold text-brand-blue">Revisar publicação</button></div></article>)}</div> : <p className="rounded-xl bg-paper p-4 text-sm text-ink/60">Nenhum aviso aguardando análise.</p>}</section>
      <div role="group" aria-label="Tipo de catálogo" className="mb-5 flex gap-3"><button onClick={() => { setKind('job'); setForm(empty('job')); setEditing(null); }} className={`rounded-full px-5 py-3 font-bold ${kind === 'job' ? 'bg-ink text-white' : 'bg-white'}`}>Vagas</button><button onClick={() => { setKind('course'); setForm(empty('course')); setEditing(null); }} className={`rounded-full px-5 py-3 font-bold ${kind === 'course' ? 'bg-ink text-white' : 'bg-white'}`}>Cursos</button></div>
      <form onSubmit={save} className="grid gap-4 rounded-3xl border border-ink/10 bg-brand-white p-6 md:grid-cols-2">
        <h2 className="text-2xl font-black md:col-span-2">{editing ? 'Editar publicação' : `Nova publicação de ${kind === 'job' ? 'vaga' : 'curso'}`}</h2>
        <label className="text-sm font-bold">Título<input required maxLength={180} value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} className={fieldClass} /></label>
        <label className="text-sm font-bold">{kind === 'job' ? 'Empresa' : 'Instituição'}<input required maxLength={180} value={form.provider} onChange={event => setForm({ ...form, provider: event.target.value })} className={fieldClass} /></label>
        <label className="text-sm font-bold">Link seguro (http/https)<input required type="url" value={form.url} onChange={event => setForm({ ...form, url: event.target.value })} className={fieldClass} /></label>
        <label className="text-sm font-bold">Área<select value={form.area} onChange={event => setForm({ ...form, area: event.target.value })} className={fieldClass}>{areas.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <label className="text-sm font-bold">Localidade<input maxLength={180} value={form.location} onChange={event => setForm({ ...form, location: event.target.value })} placeholder="Cidade, estado ou remoto" className={fieldClass} /></label>
        {kind === 'job' && <><label className="text-sm font-bold">Latitude do local de trabalho<input type="number" min="-90" max="90" step="any" value={form.latitude} onChange={event => setForm({ ...form, latitude: event.target.value })} placeholder="Opcional" className={fieldClass} /></label><label className="text-sm font-bold">Longitude do local de trabalho<input type="number" min="-180" max="180" step="any" value={form.longitude} onChange={event => setForm({ ...form, longitude: event.target.value })} placeholder="Opcional" className={fieldClass} /></label><p className="text-xs text-ink/55 md:col-span-2">As coordenadas permitem que a pessoa filtre vagas por raio usando a localização do próprio aparelho. Só use coordenadas do local de trabalho confirmadas.</p></>}
        <label className="text-sm font-bold">Modalidade<select value={form.modality} onChange={event => setForm({ ...form, modality: event.target.value })} className={fieldClass}><option value="">Não informada</option><option>Presencial</option><option>Híbrido</option><option>Remoto</option></select></label>
        {kind === 'job' && <><label className="text-sm font-bold">Experiência exigida<select value={form.experienceLevel} onChange={event => setForm({ ...form, experienceLevel: event.target.value })} className={fieldClass}><option value="">Não informada</option><option>Sem experiência</option><option>Júnior</option><option>Pleno</option><option>Sênior</option></select></label><label className="text-sm font-bold">Tipo de contrato<select value={form.contractType} onChange={event => setForm({ ...form, contractType: event.target.value })} className={fieldClass}><option value="">Não informado</option><option>CLT</option><option>PJ</option><option>Temporário</option><option>Estágio</option><option>Aprendiz</option><option>Autônomo</option></select></label><label className="text-sm font-bold">Jornada<input maxLength={80} value={form.workSchedule} onChange={event => setForm({ ...form, workSchedule: event.target.value })} placeholder="Ex.: integral · segunda a sexta" className={fieldClass} /></label><label className="text-sm font-bold">Faixa salarial<input maxLength={80} value={form.salary} onChange={event => setForm({ ...form, salary: event.target.value })} placeholder="Ex.: R$ 2.000 a R$ 2.800" className={fieldClass} /></label><label className="text-sm font-bold">Data limite para candidatura<input type="date" min={new Date().toISOString().slice(0, 10)} value={form.expiresAt || ''} onChange={event => setForm({ ...form, expiresAt: event.target.value || null })} className={fieldClass} /></label></>}
        <label className="text-sm font-bold md:col-span-2">Descrição<textarea required maxLength={3000} rows={3} value={form.description} onChange={event => setForm({ ...form, description: event.target.value })} className={fieldClass} /></label>
        {kind === 'job' && <label className="text-sm font-bold md:col-span-2">Acessibilidade e inclusão (opcional)<textarea maxLength={2000} rows={2} value={form.accessibility} onChange={event => setForm({ ...form, accessibility: event.target.value })} className={fieldClass} placeholder="Registre apenas recursos confirmados pela empresa." /></label>}
        {kind === 'job' && <div className="rounded-xl bg-paper p-4 text-sm md:col-span-2"><strong>Revisão de anúncio</strong><p className="mt-1">Confira empresa, link, local, requisitos e prazo. Se houver acessibilidade, informe somente o que a empresa confirmou. Ao salvar, a publicação ficará pendente até a revisão.</p></div>}
        <div className="flex gap-3 md:col-span-2"><button className="rounded-full bg-brand-red px-6 py-3 font-extrabold text-white">{editing ? 'Enviar alterações para revisão' : 'Enviar para revisão'}</button>{editing && <button type="button" onClick={() => { setEditing(null); setForm(empty(kind)); }} className="rounded-full border border-ink/20 px-5 py-3 font-bold">Cancelar</button>}</div>
      </form>
      <section className="mt-10"><div className="mb-4 flex flex-wrap items-end justify-between gap-3"><div><h2 className="text-2xl font-black">Publicações cadastradas</h2><p className="text-sm text-ink/60">{items.filter(item => item.moderationStatus === 'PENDING').length} aguardando revisão · {items.length} no total</p><p className="mt-1 text-xs text-ink/55">A checagem de links é manual: abra o destino, confirme se ele ainda funciona e registre a revisão. Revise anúncios com link não conferido ou conferido há mais de 30 dias.</p></div></div>
        <div className="space-y-3">{items.map(item => { const days = expiring(item); const expired = days !== null && days < 0; const badge = item.moderationStatus === 'APPROVED' && !item.active ? 'Pausada' : item.moderationStatus === 'APPROVED' ? 'Aprovada' : item.moderationStatus === 'PENDING' ? 'Aguardando revisão' : 'Rejeitada'; return <article key={item.id} className="rounded-2xl border border-ink/10 bg-brand-white p-5"><div className="flex flex-wrap items-start justify-between gap-4"><div><span className={`text-xs font-extrabold uppercase ${badge === 'Aprovada' ? 'text-brand-green' : badge === 'Aguardando revisão' ? 'text-brand-blue' : 'text-brand-red'}`}>{badge} · {item.provider}</span><h3 className="mt-1 font-extrabold">{item.title}</h3><p className="text-sm text-ink/60">{item.location || 'Local não informado'} · {item.area} · Atualizada em {date(item.updatedAt)}</p><p className="mt-1 text-xs text-ink/50">Criada por {item.createdBy || '—'} · Última alteração por {item.updatedBy || '—'}</p><a href={item.url} target="_blank" rel="noreferrer" className="mt-1 inline-block text-sm font-bold text-brand-blue">Conferir link de destino ↗</a><p className="mt-1 text-xs text-ink/55">Link: {item.linkStatus === "BROKEN" ? "reportado indisponível" : linkCheckDue(item) ? "revisão periódica necessária" : "conferido ativo"}{item.lastLinkCheckAt ? ` · ${date(item.lastLinkCheckAt)}` : ""}</p><div className="mt-2 flex flex-wrap gap-2"><button onClick={() => api(`/admin/catalog/${item.id}/link-check`, token, { method: "POST", body: JSON.stringify({ reachable: true }) }).then(load).catch(cause => setError(cause.message))} className="rounded-full border border-brand-green/30 px-3 py-1 text-xs font-bold text-brand-green">Registrar link ativo</button><button onClick={() => api(`/admin/catalog/${item.id}/link-check`, token, { method: "POST", body: JSON.stringify({ reachable: false }) }).then(load).catch(cause => setError(cause.message))} className="rounded-full border border-brand-red/30 px-3 py-1 text-xs font-bold text-brand-red">Reportar link indisponível</button></div>{item.expiresAt && <p className={`mt-1 text-sm font-semibold ${expired || (days !== null && days <= 7) ? 'text-brand-red' : 'text-ink/60'}`}>{expired ? `Prazo vencido há ${Math.abs(days!)} dia(s): revise, atualize ou remova.` : days === 0 ? 'Prazo termina hoje.' : `Prazo termina em ${days} dia(s).`}</p>}{item.reviewedBy && <p className="mt-1 text-xs text-ink/50">Revisada por {item.reviewedBy}</p>}{item.moderationStatus === 'APPROVED' && item.expiresAt && expired && <span className="mt-2 inline-block rounded-full bg-brand-red/10 px-3 py-1 text-xs font-bold text-brand-red">Vencida — oculta do catálogo</span>}</div><div className="flex flex-wrap gap-2"><button onClick={() => edit(item)} className="rounded-full border border-ink/20 px-4 py-2 text-sm font-bold">Editar</button>{item.moderationStatus !== 'APPROVED' && <button onClick={() => review(item, 'APPROVED')} className="rounded-full bg-brand-green px-4 py-2 text-sm font-bold text-white">Aprovar</button>}{item.moderationStatus !== 'REJECTED' && <button onClick={() => review(item, 'REJECTED')} className="rounded-full border border-brand-red/30 px-4 py-2 text-sm font-bold text-brand-red">Rejeitar</button>}{item.moderationStatus === 'APPROVED' && <button onClick={() => api(`/admin/catalog/${item.id}/visibility`, token, { method: 'PUT', body: JSON.stringify({ active: !item.active }) }).then(load).catch(cause => setError(cause.message))} className="rounded-full border border-ink/20 px-4 py-2 text-sm font-bold">{item.active ? 'Pausar' : 'Reativar'}</button>}<button onClick={() => remove(item)} className="rounded-full border border-ink/20 px-4 py-2 text-sm font-bold">Excluir</button></div></div></article>; })}</div>
      </section>
    </main>
  </div>;
}
