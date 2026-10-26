import { useCallback, useEffect, useState } from 'react';
import type { ChangeEvent, FormEvent, InputHTMLAttributes, ReactNode } from 'react';
import { API_URL, api } from './api';
import type { Application, AlertPreferences, Education, Experience, Profile, ProfileResponse } from './api';
import { opportunities } from '../diagnostic/recommendations';
import { AccountNavigation } from './AccountNavigation';
import type { AccountDestination } from './AccountNavigation';
import { AccountHeaderActions } from './AccountHeaderActions';
import { RibbonStrip } from '../../components/ui/RibbonStrip';

interface ProfilePageProps {
  token: string;
  onLogout: () => void;
  onBack: () => void;
  onComplete: (profile: Profile) => void;
  onNavigate: (destination: AccountDestination) => void;
  canManage?: boolean;
}

const emptyExperience = { position: '', company: '', startDate: '', endDate: '', current: false, description: '' };
const emptyEducation = { institution: '', course: '', level: '', startDate: '', endDate: '', inProgress: false };
const profileAreas = [{ value: 'service', label: 'Atendimento' }, { value: 'sales', label: 'Vendas' }, { value: 'admin', label: 'Administração' }, { value: 'numbers', label: 'Finanças e números' }, { value: 'logistics', label: 'Logística' }, { value: 'maintenance', label: 'Manutenção' }, { value: 'production', label: 'Produção' }, { value: 'technology', label: 'Tecnologia e informática' }, { value: 'health', label: 'Saúde e bem-estar' }, { value: 'marketing', label: 'Marketing e comunicação' }];

function Field({ label, ...props }: { label: string } & InputHTMLAttributes<HTMLInputElement>) {
  return <label className="block text-sm font-bold text-ink">{label}<input {...props} className={`mt-2 w-full rounded-xl border border-ink/15 bg-white px-4 py-3 font-normal outline-none focus:border-brand-blue ${props.className ?? ''}`} /></label>;
}

function Card({ title, description, children, defaultOpen = false }: { title: string; description?: string; children: ReactNode; defaultOpen?: boolean }) {
  return <details open={defaultOpen || undefined} className="group rounded-3xl border border-white/10 bg-[#121c2b] p-5 shadow-sm md:p-6"><summary className="flex cursor-pointer list-none items-center justify-between gap-4 [&::-webkit-details-marker]:hidden"><span><span className="block text-xl font-black">{title}</span>{description && <span className="mt-1 block text-sm font-normal text-ink/60">{description}</span>}</span><span aria-hidden="true" className="grid h-9 w-9 shrink-0 place-items-center rounded-full border border-white/10 text-brand-yellow transition-transform group-open:rotate-180">⌄</span></summary><div className="mt-5 border-t border-white/10 pt-5">{children}</div></details>;
}

export function ProfilePage({ token, onLogout, onBack, onComplete, onNavigate, canManage }: ProfilePageProps) {
  const [data, setData] = useState<ProfileResponse | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [experience, setExperience] = useState(emptyExperience);
  const [education, setEducation] = useState(emptyEducation);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [favoriteIds, setFavoriteIds] = useState<string[]>([]);
  const [favoriteError, setFavoriteError] = useState('');
  const [alerts, setAlerts] = useState<AlertPreferences>({ enabled: false, areas: [], frequency: 'weekly', city: '', modality: '', contractType: '', workSchedule: '', keywords: '', useDistance: false, latitude: null, longitude: null, radiusKm: null });
  const [applications, setApplications] = useState<Application[]>([]);
  const [shareEnabled, setShareEnabled] = useState(false);
  const [shareLocation, setShareLocation] = useState(false);
  const [shareId, setShareId] = useState('');
  const [shareSummary, setShareSummary] = useState(true);
  const [shareExperiences, setShareExperiences] = useState(true);
  const [shareEducation, setShareEducation] = useState(true);
  const [deletePassword, setDeletePassword] = useState('');
  const [deleteConfirmation, setDeleteConfirmation] = useState('');

  const refresh = useCallback(async () => {
    const result = await api<ProfileResponse>('/profile', token);
    setData(result);
    setProfile(result.profile);
  }, [token]);

  useEffect(() => {
    refresh().catch(cause => setError(cause.message));
    api<string[]>('/profile/favorites', token).then(setFavoriteIds).catch(() => setFavoriteError('Não foi possível carregar os itens salvos.'));
    api<Partial<AlertPreferences>>('/profile/alerts', token).then(value => setAlerts({ enabled: value.enabled ?? false, areas: value.areas ?? [], frequency: value.frequency ?? 'weekly', city: value.city ?? '', modality: value.modality ?? '', contractType: value.contractType ?? '', workSchedule: value.workSchedule ?? '', keywords: value.keywords ?? '', useDistance: value.useDistance ?? false, latitude: value.latitude ?? null, longitude: value.longitude ?? null, radiusKm: value.radiusKm ?? null })).catch(() => undefined);
    api<Application[]>('/profile/applications', token).then(setApplications).catch(() => undefined);
    api<{ shareId: string | null; enabled: boolean; showLocation: boolean; showSummary: boolean; showExperiences: boolean; showEducation: boolean }>('/profile/public', token).then(settings => { setShareId(settings.shareId || ''); setShareEnabled(settings.enabled); setShareLocation(settings.showLocation); setShareSummary(settings.showSummary); setShareExperiences(settings.showExperiences); setShareEducation(settings.showEducation); }).catch(() => undefined);
  }, [token, refresh]);

  function runAction(action: () => Promise<unknown>, successMessage: string) {
    setBusy(true); setError(''); setNotice('');
    action().then(async () => { await refresh(); setNotice(successMessage); }).catch(cause => setError(cause.message)).finally(() => setBusy(false));
  }

  function enableDistanceAlerts() {
    if (!navigator.geolocation) { setError('Seu navegador não oferece acesso à localização.'); return; }
    navigator.geolocation.getCurrentPosition(position => {
      setAlerts(current => ({ ...current, useDistance: true, latitude: position.coords.latitude, longitude: position.coords.longitude, radiusKm: current.radiusKm || 10 }));
      setError('');
    }, () => setError('Permita o acesso à localização no navegador para ativar alertas por distância.'), { enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 });
  }

  function disableDistanceAlerts() {
    setAlerts(current => ({ ...current, useDistance: false, latitude: null, longitude: null, radiusKm: null }));
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!profile) return;
    runAction(() => api('/profile', token, { method: 'PUT', body: JSON.stringify({ name: profile.name, phone: profile.phone, location: profile.location, summary: profile.summary }) }), 'Perfil atualizado.');
  }

  async function addExperience(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    runAction(() => api('/profile/experiences', token, { method: 'POST', body: JSON.stringify(experience) }), 'Experiência adicionada.');
    setExperience(emptyExperience);
  }

  async function addEducation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    runAction(() => api('/profile/educations', token, { method: 'POST', body: JSON.stringify(education) }), 'Formação adicionada.');
    setEducation(emptyEducation);
  }

  async function uploadResume(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) { setError('Selecione um arquivo PDF.'); return; }
    if (file.size > 10 * 1024 * 1024) { setError('O arquivo deve ter no máximo 10 MB.'); return; }
    const form = new FormData(); form.append('file', file);
    runAction(() => api('/profile/resume', token, { method: 'POST', body: form }), 'Currículo enviado.');
    event.target.value = '';
  }

  async function downloadResume() {
    try {
      const response = await fetch(`${API_URL}/profile/resume`, { headers: { Authorization: `Bearer ${token}` } });
      if (!response.ok) throw new Error('Não foi possível baixar o currículo.');
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a'); link.href = url; link.download = profile?.resumeFileName || 'curriculo.pdf'; link.click();
      URL.revokeObjectURL(url);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Falha ao baixar o currículo.'); }
  }

  async function confirmProfile() {
    if (!profile) return;
    setBusy(true); setError(''); setNotice('');
    try {
      const savedProfile = await api<Profile>('/profile', token, { method: 'PUT', body: JSON.stringify({ name: profile.name, phone: profile.phone, location: profile.location, summary: profile.summary }) });
      onComplete(savedProfile);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível confirmar seu perfil.');
    } finally {
      setBusy(false);
    }
  }

  async function removeFavorite(id: string) {
    const next = favoriteIds.filter(item => item !== id);
    setFavoriteIds(next);
    try {
      const saved = await api<string[]>('/profile/favorites', token, { method: 'PUT', body: JSON.stringify({ ids: next }) });
      setFavoriteIds(saved);
      setFavoriteError('');
    } catch {
      setFavoriteIds(favoriteIds);
      setFavoriteError('Não foi possível atualizar seus itens salvos. Tente novamente.');
    }
  }

  async function exportAccountData() {
    try {
      const data = await api<Record<string, unknown>>('/profile/export', token);
      const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }));
      const link = document.createElement('a'); link.href = url; link.download = 'meus-dados-trampolim.json'; link.click(); URL.revokeObjectURL(url);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível exportar seus dados.'); }
  }

  async function deleteAccount(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (deleteConfirmation !== 'EXCLUIR') return;
    setBusy(true); setError('');
    try { await api('/profile', token, { method: 'DELETE', body: JSON.stringify({ password: deletePassword }) }); onLogout(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível excluir sua conta.'); }
    finally { setBusy(false); }
  }

  async function saveApplicationDetails(application: Application) {
    try {
      const saved = await api<Application>(`/profile/applications/${application.id}/details`, token, { method: 'PUT', body: JSON.stringify({ notes: application.notes || '', followUpDate: application.followUpDate || null }) });
      setApplications(list => list.map(item => item.id === saved.id ? saved : item)); setNotice('Anotações e lembrete atualizados.');
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível salvar o lembrete.'); }
  }

  if (!profile || !data) return <main className="min-h-screen p-10 text-center">Carregando seu perfil...</main>;

  const completionItems = [Boolean(profile.name.trim()), Boolean(profile.phone?.trim()), Boolean(profile.location?.trim()), Boolean(profile.summary?.trim()), data.experiences.length > 0, data.educations.length > 0];
  const completion = Math.round(completionItems.filter(Boolean).length / completionItems.length * 100);
  const favoriteOpportunities = opportunities.filter(item => favoriteIds.includes(item.id));
  const localDate = new Date();
  const todayDate = `${localDate.getFullYear()}-${String(localDate.getMonth() + 1).padStart(2, '0')}-${String(localDate.getDate()).padStart(2, '0')}`;
  const overdueFollowUps = applications.filter(item => item.followUpDate && item.followUpDate < todayDate && item.status !== 'REJECTED' && item.status !== 'OFFER').length;

  return (
    <div className="min-h-screen pb-16">
      <RibbonStrip />
      <a href="#perfil" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand-white focus:px-4 focus:py-3 focus:font-bold">Pular para o conteúdo</a>
      <header className="flex items-center justify-between border-b border-ink/10 px-[6vw] py-5">
        <button onClick={onBack} className="border-0 bg-transparent font-serif text-2xl font-black text-ink cursor-pointer">Trampol<span className="text-brand-red">i</span>m</button>
        <div className="flex items-center gap-3"><span className="hidden text-sm font-semibold md:inline">Olá, {profile.name.split(' ')[0]}</span><AccountHeaderActions onProfile={() => onNavigate('profile')} onLogout={onLogout} /></div>
      </header>
      <AccountNavigation active="profile" onNavigate={onNavigate} canManage={canManage} />

      <main id="perfil" className="mx-auto max-w-5xl px-[6vw]">
        <div className="py-8 md:py-10"><p className="text-sm font-extrabold uppercase tracking-[0.18em] text-brand-green">Sua jornada profissional</p><h1 className="mt-2 text-4xl font-black md:text-5xl">Meu perfil</h1><p className="mt-3 max-w-2xl text-ink/65">Seus dados, experiências, formação e preferências em um só lugar.</p></div>
        {notice && <p role="status" className="mb-5 rounded-xl bg-green-50 p-4 font-semibold text-brand-green">{notice}</p>}
        {error && <p role="alert" className="mb-5 rounded-xl bg-red-50 p-4 font-semibold text-brand-red">{error}</p>}
        {favoriteError && <p role="alert" className="mb-5 rounded-xl bg-red-50 p-4 font-semibold text-brand-red">{favoriteError}</p>}
        <section className="mb-6 rounded-3xl border border-ink/10 bg-brand-white p-6 shadow-sm" aria-label="Progresso do perfil">
          <div className="flex items-center justify-between gap-4"><div><h2 className="text-xl font-black">Perfil profissional</h2><p className="mt-1 text-sm text-ink/60">{completion}% preenchido</p></div><span className="text-sm font-bold text-brand-green">{completion === 100 ? 'Perfil completo' : 'Continue quando puder'}</span></div>
          <div className="mt-4 h-3 overflow-hidden rounded-full bg-[#e7e0d0]" role="progressbar" aria-label="Progresso do perfil" aria-valuemin={0} aria-valuemax={100} aria-valuenow={completion}><div className="h-full rounded-full bg-brand-green transition-[width]" style={{ width: `${completion}%` }} /></div>
          <p className="mt-3 text-xs text-ink/55">Foto e currículo são opcionais e não reduzem seu progresso.</p>
        </section>
        {completion < 100 && <details open className="group mb-6 rounded-3xl border border-brand-blue/20 bg-brand-blue/5 p-5">
          <summary className="flex cursor-pointer list-none items-center justify-between gap-4 [&::-webkit-details-marker]:hidden"><span><span className="block font-extrabold">Sugestões para completar seu perfil</span><span className="mt-1 block text-sm font-normal text-ink/65">Orientações opcionais para apresentar melhor sua trajetória.</span></span><span aria-hidden="true" className="grid h-9 w-9 shrink-0 place-items-center rounded-full border border-white/10 text-brand-yellow transition-transform group-open:rotate-45">+</span></summary>
          <ul className="mt-4 grid gap-x-8 divide-y divide-white/10 md:grid-cols-2 md:divide-y-0">
            {!profile.summary?.trim() && <li className="py-3"><p className="font-bold">Conte um pouco sobre você</p><p className="mt-1 text-sm text-ink/65">Uma apresentação curta destaca seus interesses e objetivos.</p><a className="mt-2 inline-block text-sm font-bold text-brand-blue underline underline-offset-2" href="#sobre">Escrever apresentação</a></li>}
            {!profile.phone?.trim() && <li className="py-3"><p className="font-bold">Adicione um telefone de contato</p><p className="mt-1 text-sm text-ink/65">Empresas poderão entrar em contato; seu telefone continua privado no link público.</p><a className="mt-2 inline-block text-sm font-bold text-brand-blue underline underline-offset-2" href="#sobre">Adicionar telefone</a></li>}
            {!profile.location?.trim() && <li className="py-3"><p className="font-bold">Informe sua cidade ou região</p><p className="mt-1 text-sm text-ink/65">A localização ajuda a encontrar oportunidades próximas.</p><a className="mt-2 inline-block text-sm font-bold text-brand-blue underline underline-offset-2" href="#sobre">Adicionar localização</a></li>}
            {!data.experiences.length && <li className="py-3"><p className="font-bold">Registre uma experiência</p><p className="mt-1 text-sm text-ink/65">Trabalho voluntário, projetos e atividades informais também contam.</p><a className="mt-2 inline-block text-sm font-bold text-brand-blue underline underline-offset-2" href="#experiencias">Adicionar experiência</a></li>}
            {!data.educations.length && <li className="py-3"><p className="font-bold">Inclua sua formação</p><p className="mt-1 text-sm text-ink/65">Adicione estudos concluídos ou em andamento, inclusive cursos livres.</p><a className="mt-2 inline-block text-sm font-bold text-brand-blue underline underline-offset-2" href="#formacoes">Adicionar formação</a></li>}
            <li className="py-3"><p className="font-bold">Foto e currículo são opcionais</p><p className="mt-1 text-sm text-ink/65">Você pode adicioná-los quando quiser; não são necessários para usar a plataforma.</p><a className="mt-2 inline-block text-sm font-bold text-brand-blue underline underline-offset-2" href="#curriculo">Ver currículo</a></li>
          </ul>
        </details>}
        <div className="space-y-4">
          <div id="sobre"><Card defaultOpen title="Sobre você" description="Essas informações ajudam empresas a conhecer sua trajetória.">
            <form onSubmit={saveProfile} className="space-y-4">
              <Field label="Nome completo" required value={profile.name} onChange={e => setProfile({ ...profile, name: e.target.value })} />
              <div className="grid gap-4 md:grid-cols-2"><Field label="E-mail" type="email" value={profile.email} disabled /><Field label="Telefone" type="tel" placeholder="(11) 99999-9999" value={profile.phone || ''} onChange={e => setProfile({ ...profile, phone: e.target.value })} /></div>
              <Field label="Cidade e estado" placeholder="Ex.: Recife, PE" value={profile.location || ''} onChange={e => setProfile({ ...profile, location: e.target.value })} />
              <label className="block text-sm font-bold">Apresentação<textarea rows={4} maxLength={2000} placeholder="Fale um pouco sobre você, seus interesses e objetivos." value={profile.summary || ''} onChange={e => setProfile({ ...profile, summary: e.target.value })} className="mt-2 w-full rounded-xl border border-ink/15 bg-white px-4 py-3 font-normal outline-none focus:border-brand-blue" /></label>
              <button disabled={busy} className="rounded-full border-0 bg-ink px-6 py-3 font-extrabold text-white cursor-pointer disabled:opacity-50">Salvar perfil</button>
            </form>
          </Card></div>

          <div id="experiencias"><Card title="Experiências" description="Inclua empregos, estágios, trabalhos voluntários e projetos.">
            {data.experiences.length > 0 && <div className="mb-6 space-y-3">{data.experiences.map((item: Experience) => <article key={item.id} className="flex items-start justify-between gap-4 rounded-2xl bg-paper p-4"><div><h3 className="font-extrabold">{item.position}</h3><p className="text-sm">{item.company} · {item.startDate || 'Data não informada'} – {item.current ? 'Atual' : item.endDate || 'Atual'}</p>{item.description && <p className="mt-2 text-sm text-ink/65">{item.description}</p>}</div><button onClick={() => runAction(() => api(`/profile/experiences/${item.id}`, token, { method: 'DELETE' }), 'Experiência removida.')} aria-label="Remover experiência" className="border-0 bg-transparent text-lg text-brand-red cursor-pointer">×</button></article>)}</div>}
            <form onSubmit={addExperience} className="space-y-4 border-t border-ink/10 pt-6">
              <div className="grid gap-4 md:grid-cols-2"><Field label="Cargo" required placeholder="Ex.: Assistente administrativo" value={experience.position} onChange={e => setExperience({ ...experience, position: e.target.value })} /><Field label="Empresa ou organização" required placeholder="Nome do local" value={experience.company} onChange={e => setExperience({ ...experience, company: e.target.value })} /></div>
              <div className="grid gap-4 md:grid-cols-2"><Field label="Início" type="month" value={experience.startDate} onChange={e => setExperience({ ...experience, startDate: e.target.value })} /><Field label="Fim" type="month" disabled={experience.current} value={experience.endDate} onChange={e => setExperience({ ...experience, endDate: e.target.value })} /></div>
              <label className="flex items-center gap-2 text-sm font-semibold"><input type="checkbox" checked={experience.current} onChange={e => setExperience({ ...experience, current: e.target.checked })} />Trabalho aqui atualmente</label>
              <label className="block text-sm font-bold">O que você fazia?<textarea rows={3} value={experience.description} onChange={e => setExperience({ ...experience, description: e.target.value })} className="mt-2 w-full rounded-xl border border-ink/15 bg-white px-4 py-3 font-normal" /></label>
              <button disabled={busy} className="rounded-full border-2 border-ink bg-transparent px-6 py-3 font-extrabold text-ink cursor-pointer disabled:opacity-50">+ Adicionar experiência</button>
            </form>
          </Card></div>

          <div id="formacoes"><Card title="Formação" description="Cursos, ensino médio, faculdade e outras formações.">
            {data.educations.length > 0 && <div className="mb-6 space-y-3">{data.educations.map((item: Education) => <article key={item.id} className="flex items-start justify-between gap-4 rounded-2xl bg-paper p-4"><div><h3 className="font-extrabold">{item.course}</h3><p className="text-sm">{item.institution}{item.level ? ` · ${item.level}` : ''}</p><p className="text-sm">{item.startDate || 'Data não informada'} – {item.inProgress ? 'Em andamento' : item.endDate || 'Concluído'}</p></div><button onClick={() => runAction(() => api(`/profile/educations/${item.id}`, token, { method: 'DELETE' }), 'Formação removida.')} aria-label="Remover formação" className="border-0 bg-transparent text-lg text-brand-red cursor-pointer">×</button></article>)}</div>}
            <form onSubmit={addEducation} className="space-y-4 border-t border-ink/10 pt-6">
              <div className="grid gap-4 md:grid-cols-2"><Field label="Curso ou formação" required placeholder="Ex.: Técnico em Administração" value={education.course} onChange={e => setEducation({ ...education, course: e.target.value })} /><Field label="Instituição" required placeholder="Escola, faculdade ou plataforma" value={education.institution} onChange={e => setEducation({ ...education, institution: e.target.value })} /></div>
              <Field label="Nível ou tipo" placeholder="Ex.: Técnico, graduação, curso livre" value={education.level} onChange={e => setEducation({ ...education, level: e.target.value })} />
              <div className="grid gap-4 md:grid-cols-2"><Field label="Início" type="month" value={education.startDate} onChange={e => setEducation({ ...education, startDate: e.target.value })} /><Field label="Conclusão" type="month" disabled={education.inProgress} value={education.endDate} onChange={e => setEducation({ ...education, endDate: e.target.value })} /></div>
              <label className="flex items-center gap-2 text-sm font-semibold"><input type="checkbox" checked={education.inProgress} onChange={e => setEducation({ ...education, inProgress: e.target.checked })} />Estou cursando</label>
              <button disabled={busy} className="rounded-full border-2 border-ink bg-transparent px-6 py-3 font-extrabold text-ink cursor-pointer disabled:opacity-50">+ Adicionar formação</button>
            </form>
          </Card></div>

          <div id="curriculo"><Card title="Currículo (opcional)" description="Se quiser, envie seu currículo em PDF (até 10 MB). Você também pode fazer isso depois.">
            <div className="flex flex-col items-start justify-between gap-4 rounded-2xl bg-paper p-5 md:flex-row md:items-center"><div><p className="font-extrabold">{profile.resumeFileName || 'Nenhum currículo enviado'}</p><p className="mt-1 text-sm text-ink/60">{profile.resumeFileName ? 'Seu arquivo está salvo no seu perfil.' : 'Escolha um arquivo PDF para anexar.'}</p><p className="mt-2 text-xs font-semibold text-ink/60">Seu currículo é privado e não aparece no perfil compartilhado por link.</p></div><div className="flex flex-wrap gap-2">{profile.resumeFileName && <><button onClick={downloadResume} className="rounded-full border border-ink/20 bg-white px-4 py-2 text-sm font-bold cursor-pointer">Baixar</button><button onClick={() => runAction(() => api('/profile/resume', token, { method: 'DELETE' }), 'Currículo removido.')} className="rounded-full border border-brand-red/30 bg-white px-4 py-2 text-sm font-bold text-brand-red cursor-pointer">Remover</button></>}<label className="rounded-full bg-brand-green px-4 py-2 text-sm font-extrabold text-white cursor-pointer">{profile.resumeFileName ? 'Trocar PDF' : 'Enviar PDF'}<input type="file" accept="application/pdf,.pdf" onChange={uploadResume} className="hidden" /></label></div></div>
          </Card></div>
          <Card title="Itens salvos" description="Cursos e áreas profissionais que você favoritou para consultar depois.">
            {favoriteOpportunities.length ? <div className="space-y-3">{favoriteOpportunities.map(item => <article key={item.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-paper p-4"><div><p className="text-xs font-extrabold uppercase tracking-wider text-brand-red">{item.provider}</p><h3 className="mt-1 font-extrabold">{item.title}</h3></div><div className="flex items-center gap-2"><a href={item.url} target="_blank" rel="noreferrer" className="rounded-full bg-ink px-4 py-2 text-sm font-bold text-white no-underline">{item.kind === 'course' ? 'Abrir curso ↗' : 'Pesquisar vagas ↗'}</a><button type="button" onClick={() => removeFavorite(item.id)} aria-label={`Remover ${item.title} dos itens salvos`} className="rounded-full border border-brand-red/30 bg-white px-4 py-2 text-sm font-bold text-brand-red cursor-pointer">Remover</button></div></article>)}</div> : <p className="rounded-2xl bg-paper p-4 text-sm text-ink/60">Você ainda não salvou nenhum item. Use “Salvar no perfil” nos catálogos.</p>}
          </Card>
          <Card title="Acompanhamento de candidaturas" description="Registre seu progresso nas vagas publicadas pela plataforma.">
            {overdueFollowUps > 0 && <p role="status" className="mb-4 rounded-xl border border-brand-red/20 bg-red-50 p-3 text-sm font-semibold text-brand-red">Você tem {overdueFollowUps} lembrete{overdueFollowUps === 1 ? '' : 's'} de candidatura em atraso. Revise o próximo passo ou atualize a data.</p>}
            {applications.length ? <div className="space-y-3">{applications.map(application => <article key={application.id} className="rounded-2xl bg-paper p-4"><div className="flex flex-wrap items-center justify-between gap-4"><div><h3 className="font-extrabold">{application.title}</h3><p className="text-sm text-ink/60">{application.provider}{application.location ? ` · ${application.location}` : ''}</p><p className="mt-1 text-xs text-ink/55">{application.followUpDate ? `Lembrete: ${new Date(`${application.followUpDate}T12:00:00`).toLocaleDateString('pt-BR')}` : 'Sem lembrete marcado'}</p></div><div className="flex flex-wrap items-center gap-2"><select aria-label={`Etapa de ${application.title}`} value={application.status} onChange={event => api<Application>(`/profile/applications/${application.id}`, token, { method: 'PUT', body: JSON.stringify({ status: event.target.value }) }).then(saved => setApplications(list => list.map(item => item.id === saved.id ? saved : item))).catch(cause => setError(cause.message))} className="rounded-xl border border-ink/20 bg-white px-3 py-2 text-sm"><option value="SAVED">Para acompanhar</option><option value="APPLIED">Candidatura enviada</option><option value="INTERVIEW">Entrevista</option><option value="OFFER">Proposta recebida</option><option value="REJECTED">Encerrada</option></select><a href={application.url} target="_blank" rel="noreferrer" className="text-sm font-bold text-brand-blue">Abrir ↗</a><button onClick={() => api(`/profile/applications/${application.id}`, token, { method: 'DELETE' }).then(() => setApplications(list => list.filter(item => item.id !== application.id))).catch(cause => setError(cause.message))} aria-label={`Remover ${application.title} do acompanhamento`} className="border-0 bg-transparent text-brand-red">Remover</button></div></div><div className="mt-4 grid gap-3 sm:grid-cols-[1fr_auto]"><label className="text-sm font-bold">Notas privadas<textarea maxLength={2000} rows={2} value={application.notes || ''} onChange={event => setApplications(list => list.map(item => item.id === application.id ? { ...item, notes: event.target.value } : item))} placeholder="Anote contatos, próximos passos ou detalhes para lembrar." className="mt-1 w-full rounded-xl border border-ink/20 bg-white px-3 py-2 font-normal" /></label><div className="flex flex-col justify-end gap-2"><label className="text-sm font-bold">Lembrar em<input aria-label={`Data para lembrar sobre ${application.title}`} type="date" value={application.followUpDate || ''} onChange={event => setApplications(list => list.map(item => item.id === application.id ? { ...item, followUpDate: event.target.value || null } : item))} className="mt-1 rounded-xl border border-ink/20 bg-white px-3 py-2 font-normal" /></label><button onClick={() => saveApplicationDetails(application)} className="rounded-full bg-ink px-4 py-2 text-sm font-bold text-white">Salvar nota e lembrete</button></div></div><p className="mt-2 text-xs text-ink/50">As notas são privadas. Se o e-mail estiver configurado, enviaremos um lembrete no dia escolhido.</p></article>)}</div> : <p className="rounded-2xl bg-paper p-4 text-sm text-ink/60">As vagas publicadas pela plataforma que você decidir acompanhar aparecerão aqui.</p>}
          </Card>
          <Card title="Alertas de oportunidades" description="Escolha os critérios de vaga e a frequência dos avisos. Se ativados, alertas por distância usam coordenadas privadas e vagas com local confirmado.">
            <div className="space-y-4"><label className="flex items-center gap-3 font-semibold"><input type="checkbox" checked={alerts.enabled} onChange={event => setAlerts(current => event.target.checked ? { ...current, enabled: true } : { ...current, enabled: false, useDistance: false, latitude: null, longitude: null, radiusKm: null })} />Receber alertas por e-mail</label><div className="grid gap-3 sm:grid-cols-2">{profileAreas.map(skill => <label key={skill.value} className="flex items-center gap-2 text-sm"><input type="checkbox" checked={alerts.areas.includes(skill.value)} onChange={event => setAlerts({ ...alerts, areas: event.target.checked ? [...new Set([...alerts.areas, skill.value])] : alerts.areas.filter(area => area !== skill.value) })} />{skill.label}</label>)}</div><div className="grid gap-3 sm:grid-cols-2"><Field label="Cidade ou região" value={alerts.city} maxLength={120} onChange={event => setAlerts({ ...alerts, city: event.target.value })} placeholder="Ex.: Recife" /><Field label="Palavras-chave" value={alerts.keywords} maxLength={160} onChange={event => setAlerts({ ...alerts, keywords: event.target.value })} placeholder="Ex.: auxiliar, atendimento" /><label className="block text-sm font-bold">Modalidade<select value={alerts.modality} onChange={event => setAlerts({ ...alerts, modality: event.target.value })} className="mt-2 w-full rounded-xl border border-ink/15 bg-white px-4 py-3 font-normal"><option value="">Qualquer modalidade</option><option>Presencial</option><option>Híbrido</option><option>Remoto</option></select></label><label className="block text-sm font-bold">Tipo de contrato<select value={alerts.contractType} onChange={event => setAlerts({ ...alerts, contractType: event.target.value })} className="mt-2 w-full rounded-xl border border-ink/15 bg-white px-4 py-3 font-normal"><option value="">Qualquer tipo</option><option>CLT</option><option>PJ</option><option>Temporário</option><option>Estágio</option><option>Aprendiz</option><option>Autônomo</option></select></label><Field label="Jornada" value={alerts.workSchedule} maxLength={80} onChange={event => setAlerts({ ...alerts, workSchedule: event.target.value })} placeholder="Ex.: integral, noturno" /></div><section className="space-y-3 rounded-2xl bg-paper p-4"><label className="flex items-start gap-3 text-sm font-semibold"><input type="checkbox" checked={alerts.useDistance} disabled={!alerts.enabled} onChange={event => event.target.checked ? enableDistanceAlerts() : disableDistanceAlerts()} />Usar minha localização para alertas por distância</label><p className="text-xs text-ink/60">Ao ativar, sua localização precisa será salva em sua conta para comparar com vagas que tenham coordenadas. Ela não aparece no perfil público e pode ser removida desativando esta opção.</p>{alerts.useDistance && <label className="block text-sm font-bold">Raio máximo<select value={alerts.radiusKm || 10} onChange={event => setAlerts({ ...alerts, radiusKm: Number(event.target.value) })} className="mt-2 w-full rounded-xl border border-ink/15 bg-white px-4 py-3 font-normal"><option value={5}>5 km</option><option value={10}>10 km</option><option value={25}>25 km</option><option value={50}>50 km</option></select></label>}</section><label className="block text-sm font-bold">Frequência<select value={alerts.frequency} onChange={event => setAlerts({ ...alerts, frequency: event.target.value as AlertPreferences['frequency'] })} className="mt-2 w-full rounded-xl border border-ink/15 bg-white px-4 py-3 font-normal"><option value="immediate">Assim que uma vaga compatível for publicada</option><option value="daily">Resumo diário</option><option value="weekly">Resumo semanal</option></select></label><p className="text-xs text-ink/55">O envio depende da configuração de e-mail do servidor. Cidade/localidade é uma correspondência textual; o raio usa coordenadas da vaga e só sua localização quando você ativar essa opção.</p><button onClick={() => api<AlertPreferences>('/profile/alerts', token, { method: 'PUT', body: JSON.stringify(alerts) }).then(setAlerts).then(() => setNotice('Preferências de alertas salvas.')).catch(cause => setError(cause.message))} className="rounded-full bg-ink px-5 py-3 font-bold text-white">Salvar preferências</button></div>
          </Card>
          <Card title="Compartilhar perfil" description="O link público mostra somente as informações que você selecionar. Nome é exibido; e-mail, telefone, currículo e foto permanecem privados.">
            <div className="space-y-4"><label className="flex items-start gap-3 rounded-2xl bg-paper p-4 font-semibold"><input className="mt-1" type="checkbox" checked={shareEnabled} onChange={event => setShareEnabled(event.target.checked)} /><span><span className="block">Permitir acesso ao meu perfil por link</span><span className="mt-1 block text-xs font-normal text-ink/60">Desative para bloquear o link público. Ao ativar, o nome será exibido junto aos itens autorizados abaixo.</span></span></label><fieldset disabled={!shareEnabled} className="space-y-3 disabled:opacity-55"><legend className="text-sm font-bold">Escolha quais informações adicionais aparecem no link:</legend><div className="grid gap-3 sm:grid-cols-2">{[[shareSummary,setShareSummary,'Apresentação','Seu resumo profissional e seus objetivos.'],[shareExperiences,setShareExperiences,'Experiências','Cargos, organizações, datas e descrições cadastradas.'],[shareEducation,setShareEducation,'Formações','Cursos e instituições que você registrou.'],[shareLocation,setShareLocation,'Cidade e estado','A localização informada no perfil.']].map(([checked,setter,label,description])=><label key={label as string} className="flex items-start gap-3 rounded-xl border border-ink/10 p-4 text-sm"><input className="mt-0.5" type="checkbox" checked={checked as boolean} onChange={event => (setter as (value:boolean)=>void)(event.target.checked)} /><span><span className="block font-bold">{label as string}</span><span className="mt-1 block text-xs font-normal text-ink/60">{description as string}</span></span></label>)}</div></fieldset><p className="rounded-xl border border-brand-green/20 bg-brand-green/5 p-3 text-xs font-semibold">Privado sempre: e-mail, telefone, currículo, foto, notas de candidaturas, alertas e localização precisa. Você pode alterar estas escolhas quando quiser.</p><button onClick={() => api<{ shareId: string }>('/profile/public', token, { method: 'PUT', body: JSON.stringify({ enabled: shareEnabled, showLocation: shareLocation, showSummary: shareSummary, showExperiences: shareExperiences, showEducation: shareEducation }) }).then(data => { setShareId(data.shareId); setNotice(shareEnabled ? 'Privacidade do perfil atualizada.' : 'O acesso público ao perfil foi desativado.'); }).catch(cause => setError(cause.message))} className="rounded-full bg-ink px-5 py-3 font-bold text-white">Salvar privacidade</button>{shareEnabled && shareId && <div className="break-all rounded-xl bg-paper p-4 text-sm"><p className="mb-2 font-bold">Link público do perfil</p><a href={`/?perfil=${shareId}`} target="_blank" rel="noreferrer" className="text-brand-blue">{`${window.location.origin}/?perfil=${shareId}`}</a><button onClick={() => navigator.clipboard.writeText(`${window.location.origin}/?perfil=${shareId}`)} className="ml-3 font-bold text-brand-red">Copiar</button></div>}</div>
          </Card>
          <Card title="Seus dados e sua conta" description="Você pode baixar uma cópia dos seus dados ou excluir sua conta e as informações vinculadas a ela.">
            <div className="space-y-6"><button type="button" onClick={exportAccountData} className="rounded-full border-2 border-ink px-5 py-3 font-bold">Baixar meus dados (JSON)</button><form onSubmit={deleteAccount} className="space-y-3 border-t border-ink/10 pt-5"><h3 className="font-extrabold text-brand-red">Excluir conta permanentemente</h3><p className="text-sm text-ink/65">Esta ação remove seu perfil, currículo, foto, diagnóstico, experiências, formações e candidaturas. Para confirmar, digite sua senha e a palavra EXCLUIR.</p><div className="grid gap-3 sm:grid-cols-2"><Field label="Senha atual" type="password" required value={deletePassword} onChange={event => setDeletePassword(event.target.value)} /><Field label="Digite EXCLUIR" required value={deleteConfirmation} onChange={event => setDeleteConfirmation(event.target.value)} /></div><button disabled={busy || deleteConfirmation !== 'EXCLUIR'} className="rounded-full bg-brand-red px-5 py-3 font-bold text-white disabled:opacity-50">Excluir minha conta</button></form></div>
          </Card>
          <div className="rounded-3xl bg-brand-yellow/25 p-6 text-center md:p-8"><h2 className="text-2xl font-black">Pronto para encontrar sua próxima oportunidade?</h2><p className="mx-auto mt-2 max-w-xl text-sm text-ink/65">Confirme seus dados e acesse cursos e vagas recomendados para você. O currículo é opcional e pode ser enviado depois.</p><button onClick={confirmProfile} disabled={busy} className="mt-5 rounded-full border-0 bg-ink px-7 py-4 font-extrabold text-white cursor-pointer disabled:cursor-not-allowed disabled:opacity-40">{busy ? 'Confirmando...' : 'Confirmar dados e ver oportunidades'}</button></div>
        </div>
      </main>
    </div>
  );
}
