import { useCallback, useEffect, useState } from 'react';
import { getRecommendationReason, getRecommendationScore, getRecommendations, loadDiagnosticResult } from '../diagnostic/recommendations';
import type { Opportunity, Skill } from '../diagnostic/recommendations';
import type { CatalogItem, Profile } from './api';
import { api, recordCatalogMetric } from './api';
import { AccountNavigation } from './AccountNavigation';
import type { AccountDestination } from './AccountNavigation';
import { AccountHeaderActions } from './AccountHeaderActions';
import { getLinkReviewMessage, getOpportunityDeadlineStatus, getOpportunityDaysRemaining } from './catalogUtils';
import { RibbonStrip } from '../../components/ui/RibbonStrip';

interface CatalogPageProps {
  kind: 'course' | 'job';
  token: string;
  profile: Profile;
  onLogout: () => void;
  onRetakeDiagnostic: () => void;
  onNavigate: (destination: AccountDestination) => void;
  canManage?: boolean;
}

const buttonClass = 'mt-auto inline-flex w-fit rounded-full bg-ink px-5 py-3 text-sm font-extrabold text-brand-white no-underline transition-transform hover:-translate-y-0.5';
const skills: { value: Skill; label: string }[] = [
  { value: 'service', label: 'Atendimento' }, { value: 'sales', label: 'Vendas' }, { value: 'admin', label: 'Administração' },
  { value: 'numbers', label: 'Finanças e números' }, { value: 'logistics', label: 'Logística' },
  { value: 'maintenance', label: 'Manutenção' }, { value: 'production', label: 'Produção' },
  { value: 'technology', label: 'Tecnologia e informática' }, { value: 'health', label: 'Saúde e bem-estar' }, { value: 'marketing', label: 'Marketing e comunicação' },
];

export function CatalogPage({ kind, token, profile, onLogout, onRetakeDiagnostic, onNavigate, canManage }: CatalogPageProps) {
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [area, setArea] = useState<Skill | ''>('');
  const [provider, setProvider] = useState('');
  const [city, setCity] = useState('');
  const [modality, setModality] = useState('');
  const [experienceLevel, setExperienceLevel] = useState('');
  const [contractType, setContractType] = useState('');
  const [workSchedule, setWorkSchedule] = useState('');
  const [distanceKm, setDistanceKm] = useState('');
  const [currentLocation, setCurrentLocation] = useState<{ latitude: number; longitude: number } | null>(null);
  const [locationError, setLocationError] = useState('');
  const [accessibleOnly, setAccessibleOnly] = useState(false);
  const [minimumSalary, setMinimumSalary] = useState('');
  const [maximumSalary, setMaximumSalary] = useState('');
  const [sortOrder, setSortOrder] = useState<'recent' | 'title'>('recent');
  const [favoriteIds, setFavoriteIds] = useState<string[]>([]);
  const [favoriteError, setFavoriteError] = useState('');
  const [favoriteLoading, setFavoriteLoading] = useState(true);
  const [published, setPublished] = useState<CatalogItem[]>([]);
  const [catalogError, setCatalogError] = useState('');
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [applications, setApplications] = useState<number[]>([]);
  const [applicationError, setApplicationError] = useState('');
  const [applicationsLoading, setApplicationsLoading] = useState(true);
  const [reportingId, setReportingId] = useState<number | null>(null);
  const [reportReason, setReportReason] = useState('BROKEN_LINK');
  const [reportDetails, setReportDetails] = useState('');
  const [reportNotice, setReportNotice] = useState('');
  const [reportError, setReportError] = useState('');
  const [reportBusy, setReportBusy] = useState(false);
  const diagnostic = loadDiagnosticResult();
  const items = getRecommendations(diagnostic, kind);
  const isCourse = kind === 'course';
  const providers = [...new Set([...items.map(item => item.provider), ...published.map(item => item.provider)])].sort();
  const hasStructuredFilters = Boolean(city || modality || experienceLevel || contractType || workSchedule || distanceKm || accessibleOnly || minimumSalary || maximumSalary);
  const activeFilters: { key: string; label: string; clear: () => void }[] = [];
  if (query.trim()) activeFilters.push({ key: 'query', label: `Busca: ${query.trim()}`, clear: () => setQuery('') });
  if (area) activeFilters.push({ key: 'area', label: `Área: ${skills.find(skill => skill.value === area)?.label || area}`, clear: () => setArea('') });
  if (provider) activeFilters.push({ key: 'provider', label: `${isCourse ? 'Plataforma' : 'Empresa'}: ${provider}`, clear: () => setProvider('') });
  if (city.trim()) activeFilters.push({ key: 'city', label: `Região: ${city.trim()}`, clear: () => setCity('') });
  if (modality) activeFilters.push({ key: 'modality', label: `Modalidade: ${modality}`, clear: () => setModality('') });
  if (experienceLevel) activeFilters.push({ key: 'experience', label: `Experiência: ${experienceLevel}`, clear: () => setExperienceLevel('') });
  if (contractType) activeFilters.push({ key: 'contract', label: `Contrato: ${contractType}`, clear: () => setContractType('') });
  if (workSchedule.trim()) activeFilters.push({ key: 'schedule', label: `Jornada: ${workSchedule.trim()}`, clear: () => setWorkSchedule('') });
  if (minimumSalary) activeFilters.push({ key: 'minSalary', label: `Salário mínimo: R$ ${minimumSalary}`, clear: () => setMinimumSalary('') });
  if (maximumSalary) activeFilters.push({ key: 'maxSalary', label: `Salário máximo: R$ ${maximumSalary}`, clear: () => setMaximumSalary('') });
  if (distanceKm) activeFilters.push({ key: 'distance', label: `Raio: ${distanceKm} km`, clear: () => setDistanceKm('') });
  if (accessibleOnly) activeFilters.push({ key: 'accessibility', label: 'Acessibilidade informada', clear: () => setAccessibleOnly(false) });
  const activeFilterCount = activeFilters.length;
  const filteredItems = items.filter(item => {
    const matchesQuery = `${item.title} ${item.description} ${item.provider}`.toLocaleLowerCase('pt-BR').includes(query.trim().toLocaleLowerCase('pt-BR'));
    return matchesQuery && !hasStructuredFilters && (!area || item.tags.includes(area)) && (!provider || item.provider === provider);
  });

  const loadFavorites = useCallback(async () => {
    setFavoriteLoading(true); setFavoriteError('');
    try { setFavoriteIds(await api<string[]>('/profile/favorites', token)); }
    catch (cause) { setFavoriteError(cause instanceof Error ? cause.message : 'Não foi possível carregar seus itens salvos.'); }
    finally { setFavoriteLoading(false); }
  }, [token]);

  const loadPublished = useCallback(async () => {
    setCatalogLoading(true); setCatalogError('');
    try { setPublished(await api<CatalogItem[]>(`/opportunities/${kind}`, token)); }
    catch (cause) { setCatalogError(cause instanceof Error ? cause.message : 'Não foi possível carregar o catálogo.'); }
    finally { setCatalogLoading(false); }
  }, [kind, token]);

  const loadApplications = useCallback(async () => {
    setApplicationsLoading(true); setApplicationError('');
    try { setApplications((await api<{ itemId: number }[]>('/profile/applications', token)).map(item => item.itemId)); }
    catch (cause) { setApplicationError(cause instanceof Error ? cause.message : 'Não foi possível carregar o acompanhamento de candidaturas.'); }
    finally { setApplicationsLoading(false); }
  }, [token]);

  useEffect(() => {
    void loadFavorites(); void loadPublished(); void loadApplications();
  }, [loadFavorites, loadPublished, loadApplications]);

  function toggleFavorite(id: string) {
    const next = favoriteIds.includes(id) ? favoriteIds.filter(item => item !== id) : [...favoriteIds, id];
    setFavoriteIds(next);
    setFavoriteError('');
    api<string[]>('/profile/favorites', token, { method: 'PUT', body: JSON.stringify({ ids: next }) })
      .then(setFavoriteIds).catch(() => { setFavoriteIds(favoriteIds); setFavoriteError('Não foi possível salvar a alteração. Tente novamente.'); });
  }

  const skillLabel = (value: string) => skills.find(skill => skill.value === value)?.label || value;
  const salaryValues = (value: string | null) => (value?.match(/\d[\d.]*(?:,\d{1,2})?/g) || []).map(part => Number(part.replace(/\./g, '').replace(',', '.'))).filter(amount => amount > 0);
  const distanceBetween = (latitude: number, longitude: number) => {
    if (!currentLocation) return Number.POSITIVE_INFINITY;
    const radians = (degrees: number) => degrees * Math.PI / 180;
    const dLat = radians(latitude - currentLocation.latitude); const dLon = radians(longitude - currentLocation.longitude);
    const a = Math.max(0, Math.min(1, Math.sin(dLat / 2) ** 2 + Math.cos(radians(currentLocation.latitude)) * Math.cos(radians(latitude)) * Math.sin(dLon / 2) ** 2));
    return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  };
  const visiblePublished = published.filter(item => {
    if (item.kind === 'job' && getOpportunityDeadlineStatus(item.expiresAt) === 'expired') return false;
    const salary = salaryValues(item.salary);
    const minimum = minimumSalary ? Number(minimumSalary) : null; const maximum = maximumSalary ? Number(maximumSalary) : null;
    const salaryMatches = (!minimum || (salary.length > 0 && Math.max(...salary) >= minimum)) && (!maximum || (salary.length > 0 && Math.min(...salary) <= maximum));
    return (!diagnostic || (diagnostic.scores[item.area as Skill] ?? 0) > 0) && (!area || item.area === area) && (!provider || item.provider === provider)
      && (!city || (item.location || '').toLocaleLowerCase('pt-BR').includes(city.trim().toLocaleLowerCase('pt-BR')))
      && (!modality || item.modality === modality) && (!experienceLevel || item.experienceLevel === experienceLevel)
      && (!contractType || item.contractType === contractType) && (!workSchedule || (item.workSchedule || '').toLocaleLowerCase('pt-BR').includes(workSchedule.toLocaleLowerCase('pt-BR')))
      && (!distanceKm || (currentLocation && item.latitude !== null && item.longitude !== null && distanceBetween(item.latitude, item.longitude) <= Number(distanceKm)))
      && (!accessibleOnly || Boolean(item.accessibility?.trim())) && salaryMatches
      && `${item.title} ${item.description} ${item.provider} ${item.location || ''}`.toLocaleLowerCase('pt-BR').includes(query.trim().toLocaleLowerCase('pt-BR'));
  }).sort((a, b) => {
    if (diagnostic) {
      const scoreDifference = getRecommendationScore(diagnostic, [b.area as Skill]) - getRecommendationScore(diagnostic, [a.area as Skill]);
      if (scoreDifference) return scoreDifference;
      const preferredCity = profile.location?.split(',')[0]?.trim().toLocaleLowerCase('pt-BR');
      if (preferredCity) {
        const aLocal = Boolean(a.location?.toLocaleLowerCase('pt-BR').includes(preferredCity));
        const bLocal = Boolean(b.location?.toLocaleLowerCase('pt-BR').includes(preferredCity));
        if (aLocal !== bLocal) return aLocal ? -1 : 1;
      }
    }
    return sortOrder === 'recent' ? (b.publishedAt || b.createdAt).localeCompare(a.publishedAt || a.createdAt) : a.title.localeCompare(b.title, 'pt-BR');
  });
  async function trackApplication(item: CatalogItem) {
    try { await api('/profile/applications', token, { method: 'POST', body: JSON.stringify({ itemId: item.id }) }); setApplications(current => [...new Set([...current, item.id])]); setApplicationError(''); }
    catch (cause) { setApplicationError(cause instanceof Error ? cause.message : 'Não foi possível acompanhar a vaga.'); }
  }

  async function submitReport(itemId: number) {
    setReportBusy(true); setReportError(''); setReportNotice('');
    try {
      await api(`/opportunities/${kind}/${itemId}/reports`, token, { method: 'POST', body: JSON.stringify({ reason: reportReason, details: reportDetails }) });
      setReportNotice('Obrigado. Seu aviso foi enviado para revisão.'); setReportingId(null); setReportDetails('');
    } catch (cause) { setReportError(cause instanceof Error ? cause.message : 'Não foi possível enviar o aviso. Tente novamente.'); }
    finally { setReportBusy(false); }
  }

  return (
    <div className="min-h-screen bg-paper">
      <RibbonStrip />
      <a href="#conteudo-principal" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand-white focus:px-4 focus:py-3 focus:font-bold">Pular para o conteúdo</a>
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-ink/10 px-[6vw] py-5">
        <a href="#conteudo-principal" className="font-serif text-2xl font-black text-ink no-underline">Trampol<span className="text-brand-red">i</span>m</a>
        <AccountHeaderActions onProfile={() => onNavigate('profile')} onLogout={onLogout} />
      </header>
      <AccountNavigation active={isCourse ? 'courses' : 'jobs'} onNavigate={onNavigate} canManage={canManage} />

      <main id="conteudo-principal" className="mx-auto max-w-7xl px-[6vw] pb-20">
        <section className="relative mt-5 overflow-hidden rounded-[30px] bg-ink px-7 py-10 text-brand-white md:px-12 md:py-14">
          <div className="absolute -right-8 -top-16 h-64 w-64 rounded-full border border-brand-yellow/20 shadow-[0_0_100px_rgba(214,255,56,0.08)]" aria-hidden="true" />
          <div className="relative max-w-3xl">
            <span className="inline-block rounded-full bg-brand-yellow px-4 py-2 text-xs font-extrabold uppercase tracking-wider text-ink">{diagnostic ? 'Seleção personalizada' : 'Catálogo completo'}</span>
            <h1 className="mt-5 text-4xl font-black leading-tight md:text-5xl">{isCourse ? 'Cursos para dar o próximo passo' : 'Vagas e áreas para explorar'}, {profile.name.split(' ')[0]}.</h1>
            <p className="mt-4 max-w-2xl text-white/75">{diagnostic ? `Mostrando opções relacionadas à trilha ${diagnostic.trackName}.` : 'Explore todas as opções cadastradas. Faça o diagnóstico a qualquer momento para receber uma seleção personalizada.'}</p>
            <button onClick={onRetakeDiagnostic} className="mt-7 rounded-full border-0 bg-brand-yellow px-6 py-3 font-extrabold text-[#101725] cursor-pointer">{diagnostic ? 'Refazer diagnóstico' : 'Personalizar com diagnóstico'}</button>
          </div>
        </section>

        <div className="mb-6 mt-10 flex items-end justify-between gap-4"><div><p className="text-[13px] font-extrabold uppercase tracking-[1.5px] text-brand-red">{isCourse ? 'Aprendizagem' : 'Mercado de trabalho'}</p><h2 className="mt-2 text-3xl font-black">{isCourse ? 'Catálogo de cursos' : 'Áreas profissionais'}</h2></div><span role="status" aria-live="polite" className="rounded-full bg-brand-yellow px-4 py-2 text-sm font-extrabold text-ink">{filteredItems.length + visiblePublished.length} opções</span></div>

        <section aria-label="Pesquisa e filtros do catálogo" className="mb-7 overflow-hidden rounded-2xl border border-white/10 bg-brand-white shadow-lg">
          <div className="border-b border-white/10 px-5 py-4 md:px-6"><div className="flex flex-wrap items-center justify-between gap-3"><div><p className="text-xs font-extrabold uppercase tracking-[0.18em] text-brand-yellow">Pesquisa</p><h3 className="mt-1 text-xl font-bold">Encontre o que procura</h3></div><span className="rounded-full bg-brand-yellow/10 px-3 py-1.5 text-xs font-bold text-brand-yellow">{activeFilterCount ? `${activeFilterCount} filtros ativos` : 'Todos os resultados'}</span></div></div>
          <div className="space-y-4 p-5 md:p-6">
            <div className="grid gap-3 md:grid-cols-12">
              <label className="text-sm font-bold md:col-span-6">Buscar por nome ou palavra-chave<input type="search" value={query} onChange={event => setQuery(event.target.value)} placeholder={isCourse ? 'Ex.: atendimento, informática' : 'Ex.: auxiliar, atendimento'} className="mt-1.5 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal" /></label>
              <label className="text-sm font-bold md:col-span-3">Área<select value={area} onChange={event => setArea(event.target.value as Skill | '')} className="mt-1.5 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal"><option value="">Todas as áreas</option>{skills.map(skill => <option key={skill.value} value={skill.value}>{skill.label}</option>)}</select></label>
              <label className="text-sm font-bold md:col-span-3">{isCourse ? 'Plataforma' : 'Empresa'}<select value={provider} onChange={event => setProvider(event.target.value)} className="mt-1.5 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal"><option value="">Todas</option>{providers.map(item => <option key={item}>{item}</option>)}</select></label>
              {!isCourse && <label className="text-sm font-bold md:col-span-4">Cidade ou região<input value={city} onChange={event => setCity(event.target.value)} placeholder="Ex.: Recife" className="mt-1.5 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal" /></label>}
              <label className="text-sm font-bold md:col-span-4">Ordenar por<select value={sortOrder} onChange={event => setSortOrder(event.target.value as 'recent'|'title')} className="mt-1.5 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal"><option value="recent">Mais recentes</option><option value="title">Nome</option></select></label>
              {activeFilters.length > 0 && <div className="md:col-span-12"><div className="flex flex-wrap items-center gap-2" aria-label="Filtros aplicados"><span className="mr-1 text-xs font-bold text-ink/60">Aplicados:</span>{activeFilters.map(filter => <button key={filter.key} type="button" onClick={filter.clear} aria-label={`Remover filtro ${filter.label}`} className="inline-flex items-center gap-2 rounded-full border border-brand-blue/20 bg-brand-blue/5 px-3 py-2 text-xs font-bold text-ink transition-colors hover:border-brand-red/50 hover:text-brand-red focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-blue"><span>{filter.label}</span><span aria-hidden="true">×</span></button>)}<button type="button" onClick={() => { setQuery(''); setArea(''); setProvider(''); setCity(''); setModality(''); setExperienceLevel(''); setContractType(''); setWorkSchedule(''); setDistanceKm(''); setCurrentLocation(null); setLocationError(''); setAccessibleOnly(false); setMinimumSalary(''); setMaximumSalary(''); }} className="rounded-full px-3 py-2 text-xs font-extrabold text-brand-red underline underline-offset-2 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-blue">Limpar todos</button></div></div>}
            </div>
            {!isCourse && <>
              <div className="border-t border-white/10 pt-4">
                <button type="button" aria-expanded={advancedOpen} onClick={() => setAdvancedOpen(open => !open)} className="flex w-full items-center justify-between rounded-xl border border-white/10 bg-paper px-4 py-3 text-left font-bold transition-colors hover:border-brand-yellow/40"><span className="flex items-center gap-3"><span className="grid h-8 w-8 place-items-center rounded-full bg-brand-yellow/10 text-brand-yellow" aria-hidden="true">⌕</span><span><span className="block">Filtros adicionais</span><span className="mt-0.5 block text-xs font-medium text-ink/50">Modalidade, experiência, salário e localização</span></span></span><span className="flex items-center gap-3"><span className="hidden rounded-full bg-brand-yellow/10 px-2.5 py-1 text-xs font-bold text-brand-yellow sm:inline">{[modality, experienceLevel, contractType, workSchedule, distanceKm, minimumSalary, maximumSalary].filter(Boolean).length + Number(accessibleOnly)} aplicados</span><span className="text-lg text-brand-yellow" aria-hidden="true">{advancedOpen ? '−' : '+'}</span></span></button>
                {advancedOpen && <div className="mt-4 grid gap-4 rounded-xl border border-white/10 bg-paper/70 p-4 md:grid-cols-3 md:p-5">
                  <label className="text-sm font-bold">Modalidade<select value={modality} onChange={event => setModality(event.target.value)} className="mt-1.5 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal"><option value="">Todas</option><option>Presencial</option><option>Híbrido</option><option>Remoto</option></select></label>
                  <label className="text-sm font-bold">Experiência<select value={experienceLevel} onChange={event => setExperienceLevel(event.target.value)} className="mt-1.5 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal"><option value="">Qualquer nível</option><option>Sem experiência</option><option>Júnior</option><option>Pleno</option><option>Sênior</option></select></label>
                  <label className="text-sm font-bold">Tipo de contrato<select value={contractType} onChange={event => setContractType(event.target.value)} className="mt-1.5 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal"><option value="">Todos os tipos</option><option>CLT</option><option>PJ</option><option>Temporário</option><option>Estágio</option><option>Aprendiz</option><option>Autônomo</option></select></label>
                  <label className="text-sm font-bold">Jornada ou palavra-chave<input value={workSchedule} onChange={event => setWorkSchedule(event.target.value)} placeholder="Ex.: integral, noturno" className="mt-1.5 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal" /></label>
                  <label className="text-sm font-bold">Salário mínimo<input type="number" min="0" value={minimumSalary} onChange={event => setMinimumSalary(event.target.value)} placeholder="R$" className="mt-1.5 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal" /></label>
                  <label className="text-sm font-bold">Salário máximo<input type="number" min="0" value={maximumSalary} onChange={event => setMaximumSalary(event.target.value)} placeholder="R$" className="mt-1.5 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal" /></label>
                  <label className="flex items-center gap-3 rounded-xl border border-white/10 bg-brand-white p-4 text-sm font-semibold md:col-span-1"><input type="checkbox" checked={accessibleOnly} onChange={event => setAccessibleOnly(event.target.checked)} className="accent-lime-400" />Somente vagas com acessibilidade informada</label>
                  <label className="text-sm font-bold">Distância máxima<select value={distanceKm} onChange={event => setDistanceKm(event.target.value)} className="mt-1.5 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal"><option value="">Qualquer distância</option><option value="5">Até 5 km</option><option value="10">Até 10 km</option><option value="25">Até 25 km</option><option value="50">Até 50 km</option></select></label>
                  <div className="flex items-end"><button type="button" onClick={() => { setLocationError(''); if (!navigator.geolocation) { setLocationError('Seu navegador não oferece localização.'); return; } navigator.geolocation.getCurrentPosition(position => setCurrentLocation({ latitude: position.coords.latitude, longitude: position.coords.longitude }), () => setLocationError('Não foi possível acessar sua localização. Permita o acesso no navegador e tente de novo.'), { enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 }); }} className="w-full rounded-xl border border-ink/20 px-4 py-3 text-sm font-bold transition-colors hover:border-brand-yellow/50 hover:text-brand-yellow">{currentLocation ? '✓ Localização ativada' : 'Usar minha localização'}</button></div>
                  {locationError && <p role="status" className="text-sm text-brand-red md:col-span-3">{locationError}</p>}
                  {distanceKm && !currentLocation && <p role="status" className="text-sm text-ink/65 md:col-span-3">Permita o acesso à localização para calcular o raio. As coordenadas ficam apenas nesta sessão e não são enviadas ao Trampolim.</p>}
                </div>}
              </div>
            </>}
          </div>
        </section>
        {catalogError && <div role="alert" className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-red-50 p-4 text-sm font-semibold text-brand-red"><span>{catalogError}</span><button type="button" onClick={() => void loadPublished()} className="rounded-full border border-brand-red/30 px-4 py-2 font-bold">Tentar carregar novamente</button></div>}
        {favoriteError && <div role="alert" className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-red-50 p-4 text-sm font-semibold text-brand-red"><span>{favoriteError}</span><button type="button" onClick={() => void loadFavorites()} className="rounded-full border border-brand-red/30 px-4 py-2 font-bold">Recarregar favoritos</button></div>}
        {applicationError && <div role="alert" className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-red-50 p-4 text-sm font-semibold text-brand-red"><span>{applicationError}</span><button type="button" onClick={() => void loadApplications()} className="rounded-full border border-brand-red/30 px-4 py-2 font-bold">Recarregar candidaturas</button></div>}
        {catalogLoading && <p role="status" aria-live="polite" className="mb-5 rounded-xl bg-brand-white p-4 text-sm text-ink/65">Carregando oportunidades publicadas…</p>}

        <div className="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
          {reportNotice && <p role="status" className="mb-3 rounded-xl bg-green-50 p-3 text-sm font-semibold">{reportNotice}</p>}{reportError && <p role="alert" className="mb-3 rounded-xl bg-red-50 p-3 text-sm font-semibold text-brand-red">{reportError}</p>}
          {visiblePublished.map(item => { const publishedOn = item.publishedAt || item.createdAt; const publishedDate = new Date(publishedOn); const expiresOn = item.expiresAt ? new Date(`${item.expiresAt}T00:00:00`) : null; const deadlineStatus = getOpportunityDeadlineStatus(item.expiresAt); const daysRemaining = item.expiresAt ? getOpportunityDaysRemaining(item.expiresAt) : null; const deadlineText = deadlineStatus === 'today' ? 'Prazo termina hoje' : deadlineStatus === 'closing-soon' && daysRemaining !== null ? `Encerra em ${daysRemaining} dias` : expiresOn ? `Inscrições até ${expiresOn.toLocaleDateString('pt-BR')}` : ''; const linkReview = getLinkReviewMessage(item.linkStatus, item.lastLinkCheckAt); const reason = diagnostic ? getRecommendationReason(diagnostic, [item.area as Skill]) : null; return <article key={`published-${item.id}`} className="flex h-full min-w-0 flex-col rounded-3xl border border-brand-green/30 bg-brand-white p-6 shadow-sm"><p className="text-xs font-extrabold uppercase tracking-wider text-brand-green">{item.provider} · Publicado na plataforma</p><p className="mt-2 text-xs font-semibold text-ink/65"><time dateTime={publishedDate.toISOString()}>Publicado em {publishedDate.toLocaleDateString('pt-BR')}</time></p><div className="mt-2 flex flex-wrap gap-2 text-xs font-bold text-ink/60">{item.location && <span>{item.location}</span>}{distanceKm && currentLocation && item.latitude !== null && item.longitude !== null && <span>· A cerca de {distanceBetween(item.latitude, item.longitude).toFixed(1)} km</span>}{item.modality && <span>· {item.modality}</span>}{item.experienceLevel && <span>· {item.experienceLevel}</span>}{item.contractType && <span>· {item.contractType}</span>}{item.workSchedule && <span>· {item.workSchedule}</span>}</div>{deadlineText && <p className={`mt-3 w-fit rounded-full px-3 py-1.5 text-xs font-extrabold ${deadlineStatus === 'today' || deadlineStatus === 'closing-soon' ? 'bg-brand-red/10 text-brand-red' : 'bg-brand-yellow/20 text-ink'}`}><time dateTime={item.expiresAt || ''}>{deadlineText}</time></p>}{linkReview && <p role="note" className="mt-3 rounded-xl bg-brand-yellow/15 p-3 text-xs font-semibold text-ink/75">{linkReview}</p>}<h3 className="mt-4 text-xl font-black">{item.title}</h3><p className="mt-2 text-sm leading-6 text-ink/65">{item.description}</p>{item.salary && <p className="mt-2 text-sm font-bold">Faixa informada: {item.salary}</p>}{item.accessibility && <div className="mt-4 rounded-xl bg-brand-green/10 p-3 text-sm"><strong>Acessibilidade e inclusão</strong><p className="mt-1">{item.accessibility}</p></div>}{reason && <p className="mt-4 rounded-xl bg-brand-yellow/25 p-3 text-xs font-semibold">Recomendada porque suas respostas destacam {reason.skills.map(skillLabel).join(' e ')} (pontuação {reason.points}).</p>}{!isCourse && <button onClick={() => { recordCatalogMetric('apply', kind, item.id, token); trackApplication(item); }} disabled={applicationsLoading || applications.includes(item.id)} className="mt-4 w-fit rounded-full border border-brand-blue/30 px-4 py-2 text-sm font-bold text-brand-blue disabled:opacity-50">{applicationsLoading ? 'Verificando acompanhamento…' : applications.includes(item.id) ? '✓ Acompanhando' : 'Acompanhar candidatura'}</button>}<a href={item.url} target="_blank" rel="noreferrer" onClick={() => recordCatalogMetric('open', kind, item.id, token)} className={`${buttonClass} mt-4`}>{isCourse ? 'Acessar curso ↗' : 'Ver vaga e candidatar ↗'}</a><button type="button" onClick={() => { setReportingId(reportingId === item.id ? null : item.id); setReportNotice(''); setReportError(''); }} className="mt-4 w-fit rounded-full border border-ink/20 px-4 py-2 text-sm font-semibold">Sinalizar anúncio</button>{reportingId === item.id && <div className="mt-3 rounded-2xl border border-ink/10 bg-paper p-4"><label className="block text-sm font-bold">Motivo<select value={reportReason} onChange={event => setReportReason(event.target.value)} className="mt-1 w-full rounded-xl border border-ink/20 px-3 py-2"><option value="BROKEN_LINK">Link não funciona</option><option value="EXPIRED">Anúncio expirado</option><option value="MISLEADING">Informação incorreta</option><option value="DUPLICATE">Anúncio duplicado</option><option value="OTHER">Outro motivo</option></select></label><label className="mt-3 block text-sm font-bold">Detalhes (opcional)<textarea maxLength={500} value={reportDetails} onChange={event => setReportDetails(event.target.value)} className="mt-1 w-full rounded-xl border border-ink/20 px-3 py-2" rows={3} /></label><button type="button" disabled={reportBusy} onClick={() => void submitReport(item.id)} className="mt-3 rounded-full bg-ink px-4 py-2 text-sm font-bold text-white disabled:opacity-50">{reportBusy ? 'Enviando…' : 'Enviar aviso'}</button></div>}</article>; })}
          {filteredItems.map((item: Opportunity) => <article key={item.id} className="flex h-full flex-col rounded-3xl border border-[#e7e0d0] bg-brand-white p-6 shadow-sm">
            <p className={`text-xs font-extrabold uppercase tracking-wider ${isCourse ? 'text-brand-red' : 'text-brand-blue'}`}>{item.provider}</p>
            <p className="mt-2 text-xs font-bold text-ink/55">{isCourse ? 'Tema de curso · consulte oferta e matrícula na plataforma' : 'Área profissional · consulte anúncios abertos no portal'}</p>
            <h3 className="mt-4 text-xl font-black">{item.title}</h3>
            <p className="mb-6 mt-2 text-sm leading-6 text-ink/65">{item.description}{!isCourse && profile.location ? ` Região informada: ${profile.location}.` : ''}</p>
            {diagnostic && (() => { const reason = getRecommendationReason(diagnostic, item.tags); return reason ? <p className="mb-4 rounded-xl bg-brand-yellow/25 p-3 text-xs font-semibold">Recomendada porque suas respostas destacam {reason.skills.map(skillLabel).join(' e ')} (pontuação {reason.points}); o conteúdo se relaciona a esses interesses.</p> : null; })()}
            <button type="button" aria-pressed={favoriteIds.includes(item.id)} disabled={favoriteLoading} onClick={() => toggleFavorite(item.id)} className="mb-4 w-fit rounded-full border border-brand-red/30 bg-transparent px-4 py-2 text-sm font-bold text-brand-red cursor-pointer disabled:opacity-50">{favoriteLoading ? 'Verificando itens salvos…' : favoriteIds.includes(item.id) ? '♥ Salvo no perfil' : '♡ Salvar no perfil'}</button>
            <a href={item.url} target="_blank" rel="noreferrer" onClick={() => recordCatalogMetric('open', kind, item.id, token)} className={buttonClass}>{isCourse ? 'Acessar plataforma ↗' : 'Pesquisar vagas ↗'}</a>
          </article>)}
        </div>
        {!catalogLoading && filteredItems.length === 0 && visiblePublished.length === 0 && <p role="status" className="rounded-2xl bg-brand-white p-6 text-ink/65">Nenhuma opção encontrada. Ajuste ou limpe os filtros para ver outras opções.</p>}
        {!isCourse && <p className="mt-8 text-sm text-ink/55">As áreas sugeridas apontam caminhos de busca. Vagas com empresa, localidade e candidatura são publicadas pela administração da plataforma e identificadas no catálogo.</p>}
      </main>
    </div>
  );
}
