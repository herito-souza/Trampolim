import { useEffect, useMemo, useState } from 'react';
import { getRecommendations, loadDiagnosticResult } from '../diagnostic/recommendations';
import type { CatalogItem, ProfileResponse } from './api';
import { api } from './api';
import { AccountNavigation } from './AccountNavigation';
import type { AccountDestination } from './AccountNavigation';
import { AccountHeaderActions } from './AccountHeaderActions';
import { RibbonStrip } from '../../components/ui/RibbonStrip';

type PremiumTab = 'overview' | 'resume' | 'interview' | 'guidance' | 'development';
type InterestType = 'PILOT' | 'GUIDANCE';
interface PremiumStatus { interviewAiAvailable: boolean; professionalSchedulingAvailable: boolean; paymentsEnabled: boolean }
const tabs: { id: PremiumTab; label: string }[] = [
  { id: 'overview', label: 'Plano' }, { id: 'resume', label: 'Currículo' }, { id: 'interview', label: 'Entrevista' }, { id: 'guidance', label: 'Orientação' }, { id: 'development', label: 'Meu plano' },
];
const interviewQuestions: Record<string, string[]> = {
  Geral: ['Conte um pouco sobre você e sua trajetória.', 'O que chamou sua atenção nesta oportunidade?', 'Conte sobre uma situação em que precisou resolver um problema.'],
  Tecnologia: ['Como você explicaria uma solução técnica para alguém sem experiência na área?', 'Conte como investigaria um erro que não consegue reproduzir de imediato.', 'Como você aprende uma ferramenta ou linguagem nova?'],
  Saúde: ['Como você acolheria uma pessoa que chega preocupada ou com dúvidas?', 'Como organiza informações com atenção à privacidade?', 'Conte como trabalharia em equipe em uma rotina movimentada.'],
  Marketing: ['Como adaptaria uma mensagem para públicos diferentes?', 'O que analisaria para entender se uma campanha funcionou?', 'Como transformaria uma ideia em um calendário de conteúdo?'],
  Administração: ['Como você organiza várias tarefas com prazos próximos?', 'Conte sobre uma situação em que percebeu e corrigiu um erro em informações.', 'Como priorizaria uma solicitação urgente?'],
};
const card = 'rounded-3xl border border-white/10 bg-[#121c2b] p-6 md:p-8';
const button = 'rounded-full border border-white/25 px-5 py-3 text-sm font-extrabold transition-colors hover:border-brand-yellow/60';
const skillNames: Record<string, string> = { service: 'atendimento', sales: 'vendas', admin: 'organização', numbers: 'análise de dados', logistics: 'logística', maintenance: 'manutenção', production: 'produção', technology: 'tecnologia', health: 'saúde', marketing: 'marketing' };

export function PremiumPage({ token, profile, onLogout, onNavigate }: { token: string; profile: ProfileResponse; onLogout: () => void; onNavigate: (destination: AccountDestination) => void }) {
  const [tab, setTab] = useState<PremiumTab>('overview');
  const [role, setRole] = useState('');
  const [selectedJobId, setSelectedJobId] = useState('');
  const [publishedJobs, setPublishedJobs] = useState<CatalogItem[]>([]);
  const [resume, setResume] = useState('');
  const [notice, setNotice] = useState('');
  const [noticeError, setNoticeError] = useState(false);
  const [interests, setInterests] = useState<InterestType[]>([]);
  const [interestBusy, setInterestBusy] = useState<InterestType | null>(null);
  const [premiumStatus, setPremiumStatus] = useState<PremiumStatus>({ interviewAiAvailable: false, professionalSchedulingAvailable: false, paymentsEnabled: false });
  const [interviewArea, setInterviewArea] = useState('Geral');
  const [questionIndex, setQuestionIndex] = useState(0);
  const [answer, setAnswer] = useState('');
  const [feedback, setFeedback] = useState('');
  const [feedbackBusy, setFeedbackBusy] = useState(false);
  const [feedbackSource, setFeedbackSource] = useState<'AI' | 'LOCAL' | null>(null);
  const diagnostic = loadDiagnosticResult();
  const interest = interests.includes('PILOT');
  const guidanceInterest = interests.includes('GUIDANCE');
  const selectedJob = publishedJobs.find(job => String(job.id) === selectedJobId);
  const targetRole = role.trim() || selectedJob?.title || 'Oportunidade a definir';
  const resumeText = useMemo(() => {
    const person = profile.profile;
    const experienceText = profile.experiences.map(item => `${item.position} — ${item.company}${item.description ? `\n${item.description}` : ''}`).join('\n');
    const educationText = profile.educations.map(item => `${item.course} — ${item.institution}`).join('\n');
    return [person.name, [person.email, person.phone, person.location].filter(Boolean).join(' · '), '', `OBJETIVO PROFISSIONAL — ${targetRole}`, person.summary || 'Resumo profissional não informado.', '', 'EXPERIÊNCIAS', experienceText || 'Nenhuma experiência cadastrada no perfil.', '', 'FORMAÇÃO', educationText || 'Nenhuma formação cadastrada no perfil.'].join('\n');
  }, [profile, targetRole]);
  const questions = interviewQuestions[interviewArea];
  const roadmapSteps = useMemo(() => {
    const completion = [
      { id: 'profile-summary', title: 'Revisar seu resumo profissional', detail: profile.profile.summary ? 'Seu resumo já está preenchido; revise se ainda representa seu objetivo.' : 'Inclua um resumo curto no perfil para apresentar seu foco profissional.' },
      { id: 'experience', title: 'Organizar experiências e formação', detail: profile.experiences.length && profile.educations.length ? `${profile.experiences.length} experiência(s) e ${profile.educations.length} formação(ões) no perfil.` : 'Complete suas experiências e formações para fortalecer o currículo.' },
    ];
    const topSkills = diagnostic ? (Object.entries(diagnostic.scores) as [string, number][]).sort((a, b) => b[1] - a[1]).filter(([, score]) => score > 0).slice(0, 2).map(([skill]) => skillNames[skill] || skill) : [];
    const course = getRecommendations(diagnostic, 'course')[0];
    const job = getRecommendations(diagnostic, 'job')[0];
    return [
      ...completion,
      { id: 'learning', title: course ? `Explorar: ${course.title}` : 'Escolher um curso para seu próximo passo', detail: topSkills.length ? `Relacionada a ${topSkills.join(' e ')}. Confira requisitos e disponibilidade na plataforma.` : 'Faça o diagnóstico para receber uma sugestão alinhada aos seus interesses.' },
      { id: 'interview', title: 'Praticar uma resposta de entrevista', detail: 'Use uma situação real e organize a resposta em contexto, ação e resultado.' },
      { id: 'opportunity', title: job ? `Pesquisar: ${job.title}` : 'Definir uma oportunidade para acompanhar', detail: 'Consulte vagas abertas e escolha uma oportunidade compatível com seu momento.' },
    ];
  }, [profile, diagnostic]);
  const roadmapKey = `trampolim_premium_roadmap:${profile.profile.email}`;
  const [roadmapDone, setRoadmapDone] = useState<Record<string, boolean>>(() => {
    try { return JSON.parse(localStorage.getItem(roadmapKey) || '{}') as Record<string, boolean>; } catch { return {}; }
  });

  useEffect(() => {
    void api<PremiumStatus>('/premium/status', token).then(setPremiumStatus).catch(() => undefined);
    void api<{ type: InterestType }[]>('/premium/interests', token).then(saved => setInterests(saved.map(item => item.type))).catch(() => undefined);
    void api<CatalogItem[]>('/opportunities/job', token).then(setPublishedJobs).catch(() => undefined);
  }, [token]);

  function setMessage(message: string, error = false) { setNotice(message); setNoticeError(error); }
  function createResume() {
    setResume(resumeText);
    setMessage(selectedJob ? `Prévia direcionada para “${selectedJob.title}”. Confira se cada informação representa sua experiência.` : 'Prévia criada com as informações atuais do seu perfil. Confira os dados antes de compartilhar.');
  }
  async function copyResume() {
    try { await navigator.clipboard.writeText(resume || resumeText); setMessage('Texto do currículo copiado.'); }
    catch { setMessage('Não foi possível copiar automaticamente. Selecione o texto da prévia e copie manualmente.', true); }
  }
  function printResume() {
    if (!resume) return;
    const originalTitle = document.title;
    document.title = `Curriculo-${profile.profile.name.trim().replace(/\s+/g, '-')}`;
    window.print();
    window.setTimeout(() => { document.title = originalTitle; }, 500);
  }
  async function registerInterest(type: InterestType) {
    setInterestBusy(type); setNotice('');
    try {
      await api('/premium/interests', token, { method: 'POST', body: JSON.stringify({ type, details: type === 'GUIDANCE' ? 'Interesse em orientação profissional e disponibilidade de sessões com profissionais parceiros.' : 'Interesse em participar do piloto Trampolim Premium.' }) });
      setInterests(current => [...new Set([...current, type])]);
      setMessage(type === 'GUIDANCE' ? 'Interesse em orientação registrado. Isto não é um agendamento; entraremos em contato quando houver profissionais parceiros disponíveis.' : 'Interesse no piloto registrado. Nenhuma contratação ou cobrança foi realizada.');
    } catch (cause) { setMessage(cause instanceof Error ? cause.message : 'Não foi possível registrar seu interesse. Tente novamente.', true); }
    finally { setInterestBusy(null); }
  }
  function localFeedback() {
    const words = answer.trim().split(/\s+/).filter(Boolean).length;
    if (words < 20) setFeedback('Sua resposta ainda está curta. Tente explicar o contexto, a ação que você tomou e o resultado.');
    else if (!/eu |minha |meu |nós |equipe/i.test(answer)) setFeedback('A resposta tem contexto. Inclua também qual foi a sua contribuição e o que aprendeu com a situação.');
    else setFeedback('Você trouxe uma experiência pessoal. Para deixá-la mais clara, destaque a situação, sua ação e o resultado.');
    setFeedbackSource('LOCAL');
  }
  async function reviewAnswer() {
    if (!premiumStatus.interviewAiAvailable) { localFeedback(); return; }
    setFeedbackBusy(true); setFeedback(''); setNotice('');
    try {
      const result = await api<{ feedback: string }>('/premium/interview-feedback', token, { method: 'POST', body: JSON.stringify({ area: interviewArea, question: questions[questionIndex], answer, clientKey: crypto.randomUUID(), consent: true }) });
      setFeedback(result.feedback); setFeedbackSource('AI');
    } catch (cause) { setMessage(cause instanceof Error ? cause.message : 'Não foi possível obter o feedback da IA. Tente novamente.', true); }
    finally { setFeedbackBusy(false); }
  }
  function toggleRoadmapStep(id: string) {
    setRoadmapDone(current => {
      const next = { ...current, [id]: !current[id] };
      localStorage.setItem(roadmapKey, JSON.stringify(next));
      return next;
    });
  }

  return <div className="min-h-screen bg-[#0b1421] text-white">
    <style>{`@media print { @page { margin: 18mm; } body { background: white !important; } body * { visibility: hidden !important; } #premium-resume-preview, #premium-resume-preview * { visibility: visible !important; } #premium-resume-preview { position: absolute !important; inset: 0 auto auto 0 !important; width: 100% !important; max-height: none !important; overflow: visible !important; border: 0 !important; border-radius: 0 !important; background: white !important; color: #111827 !important; box-shadow: none !important; font-size: 11pt !important; } }`}</style>
    <RibbonStrip />
    <header className="flex flex-wrap items-center justify-between gap-3 border-b border-white/10 px-[6vw] py-5"><a href="#premium" className="font-serif text-2xl font-black text-white no-underline">Trampol<span className="text-brand-yellow">i</span>m</a><AccountHeaderActions onProfile={() => onNavigate('profile')} onLogout={onLogout} /></header>
    <AccountNavigation active="premium" onNavigate={onNavigate} />
    <main id="premium" className="mx-auto max-w-7xl px-[6vw] pb-20">
      <section className="relative mt-8 overflow-hidden rounded-[30px] bg-ink px-7 py-10 text-brand-white md:mt-12 md:px-12 md:py-14">
        <div className="absolute -right-8 -top-16 h-64 w-64 rounded-full border border-brand-yellow/20 shadow-[0_0_100px_rgba(214,255,56,0.08)]" aria-hidden="true" />
        <div className="relative max-w-2xl"><span className="inline-block rounded-full bg-brand-yellow px-4 py-2 text-xs font-extrabold uppercase tracking-wider text-ink">Trampolim Premium · em validação</span><h1 className="mt-5 text-4xl font-black leading-tight md:text-5xl">Mais preparo para o seu próximo passo.</h1><p className="mt-4 max-w-xl text-white/75">Ferramentas para apresentar sua trajetória, praticar entrevistas e planejar seu desenvolvimento profissional.</p><div className="mt-7 flex flex-wrap gap-3"><button type="button" onClick={() => void registerInterest('PILOT')} disabled={interestBusy !== null} className="rounded-full bg-brand-yellow px-6 py-3 font-extrabold text-[#101725] disabled:opacity-60">{interest ? 'Interesse registrado' : interestBusy === 'PILOT' ? 'Registrando…' : 'Quero participar do piloto'}</button><button type="button" onClick={() => setTab('resume')} className={button}>Experimentar currículo</button></div><p className="mt-4 text-xs text-white/50">Plano e preço em validação. Não há cobrança nesta demonstração.</p></div>
      </section>

      <nav aria-label="Recursos Premium" className="mobile-nav-scroll mt-7 flex gap-3 overflow-x-auto pb-3">{tabs.map(item => <button key={item.id} type="button" aria-current={tab === item.id ? 'page' : undefined} onClick={() => { setTab(item.id); setNotice(''); }} className={`min-h-14 shrink-0 rounded-full border px-7 py-3 text-base font-bold transition-colors ${tab === item.id ? 'border-brand-yellow bg-brand-yellow text-[#101725]' : 'border-white/20 text-white hover:border-white/50'}`}>{item.label}</button>)}</nav>
      {notice && <p role={noticeError ? 'alert' : 'status'} className={`mt-5 rounded-xl border p-4 text-sm font-semibold ${noticeError ? 'border-red-400/30 bg-red-400/10 text-red-200' : 'border-brand-yellow/25 bg-brand-yellow/10 text-brand-yellow'}`}>{notice}</p>}

      {tab === 'overview' && <>
        <section className="mt-8"><p className="text-xs font-extrabold uppercase tracking-[0.18em] text-brand-yellow">Recursos do Premium</p><h2 className="mt-2 text-3xl font-black">Um apoio completo para sua jornada</h2><div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {[['01', 'Currículo personalizado', 'Monte uma versão direcionada a uma vaga, revise os dados e salve como PDF.'], ['02', 'Simulação de entrevistas', premiumStatus.interviewAiAvailable ? 'Receba feedback de IA sobre clareza, exemplos e pontos a desenvolver.' : 'Pratique com perguntas e sugestões locais; o feedback por IA será ativado após configurar o serviço.'], ['03', 'Orientação profissional', 'Registre interesse em sessões com profissionais parceiros; horários ainda não estão disponíveis.'], ['04', 'Plano de desenvolvimento', 'Acompanhe metas de perfil, aprendizado, entrevistas e busca de oportunidades.'], ['05', 'Perfil profissional revisado', 'Organize resumo, experiências e apresentação para os processos seletivos.'], ['06', 'Prática contínua', 'Marque as etapas concluídas e retome seu plano quando quiser.']].map(([num, title, description]) => <article key={num} className={card}><span className="font-serif text-3xl font-black text-brand-yellow">{num}</span><h3 className="mt-5 text-xl font-black">{title}</h3><p className="mt-3 text-sm leading-6 text-white/65">{description}</p></article>)}
        </div></section>
        <section className={`${card} mt-6 border-brand-yellow/20`}><div className="flex flex-wrap items-center justify-between gap-4"><div><p className="text-xs font-extrabold uppercase tracking-widest text-brand-yellow">Acesso antecipado</p><h2 className="mt-2 text-2xl font-black">Ajude a definir o Premium</h2><p className="mt-2 text-sm text-white/65">Seu interesse fica associado à sua conta para ajudar a planejar o piloto. Sem compromisso e sem cobrança.</p></div><button type="button" onClick={() => void registerInterest('PILOT')} disabled={interest || interestBusy !== null} className="rounded-full bg-brand-yellow px-5 py-3 font-extrabold text-[#101725] disabled:opacity-60">{interest ? 'Interesse registrado' : 'Registrar interesse'}</button></div></section>
      </>}

      {tab === 'resume' && <section className={`${card} mt-8`}><p className="text-xs font-extrabold uppercase tracking-widest text-brand-yellow">Construtor de currículo</p><h2 className="mt-2 text-3xl font-black">Adapte seu currículo à oportunidade</h2><p className="mt-3 max-w-3xl text-sm leading-6 text-white/65">Escolha uma vaga publicada ou informe um cargo. A prévia usa somente fatos do seu perfil; ela não inventa experiências ou qualificações. Revise os dados antes de compartilhar.</p><label className="mt-6 block text-sm font-bold">Vaga publicada (opcional)<select value={selectedJobId} onChange={event => { setSelectedJobId(event.target.value); setResume(''); }} className="mt-2 w-full rounded-xl border border-white/15 bg-[#0b1421] px-4 py-3 text-white"><option value="">Escolher uma vaga aberta</option>{publishedJobs.map(job => <option key={job.id} value={job.id}>{job.title} · {job.provider}</option>)}</select></label><label className="mt-4 block text-sm font-bold">Cargo ou área desejada<input value={role} maxLength={120} onChange={event => { setRole(event.target.value); setResume(''); }} placeholder="Ex.: Assistente de marketing" className="mt-2 w-full rounded-xl border border-white/15 bg-[#0b1421] px-4 py-3 text-white placeholder:text-white/40" /></label><div className="mt-5 flex flex-wrap gap-3"><button type="button" onClick={createResume} className="rounded-full bg-brand-yellow px-5 py-3 font-extrabold text-[#101725]">Gerar prévia</button>{resume && <><button type="button" onClick={() => void copyResume()} className={button}>Copiar texto</button><button type="button" onClick={printResume} className={button}>Salvar como PDF</button></>}</div>{resume && <article id="premium-resume-preview" className="mt-6 max-h-[620px] overflow-auto rounded-2xl border border-white/10 bg-white p-7 font-sans text-[#101725] shadow-xl md:p-10"><header className="border-b-2 border-brand-yellow pb-5"><h3 className="text-3xl font-black">{profile.profile.name}</h3><p className="mt-2 text-sm text-[#475569]">{[profile.profile.email, profile.profile.phone, profile.profile.location].filter(Boolean).join(' · ')}</p></header><section className="mt-6"><h4 className="text-xs font-extrabold uppercase tracking-widest text-[#a85231]">Objetivo profissional</h4><p className="mt-2 font-semibold">{targetRole}</p></section><section className="mt-6"><h4 className="text-xs font-extrabold uppercase tracking-widest text-[#a85231]">Perfil profissional</h4><p className="mt-2 whitespace-pre-wrap text-sm leading-6">{profile.profile.summary || 'Resumo profissional não informado.'}</p></section><section className="mt-6"><h4 className="text-xs font-extrabold uppercase tracking-widest text-[#a85231]">Experiências</h4>{profile.experiences.length ? <ul className="mt-3 space-y-4">{profile.experiences.map(item => <li key={item.id}><p className="font-bold">{item.position} · {item.company}</p><p className="mt-1 text-xs text-[#64748b]">{[item.startDate, item.current ? 'Atual' : item.endDate].filter(Boolean).join(' — ')}</p>{item.description && <p className="mt-1 whitespace-pre-wrap text-sm leading-6">{item.description}</p>}</li>)}</ul> : <p className="mt-2 text-sm">Nenhuma experiência cadastrada no perfil.</p>}</section><section className="mt-6"><h4 className="text-xs font-extrabold uppercase tracking-widest text-[#a85231]">Formação</h4>{profile.educations.length ? <ul className="mt-3 space-y-3">{profile.educations.map(item => <li key={item.id}><p className="font-bold">{item.course} · {item.institution}</p><p className="mt-1 text-xs text-[#64748b]">{[item.level, item.inProgress ? 'Em andamento' : item.endDate].filter(Boolean).join(' · ')}</p></li>)}</ul> : <p className="mt-2 text-sm">Nenhuma formação cadastrada no perfil.</p>}</section></article>}</section>}

      {tab === 'interview' && <section className={`${card} mt-8`}><p className="text-xs font-extrabold uppercase tracking-widest text-brand-yellow">Treino de entrevista</p><h2 className="mt-2 text-3xl font-black">Pratique com uma pergunta por vez</h2><p className="mt-3 max-w-3xl text-sm leading-6 text-white/65">Escolha uma área e responda. {premiumStatus.interviewAiAvailable ? 'O feedback será gerado por IA e registrado para permitir retomada e controle de uso.' : 'A demonstração usa dicas locais. Para ativar o feedback por IA, configure OPENAI_API_KEY no backend.'}</p>{premiumStatus.interviewAiAvailable && <p className="mt-3 rounded-xl border border-brand-yellow/20 bg-brand-yellow/5 p-3 text-xs leading-5 text-white/65">Ao solicitar feedback, a pergunta e a resposta serão enviadas ao serviço de IA para processamento. Não inclua dados pessoais, informações de saúde ou segredos de terceiros.</p>}<label className="mt-6 block max-w-sm text-sm font-bold">Área da entrevista<select value={interviewArea} onChange={event => { setInterviewArea(event.target.value); setQuestionIndex(0); setAnswer(''); setFeedback(''); }} className="mt-2 w-full rounded-xl border border-white/15 bg-[#0b1421] px-4 py-3 text-white">{Object.keys(interviewQuestions).map(area => <option key={area}>{area}</option>)}</select></label><article className="mt-6 rounded-2xl border border-brand-yellow/20 bg-[#0b1421] p-5"><p className="text-xs font-bold uppercase tracking-wider text-brand-yellow">Pergunta {questionIndex + 1} de {questions.length}</p><h3 className="mt-3 text-xl font-bold">{questions[questionIndex]}</h3><label className="mt-5 block text-sm font-bold">Sua resposta<textarea value={answer} onChange={event => { setAnswer(event.target.value); setFeedback(''); }} rows={6} maxLength={2500} placeholder="Escreva como responderia na entrevista…" className="mt-2 w-full rounded-xl border border-white/15 bg-[#121c2b] px-4 py-3 font-normal text-white placeholder:text-white/40" /></label><div className="mt-4 flex flex-wrap gap-3"><button type="button" disabled={!answer.trim() || feedbackBusy} onClick={() => void reviewAnswer()} className="rounded-full bg-brand-yellow px-5 py-3 font-extrabold text-[#101725] disabled:opacity-50">{feedbackBusy ? 'Analisando…' : premiumStatus.interviewAiAvailable ? 'Autorizar envio e receber feedback da IA' : 'Ver sugestão'}</button><button type="button" onClick={() => { setQuestionIndex((questionIndex + 1) % questions.length); setAnswer(''); setFeedback(''); setFeedbackSource(null); }} className={button}>Próxima pergunta</button></div>{feedback && <div role="status" className="mt-4 rounded-xl bg-brand-yellow/10 p-4 text-sm leading-6 text-brand-yellow"><p className="mb-2 text-xs font-extrabold uppercase tracking-wider">{feedbackSource === 'AI' ? 'Feedback da IA' : 'Sugestão de estrutura'}</p>{feedback}</div>}</article></section>}

      {tab === 'guidance' && <section className={`${card} mt-8`}><p className="text-xs font-extrabold uppercase tracking-widest text-brand-yellow">Orientação profissional</p><h2 className="mt-2 text-3xl font-black">Apoio com profissionais parceiros</h2><p className="mt-3 max-w-3xl text-sm leading-6 text-white/65">A proposta é oferecer sessões com psicólogos(as) habilitados(as) e/ou profissionais de carreira, com escopo, duração e disponibilidade apresentados antes de qualquer contratação.</p><div className="mt-6 grid gap-4 md:grid-cols-2"><article className="rounded-2xl border border-white/10 bg-[#0b1421] p-5"><h3 className="text-lg font-black">Orientação de carreira</h3><p className="mt-2 text-sm leading-6 text-white/65">Conversa sobre objetivos, pontos fortes, transições e planejamento de próximos passos.</p></article><article className="rounded-2xl border border-white/10 bg-[#0b1421] p-5"><h3 className="text-lg font-black">Apoio psicológico</h3><p className="mt-2 text-sm leading-6 text-white/65">Somente com profissional habilitado, agenda confirmada e condições do atendimento claramente informadas.</p></article></div><div className="mt-6 rounded-2xl border border-brand-yellow/20 bg-brand-yellow/5 p-5"><p className="text-sm leading-6 text-white/75">{premiumStatus.professionalSchedulingAvailable ? 'A agenda de profissionais parceiros está disponível.' : 'Ainda não há agenda de profissionais conectada. Seu interesse será salvo para avaliarmos a demanda; isso não cria um agendamento.'} Não informe dados de saúde ou outros dados sensíveis neste registro.</p><button type="button" onClick={() => void registerInterest('GUIDANCE')} disabled={guidanceInterest || interestBusy !== null} className="mt-4 rounded-full bg-brand-yellow px-5 py-3 font-extrabold text-[#101725] disabled:opacity-60">{guidanceInterest ? 'Interesse registrado' : interestBusy === 'GUIDANCE' ? 'Registrando…' : 'Quero acompanhar a disponibilidade'}</button></div></section>}

      {tab === 'development' && <section className={`${card} mt-8`}><p className="text-xs font-extrabold uppercase tracking-widest text-brand-yellow">Plano de desenvolvimento</p><h2 className="mt-2 text-3xl font-black">Pequenos passos, no seu ritmo</h2><p className="mt-3 max-w-3xl text-sm leading-6 text-white/65">Uma lista de ações sugeridas a partir do seu perfil e diagnóstico. Seu progresso fica salvo neste dispositivo, e você pode marcar cada etapa como concluída.</p>{diagnostic && <p className="mt-4 rounded-xl border border-brand-yellow/20 bg-brand-yellow/5 p-4 text-sm text-brand-yellow">Trilha do diagnóstico: {diagnostic.trackName}</p>}{!diagnostic && <button type="button" onClick={() => onNavigate('diagnostic')} className="mt-4 rounded-full border border-brand-yellow/30 px-4 py-2 text-sm font-bold text-brand-yellow">Fazer diagnóstico para personalizar</button>}<ol className="mt-6 space-y-3">{roadmapSteps.map((step, index) => <li key={step.id} className="flex gap-4 rounded-2xl border border-white/10 bg-[#0b1421] p-4"><span className="font-serif text-2xl font-black text-brand-yellow">{String(index + 1).padStart(2, '0')}</span><div className="min-w-0 flex-1"><h3 className="font-bold">{step.title}</h3><p className="mt-1 text-sm leading-6 text-white/60">{step.detail}</p></div><label className="flex shrink-0 items-center gap-2 text-xs font-bold text-white/70"><input type="checkbox" checked={Boolean(roadmapDone[step.id])} onChange={() => toggleRoadmapStep(step.id)} className="h-5 w-5 accent-[#d6ff38]" />Feito</label></li>)}</ol><p className="mt-4 text-xs text-white/45">Este plano é uma ferramenta de organização e não uma avaliação de capacidade ou garantia de contratação.</p></section>}
    </main>
  </div>;
}
