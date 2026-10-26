import { useEffect, useRef, useState } from 'react';
import { API_URL } from './api';
import { RibbonStrip } from '../../components/ui/RibbonStrip';

type PublicListing = { id: number; kind: 'job' | 'course'; title: string; area: string; description: string; requirements: string; location: string; modality: string; url: string; salary: string | null; contractType: string | null; workSchedule: string | null; workload: string | null; prerequisites: string | null; benefits: string | null; coursePrice: string | null; certificate: string | null; expiresAt: string | null };
type PublicCompany = { publicSlug: string; tradeName: string; industry: string; city: string; state: string; website: string | null; description: string | null; listings: PublicListing[] };
const inputClass = 'mt-2 w-full rounded-xl border border-white/15 bg-[#0b1421] px-4 py-3 text-ink placeholder:text-ink/40';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, { ...options, headers: { ...(options?.headers || {}), ...(options?.body ? { 'Content-Type': 'application/json' } : {}) } });
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.detail || data?.message || (response.status === 409 ? 'Você já enviou sua candidatura.' : 'Não foi possível concluir. Tente novamente.'));
  return data as T;
}

export function CompanyPublicPage({ slug }: { slug: string }) {
  const [company, setCompany] = useState<PublicCompany | null>(null);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState<PublicListing | null>(null);
  const [form, setForm] = useState({ name: '', email: '', phone: '', message: '', consent: false });
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const opportunitiesRef = useRef<HTMLElement | null>(null);

  useEffect(() => { request<PublicCompany>(`/public/companies/${encodeURIComponent(slug)}`).then(setCompany).catch(cause => setError(cause instanceof Error ? cause.message : 'Página indisponível.')); }, [slug]);
  useEffect(() => {
    if (!company || !opportunitiesRef.current || typeof IntersectionObserver === 'undefined') return;
    const observer = new IntersectionObserver(entries => {
      for (const entry of entries) {
        if (!entry.isIntersecting || entry.intersectionRatio < 0.5) continue;
        const id = (entry.target as HTMLElement).dataset.listingId;
        if (!id) continue;
        observer.unobserve(entry.target);
        void request(`/public/companies/${encodeURIComponent(slug)}/listings/${id}/view`, { method: 'POST' }).catch(() => undefined);
      }
    }, { threshold: 0.5 });
    opportunitiesRef.current.querySelectorAll('[data-listing-id]').forEach(item => observer.observe(item));
    return () => observer.disconnect();
  }, [company, slug]);
  async function openApplication(listing: PublicListing) {
    setError(''); setNotice(''); setSelected(listing);
    try { await request(`/public/companies/${encodeURIComponent(slug)}/listings/${listing.id}/click`, { method: 'POST' }); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível registrar o interesse.'); }
  }
  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!selected) return;
    setBusy(true); setError('');
    try {
      const result = await request<{ message: string }>(`/public/companies/${encodeURIComponent(slug)}/listings/${selected.id}/applications`, { method: 'POST', body: JSON.stringify(form) });
      setNotice(result.message); setSelected(null); setForm({ name: '', email: '', phone: '', message: '', consent: false });
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível enviar sua inscrição.'); }
    finally { setBusy(false); }
  }

  return <><RibbonStrip /><main className="mx-auto min-h-screen max-w-6xl px-[6vw] py-8"><a href="/" className="font-serif text-2xl font-black text-ink no-underline">Trampol<span className="text-brand-yellow">i</span>m</a>{error && !selected && <p role="alert" className="mt-8 rounded-2xl bg-red-400/10 p-4 text-sm font-bold text-red-200">{error}</p>}{!company && !error && <p role="status" className="mt-12 text-center">Carregando página da empresa…</p>}{company && <><section className="relative mt-8 overflow-hidden rounded-[24px] border border-[#46572A] bg-[#101A29] p-8 md:p-14"><div aria-hidden="true" className="absolute -right-14 -top-24 h-72 w-72 rounded-full border border-[#46572A] bg-transparent"/><p className="relative text-xs font-extrabold uppercase tracking-[.2em] text-brand-yellow">{company.industry}</p><h1 className="relative mt-3 text-5xl font-black leading-[1.02] md:text-[56px]">{company.tradeName}</h1><p className="relative mt-3 text-ink/70">{company.city}/{company.state}</p>{company.description && <p className="relative mt-5 max-w-3xl leading-7 text-ink/75">{company.description}</p>}{company.website && <a href={company.website} target="_blank" rel="noreferrer" className="relative mt-5 inline-block font-bold text-brand-yellow">Conheça a empresa ↗</a>}</section>
      <section ref={opportunitiesRef} className="mt-10"><p className="text-xs font-extrabold uppercase tracking-widest text-brand-yellow">Oportunidades</p><div className="mt-2 flex flex-wrap items-end justify-between gap-3"><h2 className="text-3xl font-black">Vagas e cursos</h2><span className="rounded-full bg-brand-yellow px-4 py-2 text-sm font-extrabold text-[#101725]">{company.listings.length} opções</span></div>{notice && <p role="status" className="mt-5 rounded-2xl border border-brand-yellow/25 bg-brand-yellow/10 p-4 font-bold">{notice}</p>}{company.listings.length ? <div className="mt-5 grid gap-5 md:grid-cols-2">{company.listings.map(item => <article key={item.id} data-listing-id={item.id} className="rounded-3xl border border-white/10 bg-[#121c2b] p-6"><p className="text-xs font-extrabold uppercase tracking-widest text-brand-yellow">{item.kind === 'job' ? 'Vaga' : 'Curso'} · {item.area}</p><h3 className="mt-3 text-2xl font-black">{item.title}</h3><p className="mt-2 text-sm text-ink/60">{item.location} · {item.modality}</p><p className="mt-4 whitespace-pre-line text-sm leading-6 text-ink/75">{item.description}</p><p className="mt-4 text-sm"><strong>Perfil / requisitos:</strong> {item.requirements}</p>{item.kind === 'job' ? <div className="mt-3 text-sm text-ink/65">{item.salary} · {item.contractType} · {item.workSchedule}{item.benefits && ` · Benefícios: ${item.benefits}`}</div> : <div className="mt-3 text-sm text-ink/65">{item.workload} · {item.coursePrice || 'Consulte investimento'}{item.certificate && ` · Certificado: ${item.certificate}`}</div>}<button onClick={() => openApplication(item)} className="mt-6 rounded-full border-0 bg-brand-yellow px-6 py-3 font-extrabold text-[#101725]">{item.kind === 'job' ? 'Candidatar-se' : 'Inscrever-se'}</button></article>)}</div> : <div className="mt-5 rounded-3xl border border-dashed border-white/15 p-10 text-center"><h3 className="text-xl font-bold">Nenhuma oportunidade aberta no momento</h3><p className="mt-2 text-sm text-ink/60">Volte em breve para conferir as próximas oportunidades.</p></div>}</section></>}</main><footer className="border-t border-white/10 px-[6vw] py-7"><div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 text-xs text-ink/55"><strong className="font-serif text-lg font-extrabold text-ink">TRAMPOLIM</strong><span>Capacitação e oportunidades para o seu próximo passo.</span><span>© {new Date().getFullYear()} Trampolim</span></div></footer>
    {selected && <div className="fixed inset-0 z-50 grid place-items-center overflow-y-auto bg-black/70 p-4"><section role="dialog" aria-modal="true" aria-labelledby="apply-title" className="my-auto w-full max-w-xl rounded-3xl border border-white/10 bg-[#121c2b] p-6 md:p-8"><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-extrabold uppercase tracking-widest text-brand-yellow">{selected.kind === 'job' ? 'Candidatura' : 'Inscrição'}</p><h2 id="apply-title" className="mt-2 text-2xl font-black">{selected.title}</h2></div><button onClick={() => setSelected(null)} aria-label="Fechar formulário" className="rounded-full border border-white/15 bg-transparent px-3 py-1 text-xl text-ink">×</button></div>{error && <p role="alert" className="mt-4 rounded-xl bg-red-400/10 p-3 text-sm font-bold text-red-200">{error}</p>}<form onSubmit={submit} className="mt-5 space-y-4"><label className="block text-sm font-bold">Nome completo<input required maxLength={120} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} className={inputClass} /></label><label className="block text-sm font-bold">E-mail<input required type="email" maxLength={180} value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} className={inputClass} /></label><label className="block text-sm font-bold">Telefone (opcional)<input type="tel" maxLength={40} value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} className={inputClass} /></label><label className="block text-sm font-bold">Mensagem (opcional)<textarea maxLength={2000} rows={3} value={form.message} onChange={e => setForm({ ...form, message: e.target.value })} className={inputClass} placeholder="Conte brevemente sobre seu interesse." /></label><label className="flex items-start gap-3 text-sm text-ink/75"><input required type="checkbox" checked={form.consent} onChange={e => setForm({ ...form, consent: e.target.checked })} className="mt-1" /><span>Concordo em compartilhar estes dados com {company?.tradeName} para tratar esta candidatura ou inscrição.</span></label><button disabled={busy} className="w-full rounded-full border-0 bg-brand-yellow px-6 py-3 font-extrabold text-[#101725] disabled:opacity-60">{busy ? 'Enviando…' : 'Enviar'}</button></form></section></div>}
  </>;
}
