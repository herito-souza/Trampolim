import { ObservatoryPanel } from '../career/ObservatoryPanel';
import { YoungOpportunities } from '../career/YoungOpportunities';
import { AdaptivePanel } from './AdaptivePanel';
import { PassportPanel } from './PassportPanel';
import { AdaptiveModulePolicy } from './AdaptiveModulePolicy';
import { HubParticipationPanel } from '../hub/HubParticipationPanel';
import { CapacitaModuleNav } from './CapacitaModuleNav';
import { lazy, useEffect, useRef, useState } from 'react';
import { LearningPanelBoundary } from './LearningPanelBoundary';
import type { FormEvent } from 'react';
import { api } from '../account/api';
import { LessonPlayer } from './LessonPlayer';
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
import { buttonClass, inputClass } from './types';
type Account = { id: number; name: string; email: string; teacher: boolean; admin: boolean };
const root = '/capacita/learning';

export function LearningPortal({ token }: { token: string }) {
  const [activeModule, setActiveModule] = useState('courses');
  function selectModule(id: string) {
    setActiveModule(id); setQuestionScope(null); setLibraryScope(null); setAssessmentScope(null); setPerformance(false); setSupport(false);
    if (id === 'library') setLibraryScope({});
    if (id === 'assessments') setAssessmentScope({});
    if (id === 'performance') setPerformance(true);
    if (id === 'questions') setQuestionScope({ inbox: false });
    if (id === 'inbox') setQuestionScope({ inbox: true });
    if (id === 'support') setSupport(true);
  }
  const courseArea = useRef<HTMLDivElement>(null);
  useEffect(() => { if (activeModule !== 'courses') courseArea.current?.querySelectorAll('video, audio').forEach(media => (media as HTMLMediaElement).pause()); }, [activeModule]);
  const courseTrigger = useRef<HTMLElement | null>(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [hasMore, setHasMore] = useState(false);
  useEffect(() => { const timer = setTimeout(() => { setQuery(search); setPage(0); }, 300); return () => clearTimeout(timer); }, [search]);
  const [account, setAccount] = useState<Account | null>(null);
  const [courses, setCourses] = useState<Course[]>([]);
  const [manage, setManage] = useState(false);
  const [course, setCourse] = useState<Course | null>(null);
  const [moduleId, setModuleId] = useState<number | null>(null);
  const [lesson, setLesson] = useState<Lesson | null>(null);
  const [editor, setEditor] = useState<{ moduleId: number; lessonId?: number } | null>(null);
  const [editingCourse, setEditingCourse] = useState(false);
  const [sectionEditor, setSectionEditor] = useState<{ type: 'subject' | 'module'; parent: number; item?: Subject | Module } | null>(null);
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
    api<Account>(`${root}/me`, token, { signal: abort.signal }).then(user => { if (active) setAccount(user); }).catch(failure => { if (active) setError(failure.message); });
    return () => { active = false; abort.abort(); };
  }, [token, revision]);
  useEffect(() => {
    let active = true; const abort = new AbortController(); setLoading(true); setError('');
    Promise.all([api<Course[]>(`${root}/courses?manage=${manage}&limit=25&offset=${page * 24}&q=${encodeURIComponent(query)}`, token, { signal: abort.signal }), api<Progress[]>(`${root}/progress`, token, { signal: abort.signal })])
      .then(([rows, progress]) => { if (active) { setHasMore(Array.isArray(rows) && rows.length > 24); setCourses(Array.isArray(rows) ? rows.slice(0, 24) : []); setHistory(Array.isArray(progress) ? progress : []); } })
      .catch(failure => { if (active) setError(failure.message); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; abort.abort(); };
  }, [token, manage, revision, page, query]);
  async function openCourse(id: number) {
    courseTrigger.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    setError(''); setStudents(null); setLesson(null); setEditor(null); setModuleId(null);
    try { setCourse(await api<Course>(`${root}/courses/${id}?manage=${manage}`, token)); } catch (failure) { setError(failure instanceof Error ? failure.message : 'Curso indisponível.'); }
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
    <header className="flex flex-wrap items-center justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-widest text-brand-blue">Aprendizagem em pequenos passos</p><h2 className="mt-2 text-3xl font-black">{manage ? 'Painel do professor' : 'Cursos e videoaulas'}</h2></div><nav aria-label="Ambiente educacional" className="flex flex-wrap gap-2"><button className={buttonClass} aria-pressed={!manage} onClick={() => { selectModule('courses'); setManage(false); setPage(0); closeCourse(); }}>Meu aprendizado</button>{account?.teacher && <button className={buttonClass} aria-pressed={manage} onClick={() => { selectModule('courses'); setManage(true); setPage(0); closeCourse(); }}>Gerenciar cursos</button>}</nav></header>
    {activeModule === 'hub' && <HubParticipationPanel token={token} student onOpenCourse={id => { selectModule('courses'); void openCourse(id); }}/>}
    <QuestionNotifications token={token} onOpen={(id, inbox) => { selectModule(inbox ? 'inbox' : 'questions'); setQuestionScope({ question: id, inbox }); }} />
    <CapacitaModuleNav label="Funcionalidades de aprendizagem" value={activeModule} onChange={selectModule} items={[
      { id: 'courses', label: 'Cursos e videoaulas' }, { id: 'adaptive', label: 'Minhas Trilhas' }, { id: 'passport', label: 'Passaporte Digital' }, { id: 'library', label: 'Biblioteca virtual' },
      { id: 'assessments', label: 'Mini provas' }, { id: 'performance', label: 'Meu desempenho e competências' },
      { id: 'observatory', label: 'Observatório regional' }, { id: 'young', label: 'Estágios e aprendizagem' }, { id: 'hub', label: 'Programas institucionais' }, { id: 'questions', label: 'Minhas dúvidas' }, ...(account?.teacher ? [{ id: 'inbox', label: 'Dúvidas recebidas' }] : []),
      { id: 'support', label: 'Solicitar apoio educacional' }, ...(account?.admin ? [{ id: 'admin', label: 'Administração' }] : []),
    ]} />
    {activeModule === 'adaptive' && <AdaptivePanel token={token} onStudy={async (cid,mid,lid)=>{selectModule('courses');await openCourse(cid);setModuleId(mid);if(lid)setLesson({id:lid} as Lesson);}}/>}
    {activeModule === 'observatory' && <ObservatoryPanel token={token} trail="capacita"/>}
    {activeModule === 'young' && <YoungOpportunities/>}
    {activeModule === 'passport' && <PassportPanel token={token}/>}
    {account && support && <LearningPanelBoundary><InclusionSupport token={token} admin={account.admin} onClose={() => { setSupport(false); setActiveModule('courses'); }} /></LearningPanelBoundary>}
    <div className={activeModule === 'courses' ? "flex flex-wrap gap-2" : "hidden"}>{course && <button className={buttonClass} onClick={() => setAssessmentScope({ course: course.id, ...(selectedModule ? { module: selectedModule.id } : {}) })}>Avaliações deste {selectedModule ? 'módulo' : 'curso'}</button>}</div>
    {account && assessmentScope && <LearningPanelBoundary><AssessmentPanel key={JSON.stringify(assessmentScope)} token={token} teacher={account.teacher} scope={assessmentScope} onClose={() => { setAssessmentScope(null); setActiveModule('courses'); }} /></LearningPanelBoundary>}
    {performance && <LearningPanelBoundary><PerformancePanel token={token} onClose={() => { setPerformance(false); setActiveModule('courses'); }} /></LearningPanelBoundary>}
    <div className={activeModule === 'courses' ? "flex flex-wrap gap-2" : "hidden"}>{course && <button className={buttonClass} onClick={() => setLibraryScope({ course: course.id, ...(selectedModule ? { module: selectedModule.id } : {}) })}>Materiais deste {selectedModule ? 'módulo' : 'curso'}</button>}{course && lesson && <button className={buttonClass} onClick={() => setLibraryScope({ course: course.id, lesson: lesson.id })}>Materiais desta aula</button>}</div>
    {account && libraryScope && <LearningPanelBoundary><LibraryPanel key={JSON.stringify(libraryScope)} token={token} teacher={account.teacher} scope={libraryScope} onClose={() => { setLibraryScope(null); setActiveModule('courses'); }} /></LearningPanelBoundary>}
    <div className={activeModule === 'courses' ? "flex flex-wrap gap-2" : "hidden"}>{course && <button className={buttonClass} onClick={() => setQuestionScope({ course: course.id, inbox: manage })}>Dúvidas deste curso</button>}{course && selectedModule && <button className={buttonClass} onClick={() => setQuestionScope({ course: course.id, module: selectedModule.id, inbox: manage })}>Dúvidas deste módulo</button>}{course && lesson && <button className={buttonClass} onClick={() => setQuestionScope({ course: course.id, module: lesson.module_id || lessons.find(item => item.id === lesson.id)?.module_id, lesson: lesson.id, inbox: manage })}>Dúvidas desta aula</button>}</div>
    {account && questionScope && <LearningPanelBoundary><QuestionBoard key={JSON.stringify(questionScope)} token={token} account={account} scope={questionScope} onClose={() => { setQuestionScope(null); setActiveModule('courses'); }} /></LearningPanelBoundary>}
    {error && <p role="alert" className="rounded-xl border border-red-300/30 p-3 text-red-200">{error}</p>}
    {loading && <p role="status">Carregando cursos…</p>}
    <div ref={courseArea} hidden={activeModule !== 'courses'} className="space-y-6">
    {!course && !editingCourse && <label className="block text-sm font-bold">Buscar cursos<input type="search" maxLength={120} value={search} onChange={event => setSearch(event.target.value)} className={inputClass} placeholder="Título ou categoria" /></label>}
    {!loading && !course && !editingCourse && <>
      {manage && <button className={`${buttonClass} border-brand-blue text-brand-blue`} onClick={() => setEditingCourse(true)}>Criar curso</button>}
      {!manage && history.length > 0 && <div><h3 className="mb-3 text-xl font-bold">Retomar meus estudos</h3><div className="flex flex-wrap gap-3">{history.slice(0, 6).map(item => <button key={item.lesson_id} className={buttonClass} onClick={async () => { await openCourse(item.course_id!); setLesson({ id: item.lesson_id! } as Lesson); }}>{item.course_title} · {item.title}{item.completed_at ? ' · concluída' : ' · continuar'}</button>)}</div></div>}
      <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">{courses.map(item => <article key={item.id} className="flex flex-col gap-3 rounded-2xl border border-white/15 bg-[#121c2b] p-5">{item.image_url && <img src={item.image_url} alt="" loading="lazy" referrerPolicy="no-referrer" className="aspect-video w-full rounded-xl object-cover" />}<p className="text-xs uppercase text-brand-blue">{item.category}</p><h3 className="text-2xl font-black">{item.title}</h3><p className="line-clamp-3 text-sm leading-6 text-ink/65">{item.description}</p><p className="text-sm">Professor: {item.teacher_name}</p>{manage && <p className="text-xs">{statusLabel(item.status)}</p>}<button className={`${buttonClass} mt-auto`} onClick={() => void openCourse(item.id)}>{manage ? 'Organizar curso' : 'Conhecer curso'}</button>{account?.admin && manage && <button className={buttonClass} onClick={() => api(`${root}/admin/courses/${item.id}/status`, token, { method: 'PUT', body: JSON.stringify({ status: item.status === 'HIDDEN' ? 'DRAFT' : 'HIDDEN' }) }).then(() => setRevision(value => value + 1)).catch(failure => setError(failure.message))}>{item.status === 'HIDDEN' ? 'Devolver a rascunho' : 'Ocultar curso'}</button>}</article>)}</div>
      <nav aria-label="Páginas de cursos" className="flex flex-wrap items-center gap-3"><button className={buttonClass} disabled={page === 0} onClick={() => setPage(value => value - 1)}>Cursos anteriores</button><span aria-live="polite">Página {page + 1}</span><button className={buttonClass} disabled={!hasMore || page >= 416} onClick={() => setPage(value => value + 1)}>Próximos cursos</button></nav>
      {courses.length === 0 && <p className="rounded-2xl border border-dashed border-white/20 p-6 text-ink/65">{query ? 'Nenhum curso encontrado. Ajuste a busca ou volte às páginas anteriores.' : manage ? 'Você ainda não tem cursos. Crie o primeiro e organize suas disciplinas e módulos.' : 'Ainda não há cursos publicados. Os materiais de apoio por etapa continuam disponíveis abaixo.'}</p>}
    </>}
    {editingCourse && <CourseForm token={token} course={course} onCancel={() => setEditingCourse(false)} onSaved={() => { setEditingCourse(false); void refreshCourse(); setRevision(value => value + 1); }} />}
    {course && !editingCourse && !lesson && !editor && <>
      <div className="flex flex-wrap gap-2"><button className={buttonClass} onClick={closeCourse}>Voltar aos cursos</button>{manage && <><button className={buttonClass} onClick={() => setEditingCourse(true)}>Editar curso</button><button className={buttonClass} onClick={() => setSectionEditor({ type: 'subject', parent: course.id })}>Adicionar disciplina</button><button className={buttonClass} onClick={() => api<typeof students>(`${root}/courses/${course.id}/students`, token).then(setStudents).catch(failure => setError(failure.message))}>Progresso dos alunos</button></>}</div>
      <h3 data-course-heading tabIndex={-1} className="text-3xl font-black">{course.title}</h3><p className="whitespace-pre-line text-ink/70">{course.description}</p>
      {students && <div className="overflow-x-auto"><h4 className="mb-3 font-bold">Participação neste curso</h4>{students.length === 0 ? <p>Nenhum aluno iniciou as aulas deste curso.</p> : <table className="w-full text-left text-sm"><thead><tr><th>Aluno</th><th>Aulas iniciadas</th><th>Aulas concluídas</th></tr></thead><tbody>{students.map(student => <tr key={student.id}><td className="py-3">{student.full_name}</td><td>{student.started_lessons}</td><td>{student.completed_lessons}</td></tr>)}</tbody></table>}</div>}
      {sectionEditor && <SectionForm key={`${sectionEditor.type}-${sectionEditor.item?.id || 'new'}`} token={token} {...sectionEditor} onCancel={() => setSectionEditor(null)} onSaved={() => { setSectionEditor(null); void refreshCourse(); }} />}
      {selectedModule ? <div className="space-y-4"><button className={buttonClass} onClick={() => setModuleId(null)}>Voltar às disciplinas</button><h4 data-module-heading tabIndex={-1} className="text-2xl font-bold">{selectedModule.title}</h4><p className="text-ink/70">{selectedModule.description}</p>{manage&&course&&<AdaptiveModulePolicy key={selectedModule.id} token={token} moduleId={selectedModule.id} courseId={course.id} modules={modules}/>}{manage && <button className={buttonClass} onClick={() => setEditor({ moduleId: selectedModule.id })}>Criar videoaula</button>}<LessonList lessons={selectedModule.lessons} manage={manage} onWatch={setLesson} onEdit={item => setEditor({ moduleId: selectedModule.id, lessonId: item.id })} /></div>
        : <div className="space-y-5">{course.subjects?.map(subject => <article key={subject.id} className="rounded-2xl border border-white/15 p-5"><h4 className="text-2xl font-bold">{subject.title}</h4><p className="mt-2 text-ink/65">{subject.description}</p>{manage && <div className="my-3 flex flex-wrap gap-2"><button className={buttonClass} onClick={() => setSectionEditor({ type: 'subject', parent: course.id, item: subject })}>Editar disciplina</button><button className={buttonClass} onClick={() => setSectionEditor({ type: 'module', parent: subject.id })}>Adicionar módulo</button></div>}<div className="mt-4 grid gap-3 md:grid-cols-2">{subject.modules.map(module => <div key={module.id} className="rounded-xl bg-[#121c2b] p-4"><button className="text-left text-lg font-bold text-brand-blue" onClick={() => setModuleId(module.id)}>{module.title}</button><p className="mt-2 text-sm text-ink/65">{module.description}</p><p className="mt-2 text-xs">{module.lessons.length} aulas disponíveis</p>{manage && <button className={`${buttonClass} mt-3`} onClick={() => setSectionEditor({ type: 'module', parent: subject.id, item: module })}>Editar módulo</button>}</div>)}</div></article>)}{!course.subjects?.length && <p className="text-ink/60">Nenhuma disciplina disponível neste curso.</p>}</div>}
    </>}
    {editor && <LearningPanelBoundary><LessonEditor token={token} {...editor} onCancel={() => setEditor(null)} onSaved={() => { setEditor(null); void refreshCourse(); }} /></LearningPanelBoundary>}
    {lesson && <LessonPlayer token={token} lessonId={lesson.id} preview={manage} onBack={() => { setLesson(null); setRevision(value => value + 1); }} onNext={next ? () => setLesson(next) : undefined} />}
    </div>
    {account?.admin && activeModule === 'admin' && <LearningAdmin token={token} onChanged={() => setRevision(value => value + 1)} />}
  </section>;
}
function statusLabel(status: string) { return ({ DRAFT: 'Rascunho', PUBLISHED: 'Publicado', HIDDEN: 'Oculto' } as Record<string, string>)[status] || status; }
function LessonList({ lessons, manage, onWatch, onEdit }: { lessons: Lesson[]; manage: boolean; onWatch: (lesson: Lesson) => void; onEdit: (lesson: Lesson) => void }) {
  return <div className="space-y-3">{lessons.map(lesson => <article key={lesson.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-white/15 p-4"><div><h5 className="text-lg font-bold">{lesson.title}</h5><p className="text-sm text-ink/65">{manage ? `${statusLabel(lesson.status)} · ${lesson.validation_status === 'READY' ? 'Vídeo validado' : 'Sem vídeo validado'}` : `${Math.round((lesson.duration_seconds || 0) / 60)} minutos`}</p></div><div className="flex gap-2">{lesson.validation_status === 'READY' && <button className={buttonClass} onClick={() => onWatch(lesson)}>{manage ? 'Pré-visualizar' : 'Assistir aula'}</button>}{manage && <button className={buttonClass} onClick={() => onEdit(lesson)}>Editar aula</button>}</div></article>)}{lessons.length === 0 && <p className="text-ink/65">Ainda não há videoaulas disponíveis neste módulo.</p>}</div>;
}
function CourseForm({ token, course, onCancel, onSaved }: { token: string; course: Course | null; onCancel: () => void; onSaved: () => void }) {
  const [form, setForm] = useState({ title: course?.title || '', description: course?.description || '', category: course?.category || '', imageUrl: course?.image_url || '', status: course?.status || 'DRAFT' });
  const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) { event.preventDefault(); if (busy) return; setBusy(true); setError(''); try { await api(`${root}/courses${course ? `/${course.id}` : ''}`, token, { method: course ? 'PUT' : 'POST', body: JSON.stringify(form) }); onSaved(); } catch (failure) { setError(failure instanceof Error ? failure.message : 'Falha ao salvar curso.'); } finally { setBusy(false); } }
  return <form onSubmit={submit} className="space-y-4 rounded-2xl border border-brand-blue/30 p-5"><h3 className="text-2xl font-bold">{course ? 'Editar curso' : 'Novo curso'}</h3>{(['title', 'category', 'imageUrl'] as const).map(field => <label key={field} className="block font-bold">{{ title: 'Título do curso', category: 'Categoria do curso', imageUrl: 'Imagem de apresentação (URL HTTPS, opcional)' }[field]}<input required={field !== 'imageUrl'} type={field === 'imageUrl' ? 'url' : 'text'} maxLength={field === 'imageUrl' ? 1000 : 180} className={inputClass} value={form[field]} onChange={event => setForm({ ...form, [field]: event.target.value })} /></label>)}<label className="block font-bold">Descrição do curso<textarea maxLength={4000} className={inputClass} rows={4} value={form.description} onChange={event => setForm({ ...form, description: event.target.value })} /></label><label className="block font-bold">Situação do curso<select className={inputClass} value={form.status} onChange={event => setForm({ ...form, status: event.target.value })}>{['DRAFT', 'PUBLISHED', 'HIDDEN'].map(status => <option key={status} value={status}>{statusLabel(status)}</option>)}</select></label>{error && <p role="alert" className="text-red-200">{error}</p>}<div className="flex gap-3"><button disabled={busy} className={buttonClass}>Salvar curso</button><button type="button" disabled={busy} className={buttonClass} onClick={onCancel}>Cancelar</button></div></form>;
}
function SectionForm({ token, type, parent, item, onCancel, onSaved }: { token: string; type: 'subject' | 'module'; parent: number; item?: Subject | Module; onCancel: () => void; onSaved: () => void }) {
  const [form, setForm] = useState({ title: item?.title || '', description: item?.description || '', sortOrder: item?.sort_order || 0 }); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  async function save(event: FormEvent) { event.preventDefault(); if (busy) return; setBusy(true); try { const path = item ? `${root}/${type === 'subject' ? 'subjects' : 'modules'}/${item.id}` : `${root}/${type === 'subject' ? 'courses' : 'subjects'}/${parent}/${type === 'subject' ? 'subjects' : 'modules'}`; await api(path, token, { method: item ? 'PUT' : 'POST', body: JSON.stringify(form) }); onSaved(); } catch (failure) { setError(failure instanceof Error ? failure.message : 'Falha ao salvar.'); } finally { setBusy(false); } }
  return <form onSubmit={save} className="space-y-3 rounded-xl border border-brand-blue/30 p-4"><h4 className="text-xl font-bold">{type === 'subject' ? 'Disciplina' : 'Módulo'}</h4><label className="block font-bold">Título<input required maxLength={180} className={inputClass} value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} /></label><label className="block font-bold">Descrição<textarea maxLength={2000} className={inputClass} value={form.description} onChange={event => setForm({ ...form, description: event.target.value })} /></label><label className="block font-bold">Ordem<input type="number" min={0} required className={inputClass} value={form.sortOrder} onChange={event => setForm({ ...form, sortOrder: Number(event.target.value) })} /></label>{error && <p role="alert" className="text-red-200">{error}</p>}<button disabled={busy} className={buttonClass}>Salvar organização</button><button type="button" disabled={busy} className={`${buttonClass} ml-2`} onClick={onCancel}>Cancelar</button></form>;
}
function LearningAdmin({ token, onChanged }: { token: string; onChanged: () => void }) {
  const [accounts, setAccounts] = useState<{ id: number; full_name: string; email: string; roles: string }[]>([]);
  const [email, setEmail] = useState(''); const [role, setRole] = useState('TEACHER'); const [granted, setGranted] = useState(true); const [notice, setNotice] = useState(''); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  useEffect(() => { api<typeof accounts>(`${root}/admin/accounts`, token).then(setAccounts).catch(failure => setError(failure.message)); }, [token, notice]);
  async function save(event: FormEvent) { event.preventDefault(); if (busy) return; setBusy(true); setNotice(''); setError(''); try { await api(`${root}/admin/roles`, token, { method: 'PUT', body: JSON.stringify({ email, role, granted }) }); setNotice('Permissão atualizada.'); onChanged(); } catch (failure) { setError(failure instanceof Error ? failure.message : 'Não foi possível alterar a permissão.'); } finally { setBusy(false); } }
  return <details className="rounded-2xl border border-white/20 p-5"><summary className="cursor-pointer font-bold">Administração educacional</summary><form onSubmit={save} className="mt-4 space-y-3"><p className="text-sm text-ink/70">Permissões exclusivas para contas existentes do Capacita. As alterações são auditadas.</p><label className="block font-bold">E-mail da conta<input list="learning-accounts" required type="email" className={inputClass} value={email} onChange={event => setEmail(event.target.value)} /></label><datalist id="learning-accounts">{accounts.map(account => <option key={account.id} value={account.email}>{account.full_name} · {account.roles}</option>)}</datalist><label className="block font-bold">Papel<select className={inputClass} value={role} onChange={event => setRole(event.target.value)}><option value="TEACHER">Professor</option><option value="ADMIN">Administrador educacional</option></select></label><label className="flex gap-2"><input type="checkbox" checked={granted} onChange={event => setGranted(event.target.checked)} />Conceder permissão (desmarque para revogar)</label><button disabled={busy} className={buttonClass}>Atualizar permissão</button>{notice && <p role="status">{notice}</p>}{error && <p role="alert" className="text-red-200">{error}</p>}</form></details>;
}
