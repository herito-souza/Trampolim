import { useEffect, useState } from 'react';
import { API_URL } from './api';
type PublishedJob = { id: number; companySlug: string; companyName: string; title: string; area: string; description: string; requirements: string; location: string; modality: string; salary: string | null; contractType: string | null; expiresAt: string | null; internship: boolean };

export function CompanyOpportunities() {
  const [jobs, setJobs] = useState<PublishedJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  useEffect(() => {
    const controller = new AbortController();
    fetch(`${API_URL}/public/companies/opportunities`, { signal: controller.signal })
      .then(async response => { if (!response.ok) throw new Error('Não foi possível carregar as vagas empresariais.'); return response.json() as Promise<PublishedJob[]>; })
      .then(data => { if (!Array.isArray(data)) throw new Error('O serviço de vagas respondeu em um formato inesperado.'); setJobs(data); }).catch(cause => { if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : 'Falha ao carregar vagas.'); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, []);
  const visible = jobs.filter(job => `${job.title} ${job.companyName} ${job.area} ${job.location} ${job.modality}`.toLocaleLowerCase('pt-BR').includes(search.toLocaleLowerCase('pt-BR')));
  return <section aria-labelledby="company-opportunities-title" className="mt-10 border-t border-ink/15 pt-8"><h2 id="company-opportunities-title" className="text-2xl font-black">Vagas publicadas por empresas</h2><p className="mt-2 text-sm text-ink/65">Consulte os anúncios e os processos de candidatura diretamente na página pública da empresa.</p><label className="mt-5 block text-sm font-bold">Buscar nas vagas empresariais<input value={search} onChange={event => setSearch(event.target.value)} className="mt-2 block w-full rounded-xl border border-ink/20 bg-transparent px-4 py-3" placeholder="Cargo, empresa, localidade ou modalidade" /></label>{loading && <p role="status" className="mt-4">Carregando vagas empresariais…</p>}{error && <p role="alert" className="mt-4">{error}</p>}{!loading && !error && !visible.length && <p role="status" className="mt-4">Nenhuma vaga empresarial encontrada.</p>}<div className="mt-6 grid gap-5 md:grid-cols-2 xl:grid-cols-3">{visible.map(job => <article key={job.id} className="rounded-3xl border border-ink/15 bg-brand-white p-6"><p className="text-xs font-bold">{job.companyName}{job.internship ? ' · Estágio' : ''}</p><h3 className="mt-3 text-xl font-black">{job.title}</h3><p className="mt-2 text-sm">{job.area} · {job.location} · {job.modality}</p><p className="mt-3 text-sm text-ink/70">{job.description}</p><p className="mt-3 text-sm"><strong>Requisitos: </strong>{job.requirements}</p>{job.salary && <p className="mt-2 text-sm">Remuneração informada: {job.salary}</p>}<a href={`/?empresa=${encodeURIComponent(job.companySlug)}`} className="mt-5 inline-flex rounded-full bg-ink px-5 py-3 text-sm font-bold text-brand-white">Ver vaga na empresa</a></article>)}</div></section>;
}
