import type { Notice } from './QuestionNotifications';
import { LearningOverview } from './LearningOverview';
import { useLearningOverview } from './useLearningOverview';
import { DashboardSkeleton } from '../dashboard/DashboardSkeleton';
import {WorkspaceNavigation} from '../../components/workspace/WorkspaceNavigation';
import {NotificationSlot} from '../../components/workspace/WorkspaceSlots';
import { AsyncPanel } from '../../components/ui/Feedback';
import { DashboardActions } from '../dashboard/DashboardActions';
const ObservatoryPanel=lazy(()=>import('../career/ObservatoryPanel').then(module=>({default:module.ObservatoryPanel})));
const YoungOpportunities=lazy(()=>import('../career/YoungOpportunities').then(module=>({default:module.YoungOpportunities})));
const AdaptivePanel=lazy(()=>import('./AdaptivePanel').then(module=>({default:module.AdaptivePanel})));
const PassportPanel=lazy(()=>import('./PassportPanel').then(module=>({default:module.PassportPanel})));
const AdaptiveModulePolicy=lazy(()=>import('./AdaptiveModulePolicy').then(module=>({default:module.AdaptiveModulePolicy})));
const HubParticipationPanel=lazy(()=>import('../hub/HubParticipationPanel').then(module=>({default:module.HubParticipationPanel})));
import { CapacitaModuleNav } from './CapacitaModuleNav';
import { lazy, useEffect, useRef, useState } from 'react';
import { LearningPanelBoundary } from './LearningPanelBoundary';
import { api } from '../account/api';
const LessonPlayer=lazy(()=>import('./LessonPlayer').then(module=>({default:module.LessonPlayer})));
const CourseForm=lazy(()=>import('./CourseManagement').then(module=>({default:module.CourseForm})));
const SectionForm=lazy(()=>import('./CourseManagement').then(module=>({default:module.SectionForm})));
const LearningAdmin=lazy(()=>import('./CourseManagement').then(module=>({default:module.LearningAdmin})));
const LessonEditor = lazy(() => import('./LessonEditor').then(module => ({ default: module.LessonEditor })));
import { QuestionNotifications } from './QuestionNotifications';
const QuestionBoard = lazy(() => import('./QuestionBoard').then(module => ({ default: module.QuestionBoard })));
const LibraryPanel = lazy(() => import('./LibraryPanel').then(module => ({ default: module.LibraryPanel })));
const AssessmentPanel = lazy(() => import('./AssessmentPanel').then(module => ({ default: module.AssessmentPanel })));
const PerformancePanel = lazy(() => import('./PerformancePanel').then(module => ({ default: module.PerformancePanel })));
const InclusionSupport = lazy(() => import('./InclusionSupport').then(module => ({ default: module.InclusionSupport })));
import type { LibraryScope } from './libraryTypes';
import type { QuestionScope } from './QuestionBoard';
import type { Course, Lesson, Module, Subject, Progress } from './types';
import { buttonClass, inputClass, statusLabel } from './types';
type Account = { id: number; name: string; email: string; teacher: boolean; admin: boolean };
const root = '/capacita/learning';

export function LearningPortal({ token,navigationActive=true,onActivate,onName }: { token: string;navigationActive?:boolean;onActivate?:()=>void;onName?:(name:string)=>void }) {
  const [notices,setNotices]=useState<{unread:number;items:Notice[]}|null>(null);
  const [activeModule, setActiveModule] = useState('courses');
  const nameCallback=useRef(onName);nameCallback.current=onName;
  function selectModule(id: string,write=true) {
    onActivate?.();
    if(write){const hash='#capacita/learning/'+id+(manage?'?mode=teacher':'');if(window.location.hash!==hash)window.history.pushState(null,'',window.location.pathname+window.location.search+hash)}
    setActiveModule(id); setQuestionScope(null); setLibraryScope(null); setAssessmentScope(null); setPerformance(false); setSupport(false);
    if (id === 'library') setLibraryScope({});
    if (id === 'assessments') setAssessmentScope({});
    if (id === 'performance') setPerformance(true);
    if (id === 'questions') setQuestionScope({ inbox: false });
    if (id === 'inbox') setQuestionScope({ inbox: true });
    if (id === 'support') setSupport(true);
  }
  function chooseLearningMode(teacher:boolean){selectModule('courses',false);setManage(teacher);setPage(0);closeCourse();const hash='#capacita/learning/courses'+(teacher?'?mode=teacher':'');if(window.location.hash!==hash)window.history.pushState(null,'',window.location.pathname+window.location.search+hash)}
  const courseArea = useRef<HTMLDivElement>(null);
  useEffect(() => { if (activeModule !== 'courses') courseArea.current?.querySelectorAll('video, audio').forEach(media => (media as HTMLMediaElement).pause()); }, [activeModule]);
  const courseTrigger = useRef<HTMLElement | null>(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [hasMore, setHasMore] = useState(false);
  useEffect(() => { if (search === query) return; const timer = setTimeout(() => { setQuery(search); setPage(0); }, 300); return () => clearTimeout(timer); }, [search, query]);
  const [account, setAccount] = useState<Account | null>(null);
  const [courses, setCourses] = useState<Course[]>([]);
  const [manage, setManage] = useState(false);
  const [course, setCourse] = useState<Course | null>(null);
  const [moduleId, setModuleId] = useState<number | null>(null);
  const [lesson, setLesson] = useState<Lesson | null>(null);
  const [editor, setEditor] = useState<{ moduleId: number; lessonId?: number } | null>(null);
  const [editingCourse, setEditingCourse] = useState(false);
  const [sectionEditor, setSectionEditor] = useState<{ type: 'subject' | 'module'; parent: number; item?: Subject | Module } | null>(null);
  const [historyLoading,setHistoryLoading]=useState(true),[historyError,setHistoryError]=useState('');
  const [history, setHistory] = useState<Progress[]>([]);
  const [students, setStudents] = useState<{ id: number; full_name: string; started_lessons: number; completed_lessons: number }[] | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [revision, setRevision] = useState(0);
  const [questionScope, setQuestionScope] = useState<QuestionScope | null>(null);
  const [libraryScope, setLibraryScope] = useState<LibraryScope | null>(null);
  const [assessmentScope, setAssessmentScope] = useState<{ course?: number; module?: number } | null>(null);
  const [performance, setPerformance] = useState(false);
  const [support, setSupport] = useState(false);
  useEffect(() => {
    let active = true;
    const abort = new AbortController();
    api<Account>(`${root}/me`, token, { signal: abort.signal }).then(user => { if (active) {setAccount(user);if(user.name)nameCallback.current?.(user.name)} }).catch(failure => { if (active) setError(failure.message); });
    return () => { active = false; abort.abort(); };
  }, [token, revision]);
  const routeSelect=useRef(selectModule);routeSelect.current=selectModule;
  useEffect(()=>{if(!account)return;const sync=()=>{const parts=window.location.hash.split('/');if(parts[0]!=='#capacita'||parts[1]!=='learning')return;const id=parts[2]?.split('?')[0]||'courses';const allowed=['courses','adaptive','passport','library','assessments','performance','observatory','young','hub','questions','support',...(account.teacher?['inbox']:[])];setManage(account.teacher&&new URLSearchParams(window.location.hash.split('?')[1]||'').get('mode')==='teacher');routeSelect.current(allowed.includes(id)?id:'courses',false)};sync();window.addEventListener('hashchange',sync);window.addEventListener('popstate',sync);return()=>{window.removeEventListener('hashchange',sync);window.removeEventListener('popstate',sync)}},[account]);
  useEffect(() => {
    let active = true; const abort = new AbortController(); setLoading(true); setError('');
    api<Course[]>(`${root}/courses?manage=${manage}&limit=25&offset=${page * 24}&q=${encodeURIComponent(query)}`, token, { signal: abort.signal })
      .then(rows => { if (active) { setHasMore(Array.isArray(rows) && rows.length > 24); setCourses(Array.isArray(rows) ? rows.slice(0, 24) : []); } })
      .catch(failure => { if (active) setError(failure.message); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; abort.abort(); };
  }, [token, manage, revision, page, query]);
  useEffect(()=>{
    const abort=new AbortController();setHistory([]);setHistoryLoading(true);setHistoryError('');
    api<Progress[]>(`${root}/progress`,token,{signal:abort.signal}).then(rows=>{if(!abort.signal.aborted){if(!Array.isArray(rows))throw new Error('Histórico indisponível.');setHistory(rows)}}).catch(()=>{if(!abort.signal.aborted)setHistoryError('Não foi possível carregar o histórico. Você pode continuar explorando os cursos.')}).finally(()=>{if(!abort.signal.aborted)setHistoryLoading(false)});
    return()=>abort.abort();
  },[token,revision]);
  const overview=useLearningOverview(token,Boolean(account)&&!manage&&activeModule==='courses'&&!course&&!editingCourse,revision);
  async function openCourse(id: number) {
    courseTrigger.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    setError(''); setStudents(null); setLesson(null); setEditor(null); setModuleId(null);
    try { setCourse(await api<Course>(`${root}/courses/${id}?manage=${manage}`, token));return true; } catch (failure) { setError(failure instanceof Error ? failure.message : 'Curso indisponível.');return false; }
  }
  const accountId = account?.id;
  useEffect(() => {
    if (!accountId) return;
    const raw = sessionStorage.getItem('trampolim_capacita_course_intent'); if (!raw) return;
    sessionStorage.removeItem('trampolim_capacita_course_intent'); const abort = new AbortController();
    try { const intent = JSON.parse(raw) as { courseId: number; moduleId: number }; if (!Number.isSafeInteger(intent.courseId) || intent.courseId < 1 || !Number.isSafeInteger(intent.moduleId) || intent.moduleId < 1) return;
      api<Course>(`${root}/courses/${intent.courseId}?manage=false`, token, { signal: abort.signal }).then(found => { setManage(false); setActiveModule('courses'); setCourse(found); if (found.subjects?.some(subject => subject.modules.some(module => module.id === intent.moduleId))) setModuleId(intent.moduleId); }).catch(failure => { if (!abort.signal.aborted) setError(failure.message); });
    } catch { setError('O direcionamento do curso é inválido. Consulte o catálogo.'); }
    return () => abort.abort();
  }, [accountId, token]);
  useEffect(() => { if (accountId && sessionStorage.getItem('trampolim_hub_program_intent')) setActiveModule('hub'); }, [accountId]);
  const focusCourseId = course?.id;
  useEffect(() => { if (!focusCourseId) return; const frame = requestAnimationFrame(() => { document.querySelector<HTMLElement>(moduleId ? "[data-module-heading]" : "[data-course-heading]")?.focus({ preventScroll: true }); }); return () => cancelAnimationFrame(frame); }, [focusCourseId, moduleId]);
  function closeCourse() { requestAnimationFrame(() => { if (courseTrigger.current?.isConnected) courseTrigger.current.focus({ preventScroll: true }); }); setCourse(null); setModuleId(null); setLesson(null); setEditor(null); setEditingCourse(false); setSectionEditor(null); setStudents(null); setRevision(value => value + 1); }
  async function refreshCourse() { if (course) await openCourse(course.id); setRevision(value => value + 1); }
  const modules = course?.subjects?.flatMap(subject => subject.modules || []) || [];
  const selectedModule = modules.find(item => item.id === moduleId);
  const lessons = modules.flatMap(item => item.lessons || []);
  const next = lesson ? lessons[lessons.findIndex(item => item.id === lesson.id) + 1] : undefined;
  return <section id="videoaulas" className="mb-14 space-y-6 rounded-[28px] border border-brand-blue/20 bg-[#0b1421] p-4 md:p-7" aria-label="Cursos e videoaulas">
    <header className="flex flex-wrap items-center justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-widest text-brand-blue">Aprendizagem em pequenos passos</p><h2 className="mt-2 text-3xl font-black">{manage ? 'Painel do professor' : 'Cursos e videoaulas'}</h2></div><WorkspaceNavigation label="Ambiente educacional" active={navigationActive&&activeModule==='courses'?(manage?'teacher':'student'):''} groups={[{id:'modo',label:account?.teacher?'Professor e aluno':'Meus estudos',items:[{id:'student',label:'Meu aprendizado',onSelect:()=>chooseLearningMode(false)},...(account?.teacher?[{id:'teacher',label:'Gerenciar cursos',onSelect:()=>chooseLearningMode(true)}]:[])]}]}/></header>
    {activeModule === 'hub' && <AsyncPanel><HubParticipationPanel token={token} student onOpenCourse={id => { selectModule('courses'); void openCourse(id); }}/></AsyncPanel>}
    {activeModule==='courses'&&!course&&!editingCourse&&!manage&&<LearningOverview token={token} history={history} historyLoading={historyLoading} summary={overview.summary} attempts={overview.attempts} loading={overview.loading} error={overview.error} onRetry={overview.retry} onPerformance={()=>selectModule('performance')} onResume={(found,next)=>{courseTrigger.current=document.activeElement as HTMLElement;setCourse(found);setModuleId(next.module_id);setLesson(next);}}/>}
    {activeModule==='courses'&&!course&&!editingCourse&&notices&&<section aria-label="Notificações recentes" className="rounded-2xl border border-white/15 p-5"><h3 className="text-xl font-bold">Notificações relevantes</h3><p className="mt-2 text-sm">{notices.unread} não lidas. O histórico completo está no sino da barra superior.</p>{notices.items.length?<ul className="mt-2 divide-y divide-white/10">{notices.items.map(item=><li key={item.id}><button className="min-h-11 rounded-lg text-left text-sm font-bold" onClick={()=>{selectModule(item.kind==='QUESTION'||item.kind==='FOLLOW_UP'?'inbox':'questions');setQuestionScope({question:item.question_id,inbox:item.kind==='QUESTION'||item.kind==='FOLLOW_UP'});}}>{item.read_at?'':'Não lida · '}{({QUESTION:'Nova dúvida recebida',FOLLOW_UP:'Nova mensagem do aluno',REPLY:'Resposta recebida',STATUS:'Atendimento atualizado'} as Record<string,string>)[item.kind]||'Atualização de dúvida'} · Dúvida #{item.question_id}</button></li>)}</ul>:<p className="mt-2 text-sm text-ink/80">Nenhuma notificação registrada até agora.</p>}</section>}
    {activeModule==='courses'&&!course&&!editingCourse&&<><DashboardActions title={manage?'Ensinar e acompanhar':'Organize seu aprendizado'} description={manage?'Use as ferramentas dos seus cursos para publicar aulas, materiais e avaliações e acompanhar estudantes autorizados.':'Para estudantes e pessoas que buscam qualificação, o primeiro emprego ou novos caminhos. Escolha o conteúdo conforme seu objetivo; os requisitos reais das vagas podem orientar suas prioridades.'} actions={[
      ...(manage&&account?.teacher?[{label:'Criar um curso',description:'Organizar disciplinas, módulos e videoaulas pelo formulário existente.',onSelect:()=>setEditingCourse(true)},{label:'Responder dúvidas recebidas',description:'Abrir a caixa de dúvidas dos cursos em que você tem permissão.',onSelect:()=>selectModule('inbox')}]:[...(history.find(item=>!item.completed_at&&item.course_id&&item.lesson_id)?[{label:'Retomar meu estudo',description:(()=>{const item=history.find(item=>!item.completed_at&&item.course_id&&item.lesson_id)!;return (item.course_title||'Curso')+' · '+(item.title||'Aula em andamento')})(),onSelect:()=>{const item=history.find(item=>!item.completed_at&&item.course_id&&item.lesson_id)!;void openCourse(item.course_id!).then(opened=>{if(opened)setLesson({id:item.lesson_id!} as Lesson)});}}]:[]),{label:'Explorar cursos e videoaulas',description:'Pesquisar pelo título ou categoria no catálogo disponível.',onSelect:()=>{courseArea.current?.scrollIntoView({behavior:'smooth',block:'start'});courseArea.current?.querySelector<HTMLInputElement>('input[type="search"]')?.focus()}},{label:'Acompanhar progresso e competências',description:'Consultar resultados, histórico e conquistas verificadas, sem confundir com certificados formais.',onSelect:()=>selectModule('performance')}]),
      {label:'Abrir avaliações',description:manage?'Criar e acompanhar mini provas nos cursos autorizados.':'Consultar avaliações disponíveis e tentativas permitidas.',onSelect:()=>selectModule('assessments')},
      {label:'Consultar materiais',description:'Abrir a biblioteca de PDFs e materiais complementares.',onSelect:()=>selectModule('library')},
      {label:manage?'Escolher curso para acompanhar alunos':'Organizar minhas trilhas',description:manage?'Escolher um curso próprio e usar o acompanhamento dos alunos.':'Consultar objetivos e recomendações de aprendizagem existentes.',onSelect:()=>{if(manage){courseArea.current?.scrollIntoView({behavior:'smooth'});}else selectModule('adaptive');}},
      ...(!manage?[{label:'Tirar uma dúvida',description:'Enviar texto ou mídia ao professor, conforme as permissões do curso.',onSelect:()=>selectModule('questions')},{label:'Estágios e primeiro emprego',description:'Consultar oportunidades públicas e orientações para começar.',onSelect:()=>selectModule('young')}]:[])
    ]}/>{historyLoading&&!manage&&<p role="status">Preparando seu histórico de estudos…</p>}{historyError&&!manage&&<p role="status">{historyError}</p>}</>}
    <NotificationSlot><QuestionNotifications token={token} onSummary={setNotices} onOpen={(id, inbox) => { selectModule(inbox ? 'inbox' : 'questions'); setQuestionScope({ question: id, inbox }); }} /></NotificationSlot>
    <CapacitaModuleNav label="Funcionalidades de aprendizagem" value={activeModule} current={navigationActive} onChange={id=>selectModule(id)} items={[
      { id: 'courses', label: 'Cursos e videoaulas' }, { id: 'adaptive', label: 'Minhas Trilhas' }, { id: 'passport', label: 'Passaporte Digital' }, { id: 'library', label: 'Biblioteca virtual' },
      { id: 'assessments', label: 'Mini provas' }, { id: 'performance', label: 'Meu desempenho e competências' },
      { id: 'observatory', label: 'Observatório regional' }, { id: 'young', label: 'Estágios e aprendizagem' }, { id: 'hub', label: 'Programas institucionais' }, { id: 'questions', label: 'Minhas dúvidas' }, ...(account?.teacher ? [{ id: 'inbox', label: 'Dúvidas recebidas' }] : []),
      { id: 'support', label: 'Solicitar apoio educacional' }, ...(account?.admin ? [{ id: 'admin', label: 'Administração' }] : []),
    ]} />
    {activeModule === 'adaptive' && <AsyncPanel><AdaptivePanel token={token} onStudy={async (cid,mid,lid)=>{selectModule('courses');await openCourse(cid);setModuleId(mid);if(lid)setLesson({id:lid} as Lesson);}}/></AsyncPanel>}
    {activeModule === 'observatory' && <AsyncPanel><ObservatoryPanel token={token} trail="capacita"/></AsyncPanel>}
    {activeModule === 'young' && <AsyncPanel><YoungOpportunities/></AsyncPanel>}
    {activeModule === 'passport' && <AsyncPanel><PassportPanel token={token}/></AsyncPanel>}
    {account && support && <LearningPanelBoundary><InclusionSupport token={token} admin={account.admin} onClose={() => { setSupport(false); setActiveModule('courses'); }} /></LearningPanelBoundary>}
    <div className={activeModule === 'courses' ? "flex flex-wrap gap-2" : "hidden"}>{course && <button className={buttonClass} onClick={() => setAssessmentScope({ course: course.id, ...(selectedModule ? { module: selectedModule.id } : {}) })}>Avaliações deste {selectedModule ? 'módulo' : 'curso'}</button>}</div>
    {account && assessmentScope && <LearningPanelBoundary><AssessmentPanel key={JSON.stringify(assessmentScope)} token={token} teacher={account.teacher} scope={assessmentScope} onClose={() => { setAssessmentScope(null); setActiveModule('courses'); }} /></LearningPanelBoundary>}
    {performance && <LearningPanelBoundary><PerformancePanel token={token} summary={overview.summary} attempts={overview.attempts} onClose={() => { setPerformance(false); setActiveModule('courses'); }} /></LearningPanelBoundary>}
    <div className={activeModule === 'courses' ? "flex flex-wrap gap-2" : "hidden"}>{course && <button className={buttonClass} onClick={() => setLibraryScope({ course: course.id, ...(selectedModule ? { module: selectedModule.id } : {}) })}>Materiais deste {selectedModule ? 'módulo' : 'curso'}</button>}{course && lesson && <button className={buttonClass} onClick={() => setLibraryScope({ course: course.id, lesson: lesson.id })}>Materiais desta aula</button>}</div>
    {account && libraryScope && <LearningPanelBoundary><LibraryPanel key={JSON.stringify(libraryScope)} token={token} teacher={account.teacher} scope={libraryScope} onClose={() => { setLibraryScope(null); setActiveModule('courses'); }} /></LearningPanelBoundary>}
    <div className={activeModule === 'courses' ? "flex flex-wrap gap-2" : "hidden"}>{course && <button className={buttonClass} onClick={() => setQuestionScope({ course: course.id, inbox: manage })}>Dúvidas deste curso</button>}{course && selectedModule && <button className={buttonClass} onClick={() => setQuestionScope({ course: course.id, module: selectedModule.id, inbox: manage })}>Dúvidas deste módulo</button>}{course && lesson && <button className={buttonClass} onClick={() => setQuestionScope({ course: course.id, module: lesson.module_id || lessons.find(item => item.id === lesson.id)?.module_id, lesson: lesson.id, inbox: manage })}>Dúvidas desta aula</button>}</div>
    {account && questionScope && <LearningPanelBoundary><QuestionBoard key={JSON.stringify(questionScope)} token={token} account={account} scope={questionScope} onClose={() => { setQuestionScope(null); setActiveModule('courses'); }} /></LearningPanelBoundary>}
    {error && <p role="alert" className="rounded-xl border border-red-300/30 p-3 text-red-200">{error}</p>}
    {loading && <DashboardSkeleton label="Carregando cursos…"/>}
    <div ref={courseArea} hidden={activeModule !== 'courses'} className="space-y-6">
    {!course && !editingCourse && <label className="block text-sm font-bold">Buscar cursos<input type="search" maxLength={120} value={search} onChange={event => setSearch(event.target.value)} className={inputClass} placeholder="Título ou categoria" /></label>}
    {!loading && !course && !editingCourse && <>
      {manage && <button className={`${buttonClass} border-brand-blue text-brand-blue`} onClick={() => setEditingCourse(true)}>Criar curso</button>}
      {!manage && history.length > 0 && <div><h3 className="mb-3 text-xl font-bold">Retomar meus estudos</h3><div className="flex flex-wrap gap-3">{history.slice(0, 6).map(item => <button key={item.lesson_id} className={buttonClass} onClick={async () => { await openCourse(item.course_id!); setLesson({ id: item.lesson_id! } as Lesson); }}>{item.course_title} · {item.title}{item.completed_at ? ' · concluída' : ' · continuar'}</button>)}</div></div>}
      {courses.some(item=>item.category)&&<div className="space-y-2"><p className="text-sm font-bold">Categorias disponíveis nesta página</p><div className="flex flex-wrap gap-2">{[...new Set(courses.map(item=>item.category).filter(Boolean))].map(category=><button type="button" key={category} className={buttonClass} onClick={()=>setSearch(category)}>{category}</button>)}</div>{search&&<button type="button" className="min-h-11 text-sm underline" onClick={()=>setSearch('')}>Limpar busca de cursos</button>}</div>}
      <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">{courses.map(item => <article key={item.id} className="flex flex-col gap-3 rounded-2xl border border-white/15 bg-[#121c2b] p-5">{item.image_url && <img src={item.image_url} alt="" loading="lazy" decoding="async" width={640} height={360} referrerPolicy="no-referrer" className="aspect-video w-full rounded-xl object-cover" />}<p className="text-xs uppercase text-brand-blue">{item.category}</p><h3 className="text-2xl font-black">{item.title}</h3><p className="line-clamp-3 text-sm leading-6 text-ink/65">{item.description}</p><p className="text-sm">Professor: {item.teacher_name}</p>{manage && <p className="text-xs">{statusLabel(item.status)}</p>}<button className={`${buttonClass} mt-auto`} onClick={() => void openCourse(item.id)}>{manage ? 'Organizar curso' : 'Conhecer curso'}</button>{account?.admin && manage && <button className={buttonClass} onClick={() => api(`${root}/admin/courses/${item.id}/status`, token, { method: 'PUT', body: JSON.stringify({ status: item.status === 'HIDDEN' ? 'DRAFT' : 'HIDDEN' }) }).then(() => setRevision(value => value + 1)).catch(failure => setError(failure.message))}>{item.status === 'HIDDEN' ? 'Devolver a rascunho' : 'Ocultar curso'}</button>}</article>)}</div>
      <nav aria-label="Páginas de cursos" className="flex flex-wrap items-center gap-3"><button className={buttonClass} disabled={page === 0} onClick={() => setPage(value => value - 1)}>Cursos anteriores</button><span aria-live="polite">Página {page + 1}</span><button className={buttonClass} disabled={!hasMore || page >= 416} onClick={() => setPage(value => value + 1)}>Próximos cursos</button></nav>
      {courses.length === 0 && <p className="rounded-2xl border border-dashed border-white/20 p-6 text-ink/65">{query ? 'Nenhum curso encontrado. Ajuste a busca ou volte às páginas anteriores.' : manage ? 'Você ainda não tem cursos. Crie o primeiro e organize suas disciplinas e módulos.' : 'Ainda não há cursos publicados. Os materiais de apoio por etapa continuam disponíveis abaixo.'}</p>}
    </>}
    {editingCourse && <LearningPanelBoundary><CourseForm token={token} course={course} onCancel={() => setEditingCourse(false)} onSaved={() => { setEditingCourse(false); void refreshCourse(); setRevision(value => value + 1); }} /></LearningPanelBoundary>}
    {course && !editingCourse && !lesson && !editor && <>
      <div className="flex flex-wrap gap-2"><button className={buttonClass} onClick={closeCourse}>Voltar aos cursos</button>{manage && <><button className={buttonClass} onClick={() => setEditingCourse(true)}>Editar curso</button><button className={buttonClass} onClick={() => setSectionEditor({ type: 'subject', parent: course.id })}>Adicionar disciplina</button><button className={buttonClass} onClick={() => api<typeof students>(`${root}/courses/${course.id}/students`, token).then(setStudents).catch(failure => setError(failure.message))}>Progresso dos alunos</button></>}</div>
      <h3 data-course-heading tabIndex={-1} className="text-3xl font-black">{course.title}</h3><p className="whitespace-pre-line text-ink/70">{course.description}</p>
      {students && <div className="overflow-x-auto"><h4 className="mb-3 font-bold">Participação neste curso</h4>{students.length === 0 ? <p>Nenhum aluno iniciou as aulas deste curso.</p> : <table className="w-full text-left text-sm"><thead><tr><th>Aluno</th><th>Aulas iniciadas</th><th>Aulas concluídas</th></tr></thead><tbody>{students.map(student => <tr key={student.id}><td className="py-3">{student.full_name}</td><td>{student.started_lessons}</td><td>{student.completed_lessons}</td></tr>)}</tbody></table>}</div>}
      {sectionEditor && <LearningPanelBoundary><SectionForm key={`${sectionEditor.type}-${sectionEditor.item?.id || 'new'}`} token={token} {...sectionEditor} onCancel={() => setSectionEditor(null)} onSaved={() => { setSectionEditor(null); void refreshCourse(); }} /></LearningPanelBoundary>}
      {selectedModule ? <div className="space-y-4"><button className={buttonClass} onClick={() => setModuleId(null)}>Voltar às disciplinas</button><h4 data-module-heading tabIndex={-1} className="text-2xl font-bold">{selectedModule.title}</h4><p className="text-ink/70">{selectedModule.description}</p>{manage&&course&&<AsyncPanel><AdaptiveModulePolicy key={selectedModule.id} token={token} moduleId={selectedModule.id} courseId={course.id} modules={modules}/></AsyncPanel>}{manage && <button className={buttonClass} onClick={() => setEditor({ moduleId: selectedModule.id })}>Criar videoaula</button>}<LessonList lessons={selectedModule.lessons} manage={manage} onWatch={setLesson} onEdit={item => setEditor({ moduleId: selectedModule.id, lessonId: item.id })} /></div>
        : <div className="space-y-5">{course.subjects?.map(subject => <article key={subject.id} className="rounded-2xl border border-white/15 p-5"><h4 className="text-2xl font-bold">{subject.title}</h4><p className="mt-2 text-ink/65">{subject.description}</p>{manage && <div className="my-3 flex flex-wrap gap-2"><button className={buttonClass} onClick={() => setSectionEditor({ type: 'subject', parent: course.id, item: subject })}>Editar disciplina</button><button className={buttonClass} onClick={() => setSectionEditor({ type: 'module', parent: subject.id })}>Adicionar módulo</button></div>}<div className="mt-4 grid gap-3 md:grid-cols-2">{subject.modules.map(module => <div key={module.id} className="rounded-xl bg-[#121c2b] p-4"><button className="text-left text-lg font-bold text-brand-blue" onClick={() => setModuleId(module.id)}>{module.title}</button><p className="mt-2 text-sm text-ink/65">{module.description}</p><p className="mt-2 text-xs">{module.lessons.length} aulas disponíveis</p>{manage && <button className={`${buttonClass} mt-3`} onClick={() => setSectionEditor({ type: 'module', parent: subject.id, item: module })}>Editar módulo</button>}</div>)}</div></article>)}{!course.subjects?.length && <p className="text-ink/60">Nenhuma disciplina disponível neste curso.</p>}</div>}
    </>}
    {editor && <LearningPanelBoundary><LessonEditor token={token} {...editor} onCancel={() => setEditor(null)} onSaved={() => { setEditor(null); void refreshCourse(); }} /></LearningPanelBoundary>}
    {lesson && <LearningPanelBoundary><LessonPlayer token={token} lessonId={lesson.id} preview={manage} onBack={() => { setLesson(null); setRevision(value => value + 1); }} onNext={next ? () => setLesson(next) : undefined} /></LearningPanelBoundary>}
    </div>
    {account?.admin && activeModule === 'admin' && <LearningPanelBoundary><LearningAdmin token={token} onChanged={() => setRevision(value => value + 1)} /></LearningPanelBoundary>}
  </section>;
}

function LessonList({ lessons, manage, onWatch, onEdit }: { lessons: Lesson[]; manage: boolean; onWatch: (lesson: Lesson) => void; onEdit: (lesson: Lesson) => void }) {
  return <div className="space-y-3">{lessons.map(lesson => <article key={lesson.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-white/15 p-4"><div><h5 className="text-lg font-bold">{lesson.title}</h5><p className="text-sm text-ink/65">{manage ? `${statusLabel(lesson.status)} · ${lesson.validation_status === 'READY' ? 'Vídeo validado' : 'Sem vídeo validado'}` : `${Math.round((lesson.duration_seconds || 0) / 60)} minutos`}</p></div><div className="flex gap-2">{lesson.validation_status === 'READY' && <button className={buttonClass} onClick={() => onWatch(lesson)}>{manage ? 'Pré-visualizar' : 'Assistir aula'}</button>}{manage && <button className={buttonClass} onClick={() => onEdit(lesson)}>Editar aula</button>}</div></article>)}{lessons.length === 0 && <p className="text-ink/65">Ainda não há videoaulas disponíveis neste módulo.</p>}</div>;
}
