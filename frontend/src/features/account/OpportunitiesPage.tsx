import { useCallback, useEffect, useState } from 'react';
import { getRecommendationReason, getRecommendations, loadDiagnosticResult } from '../diagnostic/recommendations';
import { api } from './api';
import type { Application, Profile, ProfileResponse } from './api';
import { recordCatalogMetric } from './api';
import { GuidancePanel } from '../diagnostic/GuidancePanel';
import { AccountNavigation } from './AccountNavigation';
import type { AccountDestination } from './AccountNavigation';
import { AccountHeaderActions } from './AccountHeaderActions';
import { RibbonStrip } from '../../components/ui/RibbonStrip';

interface OpportunitiesPageProps {
  token: string;
  profile: Profile;
  onEditProfile: () => void;
  onLogout: () => void;
  onRetakeDiagnostic: () => void;
  onOpenCatalog: (kind: 'course' | 'job') => void;
  canManage?: boolean;
  onNavigate: (destination: AccountDestination) => void;
}

const buttonClass = 'inline-flex w-fit rounded-full bg-ink px-5 py-3 text-sm font-extrabold text-brand-white no-underline transition-transform hover:-translate-y-0.5';
const skillLabels: Record<string, string> = { service: 'atendimento', sales: 'vendas', admin: 'organização', numbers: 'números', logistics: 'logística', maintenance: 'manutenção', production: 'produção', technology: 'tecnologia', health: 'saúde', marketing: 'marketing e comunicação' };
export function OpportunitiesPage({ token, profile, onEditProfile, onLogout, onRetakeDiagnostic, onOpenCatalog, canManage, onNavigate }: OpportunitiesPageProps) {
  const [favoriteIds, setFavoriteIds] = useState<string[]>([]);
  const [favoriteError, setFavoriteError] = useState('');
  const [journeyProfile, setJourneyProfile] = useState<ProfileResponse | null>(null);
  const [applicationCount, setApplicationCount] = useState<number | null>(null);
  const [journeyLoading, setJourneyLoading] = useState(true);
  const [journeyError, setJourneyError] = useState('');
  const diagnostic = loadDiagnosticResult();
  const courses = getRecommendations(diagnostic, 'course');
  const jobs = getRecommendations(diagnostic, 'job');

  const loadFavorites = useCallback(() => {
    setFavoriteError('');
    return api<string[]>('/profile/favorites', token).then(setFavoriteIds).catch(() => setFavoriteError('Não foi possível carregar seus favoritos.'));
  }, [token]);

  const loadJourney = useCallback(() => {
    setJourneyLoading(true);
    setJourneyError('');
    Promise.all([api<ProfileResponse>('/profile', token), api<Application[]>('/profile/applications', token)])
      .then(([savedProfile, applications]) => { setJourneyProfile(savedProfile); setApplicationCount(applications.length); })
      .catch(() => { setJourneyProfile(null); setApplicationCount(null); setJourneyError('Não foi possível carregar o resumo da sua jornada. Verifique sua conexão e tente novamente.'); })
      .finally(() => setJourneyLoading(false));
  }, [token]);

  useEffect(() => {
    void loadFavorites();
    loadJourney();
  }, [loadFavorites, loadJourney]);

  function toggleFavorite(id: string) {
    const previous = favoriteIds;
    const next = previous.includes(id) ? previous.filter(item => item !== id) : [...previous, id];
    setFavoriteIds(next);
    setFavoriteError('');
    api<string[]>('/profile/favorites', token, { method: 'PUT', body: JSON.stringify({ ids: next }) })
      .then(setFavoriteIds).catch(() => { setFavoriteIds(previous); setFavoriteError('Não foi possível salvar o favorito. Tente novamente.'); });
  }

  const profileCompletionItems = journeyProfile ? [Boolean(journeyProfile.profile.name.trim()), Boolean(journeyProfile.profile.phone?.trim()), Boolean(journeyProfile.profile.location?.trim()), Boolean(journeyProfile.profile.summary?.trim()), journeyProfile.experiences.length > 0, journeyProfile.educations.length > 0] : [];
  const profileCompletion = profileCompletionItems.length ? Math.round(profileCompletionItems.filter(Boolean).length / profileCompletionItems.length * 100) : null;

  return (
    <div className="min-h-screen bg-paper">
      <RibbonStrip />
      <a href="#inicio" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand-white focus:px-4 focus:py-3 focus:font-bold">Pular para o conteúdo</a>
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-ink/10 px-[6vw] py-5">
        <a href="#inicio" className="font-serif text-2xl font-black text-ink no-underline">Trampol<span className="text-brand-red">i</span>m</a>
        <AccountHeaderActions onProfile={onEditProfile} onLogout={onLogout} />
      </header>
      <AccountNavigation active="opportunities" onNavigate={onNavigate} canManage={canManage} />

      <main id="inicio" className="mx-auto max-w-7xl px-[6vw] pb-20">
        <section className="relative mt-8 overflow-hidden rounded-[30px] bg-ink px-7 py-10 text-brand-white md:mt-12 md:px-12 md:py-14">
          <div className="absolute -right-8 -top-16 h-64 w-64 rounded-full border border-brand-yellow/20 shadow-[0_0_100px_rgba(214,255,56,0.08)]" aria-hidden="true" />
          <div className="relative max-w-2xl">
            <span className="inline-block rounded-full bg-brand-yellow px-4 py-2 text-xs font-extrabold uppercase tracking-wider text-ink">{diagnostic ? `Trilha: ${diagnostic.trackName}` : 'Seu próximo passo'}</span>
            <h1 className="mt-5 text-4xl font-black leading-tight md:text-5xl">Oportunidades para você, {profile.name.split(' ')[0]}.</h1>
            <p className="mt-4 max-w-xl text-white/75">{diagnostic ? 'Cursos e áreas de trabalho selecionados a partir das suas respostas ao questionário.' : 'Explore todos os cursos e áreas de trabalho disponíveis no catálogo. Responda ao questionário quando quiser receber sugestões personalizadas.'}</p>
            <div className="mt-7 flex flex-wrap gap-3"><button onClick={() => onOpenCatalog('course')} className="rounded-full border-0 bg-brand-yellow px-6 py-3 font-extrabold text-[#101725] cursor-pointer">Explorar cursos</button><button onClick={() => onOpenCatalog('job')} className="rounded-full border border-white/35 bg-transparent px-6 py-3 font-extrabold text-white cursor-pointer">Ver vagas</button><button onClick={onRetakeDiagnostic} className="rounded-full border border-white/35 bg-transparent px-6 py-3 font-extrabold text-white cursor-pointer">Refazer diagnóstico</button></div>
          </div>
        </section>

        {!diagnostic && <div role="status" className="mt-7 rounded-2xl border border-brand-yellow/50 bg-brand-yellow/15 p-5 font-semibold">Você está vendo o catálogo completo. Faça o diagnóstico para filtrar cursos e vagas de acordo com seus interesses.</div>}
        {diagnostic && <GuidancePanel result={diagnostic} />}
        {favoriteError && <p role="alert" className="mt-5 rounded-xl bg-red-50 p-3 text-sm font-semibold text-brand-red">{favoriteError}</p>}
        {favoriteError && <button type="button" onClick={() => void loadFavorites()} className="mt-2 rounded-full border border-brand-red/30 px-4 py-2 text-sm font-bold text-brand-red">Tentar carregar favoritos novamente</button>}

        <section aria-labelledby="journey-title" className="mt-8 rounded-3xl border border-ink/10 bg-brand-white p-6 shadow-sm md:p-8">
          <div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xs font-extrabold uppercase tracking-[0.18em] text-brand-red">Seu painel</p><h2 id="journey-title" className="mt-1 text-2xl font-black md:text-3xl">Minha jornada</h2><p className="mt-2 text-sm text-ink/65">Acompanhe seu perfil, itens salvos e candidaturas em um só lugar.</p></div><button type="button" onClick={onEditProfile} className="rounded-full border border-ink/20 px-4 py-2 text-sm font-bold hover:border-brand-red focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-blue">Abrir meu perfil</button></div>
          {journeyError && <div role="alert" className="mt-5 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-red-50 p-4 text-sm font-semibold text-brand-red"><span>{journeyError}</span><button type="button" onClick={loadJourney} className="rounded-full border border-brand-red/30 px-4 py-2 font-bold">Tentar novamente</button></div>}
          <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <button type="button" onClick={onEditProfile} className="rounded-2xl bg-paper p-4 text-left transition hover:-translate-y-0.5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-blue"><span className="text-xs font-bold uppercase tracking-wider text-ink/55">Perfil</span><strong className="mt-2 block text-2xl">{journeyLoading ? '…' : profileCompletion === null ? '—' : `${profileCompletion}%`}</strong><span className="mt-1 block text-sm text-ink/65">{journeyProfile ? `${journeyProfile.experiences.length} experiências · ${journeyProfile.educations.length} formações` : 'Ver seus dados profissionais'}</span></button>
            <button type="button" onClick={onEditProfile} className="rounded-2xl bg-paper p-4 text-left transition hover:-translate-y-0.5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-blue"><span className="text-xs font-bold uppercase tracking-wider text-ink/55">Itens salvos</span><strong className="mt-2 block text-2xl">{favoriteIds.length}</strong><span className="mt-1 block text-sm text-ink/65">Cursos e áreas para consultar depois</span></button>
            <button type="button" onClick={onEditProfile} className="rounded-2xl bg-paper p-4 text-left transition hover:-translate-y-0.5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-blue"><span className="text-xs font-bold uppercase tracking-wider text-ink/55">Candidaturas</span><strong className="mt-2 block text-2xl">{applicationCount === null ? '…' : applicationCount}</strong><span className="mt-1 block text-sm text-ink/65">Vagas que você está acompanhando</span></button>
            <button type="button" onClick={onRetakeDiagnostic} className="rounded-2xl bg-paper p-4 text-left transition hover:-translate-y-0.5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-blue"><span className="text-xs font-bold uppercase tracking-wider text-ink/55">Diagnóstico</span><strong className="mt-2 block text-2xl">{diagnostic ? 'Concluído' : 'Pendente'}</strong><span className="mt-1 block text-sm text-ink/65">{diagnostic ? `Trilha ${diagnostic.trackName}` : 'Faça o teste para personalizar recomendações'}</span></button>
          </div>
        </section>

        <section id="cursos" className="scroll-mt-8 py-14 md:py-16">
          <p className="mb-2 text-[13px] font-extrabold uppercase tracking-[1.5px] text-brand-red">Aprenda e avance</p>
          <h2 className="text-3xl font-black md:text-4xl">{diagnostic ? 'Cursos que combinam com suas respostas' : 'Todos os cursos disponíveis'}</h2>
          <p className="mt-3 max-w-2xl text-ink/65">Acesse as plataformas parceiras para consultar cursos, requisitos, modalidades e inscrições disponíveis.</p>
          {courses.length ? <div className="mt-8 grid gap-5 md:grid-cols-3">{courses.slice(0, 3).map(course => { const reason = diagnostic ? getRecommendationReason(diagnostic, course.tags) : null; return <article key={course.id} className="flex h-full flex-col rounded-3xl border border-[#e7e0d0] bg-brand-white p-6 shadow-sm"><p className="text-xs font-extrabold tracking-wider text-brand-red">{course.provider}</p><p className="mt-2 text-xs font-bold text-ink/55">Tema de curso · consulte oferta e matrícula na plataforma</p><h3 className="mt-4 text-2xl font-black">{course.title}</h3><p className="mt-2 text-sm leading-6 text-ink/65">{course.description}</p>{reason && <p className="mb-4 mt-3 rounded-xl bg-brand-green/10 p-3 text-sm font-semibold text-ink/75">Recomendado pelas suas respostas: {reason.skills.map(skill => skillLabels[skill]).join(' e ')}.</p>}<button type="button" aria-pressed={favoriteIds.includes(course.id)} onClick={() => toggleFavorite(course.id)} className="mb-4 mt-auto w-fit rounded-full border border-brand-red/30 bg-transparent px-4 py-2 text-sm font-bold text-brand-red cursor-pointer">{favoriteIds.includes(course.id) ? '♥ Salvo no perfil' : '♡ Salvar no perfil'}</button><a href={course.url} target="_blank" rel="noreferrer" onClick={() => recordCatalogMetric('open', 'course', course.id, token)} className={buttonClass}>Acessar plataforma ↗</a></article>; })}</div> : <p className="mt-6 rounded-2xl bg-brand-white p-5 text-ink/60">Nenhum curso compatível no catálogo.</p>}
          {courses.length > 3 && <button onClick={() => onOpenCatalog('course')} className="mt-6 rounded-full border-2 border-ink px-6 py-3 font-extrabold text-ink cursor-pointer">Ver mais cursos</button>}
        </section>

        <section id="vagas" className="scroll-mt-8 border-t border-ink/10 py-14 md:py-16">
          <p className="mb-2 text-[13px] font-extrabold uppercase tracking-[1.5px] text-brand-red">Oportunidades de trabalho</p>
          <h2 className="text-3xl font-black md:text-4xl">{diagnostic ? 'Áreas e vagas recomendadas' : 'Todas as áreas de vagas disponíveis'}</h2>
          <p className="mt-3 max-w-2xl text-ink/65">{diagnostic ? 'Estas áreas foram selecionadas conforme suas respostas. Consulte vagas reais e candidaturas no serviço oficial de emprego.' : 'Explore todas as áreas do catálogo e consulte vagas reais e candidaturas no serviço oficial de emprego.'}</p>
          {jobs.length ? <div className="mt-8 grid gap-5 md:grid-cols-3">{jobs.slice(0, 3).map(job => { const reason = diagnostic ? getRecommendationReason(diagnostic, job.tags) : null; return <article key={job.id} className="flex h-full flex-col rounded-3xl border border-[#e7e0d0] bg-brand-white p-6 shadow-sm"><p className="text-xs font-extrabold uppercase tracking-wider text-brand-blue">{job.provider}</p><p className="mt-2 text-xs font-bold text-ink/55">Área profissional · consulte anúncios abertos no portal</p><h3 className="mt-4 text-xl font-black">{job.title}</h3><p className="mt-2 text-sm leading-6 text-ink/65">{job.description}{profile.location ? ` Região informada: ${profile.location}.` : ''}</p>{reason && <p className="mb-4 mt-3 rounded-xl bg-brand-green/10 p-3 text-sm font-semibold text-ink/75">Recomendada pelas suas respostas: {reason.skills.map(skill => skillLabels[skill]).join(' e ')}.</p>}<button type="button" aria-pressed={favoriteIds.includes(job.id)} onClick={() => toggleFavorite(job.id)} className="mb-4 mt-auto w-fit rounded-full border border-brand-red/30 bg-transparent px-4 py-2 text-sm font-bold text-brand-red cursor-pointer">{favoriteIds.includes(job.id) ? '♥ Salvo no perfil' : '♡ Salvar no perfil'}</button><a href={job.url} target="_blank" rel="noreferrer" onClick={() => recordCatalogMetric('open', 'job', job.id, token)} className={buttonClass}>Pesquisar vagas ↗</a></article>; })}</div> : <p className="mt-6 rounded-2xl bg-brand-white p-5 text-ink/60">Nenhuma vaga compatível no catálogo.</p>}
          {jobs.length > 3 && <button onClick={() => onOpenCatalog('job')} className="mt-6 rounded-full border-2 border-ink px-6 py-3 font-extrabold text-ink cursor-pointer">Ver mais vagas</button>}
          <p className="mt-5 text-xs text-ink/50">As recomendações indicam áreas de interesse. A disponibilidade de vagas e cursos depende das plataformas parceiras.</p>
        </section>

        <footer className="rounded-3xl border border-brand-green/25 bg-brand-green/10 px-7 py-8 md:px-10"><p className="text-xs font-extrabold uppercase tracking-widest text-brand-green">Continue construindo</p><h2 className="mt-2 text-2xl font-black">Seu perfil profissional cresce junto com você.</h2><p className="mt-2 max-w-2xl text-sm text-ink/65">Quando quiser, atualize suas experiências e formações pelo atalho “Meu perfil” no cabeçalho.</p></footer>
      </main>
    </div>
  );
}
