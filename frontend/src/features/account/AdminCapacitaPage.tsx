import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from './api';
import { RibbonStrip } from '../../components/ui/RibbonStrip';
import type { CapacitaContentInput } from './api';

type Category = { id: number; name: string; description: string; contentsCount: number };
type Track = { id: number; code: string; title: string; categories: Category[] };
type Content = { id: number; title: string; summary: string; difficulty: string | null; resourceType: string; sourceUrl: string | null; isFree: boolean; hasCertificate: boolean; durationMinutes: number | null; categoryId: number; categoryName: string; stageCode: string; status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED' };

const empty: CapacitaContentInput = { categoryId: 0, title: '', summary: '', difficulty: '', resourceType: 'Material', sourceUrl: '', isFree: false, hasCertificate: false, durationMinutes: null };
const field = 'mt-2 w-full rounded-xl border border-white/15 bg-[#0b1421] px-4 py-3 text-ink';

export function AdminCapacitaPage({ token, onBack }: { token: string; onBack: () => void }) {
  const [tracks, setTracks] = useState<Track[]>([]);
  const [contents, setContents] = useState<Content[]>([]);
  const [form, setForm] = useState<CapacitaContentInput>(empty);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const categories = tracks.flatMap(track => track.categories.map(category => ({ ...category, trackTitle: track.title })));

  const refresh = useCallback(async () => {
    const [nextTracks, nextContents] = await Promise.all([api<Track[]>('/capacita/tracks', token), api<Content[]>('/admin/capacita/contents', token)]);
    setTracks(nextTracks); setContents(nextContents);
    if (nextTracks[0]?.categories[0]) setForm(current => current.categoryId ? current : ({ ...current, categoryId: nextTracks[0].categories[0].id }));
  }, [token]);
  useEffect(() => { refresh().catch(cause => setError(cause instanceof Error ? cause.message : 'Não foi possível carregar os conteúdos.')).finally(() => setLoading(false)); }, [refresh]);

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('');
    try { await api<Content>('/admin/capacita/contents', token, { method: 'POST', body: JSON.stringify(form) }); setForm({ ...empty, categoryId: form.categoryId }); await refresh(); setNotice('Conteúdo salvo como rascunho. Revise e publique quando estiver pronto.'); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível salvar o conteúdo.'); }
    finally { setBusy(false); }
  }
  async function setStatus(content: Content, status: Content['status']) {
    setError(''); setNotice('');
    try { await api<Content>(`/admin/capacita/contents/${content.id}/review`, token, { method: 'PUT', body: JSON.stringify({ status }) }); await refresh(); setNotice(status === 'PUBLISHED' ? 'Conteúdo publicado.' : 'Status atualizado.'); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível atualizar o conteúdo.'); }
  }

  return <><RibbonStrip/><header className="border-b border-white/10 px-[6vw] py-5"><div className="mx-auto flex max-w-7xl items-center justify-between"><strong className="font-serif text-xl font-black">TRAMPOLIM <span className="text-brand-blue">CAPACITA</span></strong><button onClick={onBack} className="rounded-full border border-white/20 px-4 py-2 text-sm font-bold">Voltar à administração</button></div></header><main className="mx-auto max-w-7xl px-[6vw] py-10"><p className="text-xs font-extrabold uppercase tracking-widest text-brand-blue">Gestão de aprendizagem</p><h1 className="mt-2 text-4xl font-black">Conteúdos do Capacita</h1><p className="mt-2 max-w-3xl text-sm leading-6 text-ink/60">Cadastre apenas materiais e links conferidos. Conteúdos salvos como rascunho não aparecem para estudantes.</p>{error && <p role="alert" className="mt-5 rounded-xl bg-red-400/10 p-4 text-sm font-bold">{error}</p>}{notice && <p role="status" className="mt-5 rounded-xl bg-brand-green/10 p-4 text-sm font-bold text-brand-green">{notice}</p>}
      <form onSubmit={create} className="mt-7 grid gap-4 rounded-3xl border border-white/10 bg-[#121c2b] p-6 md:grid-cols-2"><h2 className="text-2xl font-black md:col-span-2">Adicionar material</h2><label className="text-sm font-bold">Etapa e categoria<select required value={form.categoryId || ''} onChange={event => setForm({ ...form, categoryId: Number(event.target.value) })} className={field}><option value="" disabled>Selecione</option>{tracks.map(track => <optgroup key={track.id} label={track.title}>{track.categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</optgroup>)}</select></label><label className="text-sm font-bold">Dificuldade (opcional)<select value={form.difficulty || ''} onChange={event => setForm({ ...form, difficulty: event.target.value })} className={field}><option value="">Não informada</option><option>Iniciante</option><option>Intermediário</option><option>Avançado</option></select></label><label className="text-sm font-bold md:col-span-2">Título<input required maxLength={180} value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} className={field}/></label><label className="text-sm font-bold md:col-span-2">Resumo e orientação<textarea required maxLength={3000} rows={4} value={form.summary} onChange={event => setForm({ ...form, summary: event.target.value })} className={field}/></label><label className="text-sm font-bold">Tipo de recurso<input maxLength={40} value={form.resourceType} onChange={event => setForm({ ...form, resourceType: event.target.value })} className={field} placeholder="Material, vídeo, curso…"/></label><label className="text-sm font-bold">Link verificado (opcional)<input type="url" maxLength={1000} value={form.sourceUrl || ''} onChange={event => setForm({ ...form, sourceUrl: event.target.value })} className={field} placeholder="https://"/></label><label className="text-sm font-bold">Duração em minutos (opcional)<input type="number" min={1} value={form.durationMinutes || ''} onChange={event => setForm({ ...form, durationMinutes: event.target.value ? Number(event.target.value) : null })} className={field}/></label><div className="flex flex-wrap gap-5 self-end pb-3"><label className="flex items-center gap-2 text-sm font-bold"><input type="checkbox" checked={form.isFree} onChange={event => setForm({ ...form, isFree: event.target.checked })}/>Gratuito confirmado</label><label className="flex items-center gap-2 text-sm font-bold"><input type="checkbox" checked={form.hasCertificate} onChange={event => setForm({ ...form, hasCertificate: event.target.checked })}/>Certificado confirmado</label></div><button disabled={busy || categories.length === 0} className="rounded-full bg-brand-blue px-6 py-3 font-extrabold text-[#101725] disabled:opacity-50 md:col-span-2">{busy ? 'Salvando…' : 'Salvar como rascunho'}</button></form>
      <section className="mt-10"><h2 className="text-2xl font-black">Materiais cadastrados</h2>{loading ? <p role="status" className="mt-4">Carregando…</p> : contents.length ? <div className="mt-4 space-y-3">{contents.map(content => <article key={content.id} className="rounded-2xl border border-white/10 bg-[#121c2b] p-5"><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-xs font-bold uppercase tracking-widest text-brand-blue">{content.stageCode.replaceAll('_', ' ')} · {content.categoryName}</p><h3 className="mt-1 text-lg font-extrabold">{content.title}</h3><p className="mt-1 text-sm text-ink/55">Status: {content.status === 'PUBLISHED' ? 'Publicado' : content.status === 'ARCHIVED' ? 'Arquivado' : 'Rascunho'}</p></div><div className="flex gap-2">{content.status !== 'PUBLISHED' && <button onClick={() => void setStatus(content, 'PUBLISHED')} className="rounded-full bg-brand-green px-4 py-2 text-sm font-extrabold text-[#101725]">Publicar</button>}{content.status === 'PUBLISHED' && <button onClick={() => void setStatus(content, 'ARCHIVED')} className="rounded-full border border-white/20 px-4 py-2 text-sm font-bold">Arquivar</button>}</div></div></article>)}</div> : <p className="mt-4 rounded-2xl border border-dashed border-white/15 p-6 text-sm text-ink/55">Nenhum material cadastrado ainda.</p>}</section>
    </main></>;
}
