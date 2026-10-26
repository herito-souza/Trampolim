import { useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { createDiagnosticResult, diagnosticQuestionCount, firstQuestion, getRecommendations, trackQuestions } from './recommendations';
import type { DiagnosticResult, Question } from './recommendations';

interface DiagnosticQuizProps {
  onContinue: (mode: 'login' | 'cadastro') => void;
  onDiagnosticComplete?: (result: DiagnosticResult) => void;
  isAuthenticated?: boolean;
  hasPreviousDiagnostic?: boolean;
  startNewTest?: boolean;
}

export function DiagnosticQuiz({ onContinue, onDiagnosticComplete, isAuthenticated = false, hasPreviousDiagnostic = false, startNewTest = false }: DiagnosticQuizProps) {
  const [answers, setAnswers] = useState<number[]>([]);
  const [selectedOpt, setSelectedOpt] = useState<number | null>(null);
  const [result, setResult] = useState<DiagnosticResult | null>(null);
  const [reviewing, setReviewing] = useState(false);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [notice, setNotice] = useState('');
  useEffect(() => {
    if (isAuthenticated && !startNewTest) {
      try {
        const stored = sessionStorage.getItem('trampolim_diagnostic_result');
        if (stored) {
          const saved = JSON.parse(stored) as DiagnosticResult;
          if (Array.isArray(saved.answers) && saved.answers.length === diagnosticQuestionCount) {
            setAnswers(saved.answers);
            setResult(saved);
          }
        }
      } catch { sessionStorage.removeItem('trampolim_diagnostic_result'); }
    }
  }, [isAuthenticated, startNewTest]);
  const current = editingIndex ?? answers.length;
  const trackIndex = answers[0] ?? 0;
  const question: Question | undefined = current === 0 ? firstQuestion : trackQuestions[trackIndex]?.[current - 1];

  function selectOption(index: number) {
    if (selectedOpt !== null || !question) return;
    setSelectedOpt(index);
    window.setTimeout(() => {
      const nextAnswers = editingIndex === null ? [...answers, index] : answers.map((answer, answerIndex) => answerIndex === editingIndex ? index : answer);
      setSelectedOpt(null);
      setAnswers(nextAnswers);
      if (editingIndex !== null) {
        if (editingIndex === 0 && index !== answers[0]) {
          setAnswers([index]);
          setReviewing(false);
          setNotice('Sua primeira resposta mudou, então a trilha foi atualizada. Responda às perguntas seguintes para rever o diagnóstico.');
        } else {
          setReviewing(true);
          setNotice('Resposta atualizada. Confira suas escolhas antes de confirmar.');
        }
        setEditingIndex(null);
      } else if (nextAnswers.length === diagnosticQuestionCount) {
        setReviewing(true);
      }
    }, 250);
  }

  function confirmDiagnostic() {
    if (answers.length !== diagnosticQuestionCount) return;
    if (hasPreviousDiagnostic && !window.confirm('Confirmar este novo diagnóstico e substituir o resultado salvo anteriormente?')) return;
    const completed = createDiagnosticResult(answers);
    sessionStorage.setItem('trampolim_diagnostic_result', JSON.stringify(completed));
    onDiagnosticComplete?.(completed);
    setResult(completed);
    setReviewing(false);
    setNotice('');
  }

  function changeAnswer(index: number) {
    if (index === 0) setNotice('Se mudar a primeira resposta, as perguntas da trilha atual serão reiniciadas.');
    setEditingIndex(index);
    setReviewing(false);
  }

  const courses = getRecommendations(result, 'course').slice(0, 3);
  const jobs = getRecommendations(result, 'job').slice(0, 3);
  const progress = (answers.length / diagnosticQuestionCount) * 100;

  return (
    <section className="border-y border-white/10 bg-[#0b1421] px-[6vw] py-20 md:py-28" id="diagnostico">
      <div className="mx-auto max-w-7xl">
      <div className="mb-3 text-xs font-extrabold uppercase tracking-[0.2em] text-brand-yellow">Seu próximo passo</div>
      <h2 className="mb-10 max-w-3xl text-4xl font-bold leading-[0.95] md:text-6xl">Descubra caminhos que<br /><span className="text-white/45">combinam com você.</span></h2>

      <div className="mx-auto max-w-3xl rounded-3xl border border-white/10 bg-[#121c2b] p-6 shadow-2xl md:p-10">
        {!result && reviewing ? (
          <div aria-labelledby="review-title">
            <div className="mb-3 text-xs font-extrabold uppercase tracking-[0.18em] text-brand-blue">Conferência final</div>
            <h3 id="review-title" className="text-3xl font-black">Revise suas respostas</h3>
            <p className="mt-2 text-sm text-ink/65">Você pode voltar a qualquer resposta antes de salvar. O diagnóstico anterior só será substituído depois da confirmação.</p>
            <ol className="mt-5 space-y-2">{answers.map((answer, index) => {
              const item = index === 0 ? firstQuestion : trackQuestions[answers[0]]?.[index - 1];
              return item ? <li key={index} className="flex items-start justify-between gap-3 rounded-xl bg-paper p-4"><div><p className="text-xs font-bold text-ink/55">Pergunta {index + 1}: {item.prompt}</p><p className="mt-1 font-semibold">{item.options[answer]?.label}</p></div><button type="button" onClick={() => changeAnswer(index)} aria-label={`Alterar resposta da pergunta ${index + 1}`} className="shrink-0 rounded-full border border-ink/20 px-3 py-2 text-sm font-bold focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-blue">Alterar</button></li> : null;
            })}</ol>
            {notice && <p role="status" className="mt-4 rounded-xl bg-brand-yellow/20 p-3 text-sm font-semibold">{notice}</p>}
            <div className="mt-6 flex flex-wrap gap-3"><Button onClick={confirmDiagnostic}>Confirmar diagnóstico</Button><button type="button" onClick={() => { setAnswers([]); setReviewing(false); setNotice(''); }} className="rounded-full border border-ink/25 px-5 py-3 font-bold focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-blue">Refazer respostas</button></div>
          </div>
        ) : !result && question ? (
          <div>
            <div className="mb-2 h-1.5 overflow-hidden rounded-full bg-white/10" role="progressbar" aria-label={`Progresso do diagnóstico: pergunta ${current + 1} de ${diagnosticQuestionCount}`} aria-valuemin={0} aria-valuemax={diagnosticQuestionCount} aria-valuenow={current}><div className="h-full bg-brand-yellow transition-all duration-300 ease-out" style={{ width: `${progress}%` }} /></div>
            <div className="text-xs font-extrabold uppercase tracking-[0.18em] text-brand-blue">Pergunta {current + 1} <span className="text-white/40">/ {diagnosticQuestionCount}</span></div>
            {current > 0 && <p className="mt-2 text-xs font-bold uppercase tracking-wider text-brand-green">Trilha personalizada a partir da sua primeira resposta</p>}
            <h3 id="question-title" className="my-4 text-3xl font-bold md:text-4xl">{question.prompt}</h3>
            {notice && <p role="status" className="mb-3 rounded-xl bg-brand-yellow/20 p-3 text-sm font-semibold">{notice}</p>}
            <div role="group" aria-labelledby="question-title" className="mt-5 flex flex-col gap-2.5">
              {question.options.map((option, index) => <button key={option.label} type="button" aria-pressed={editingIndex === null ? false : answers[editingIndex] === index} disabled={selectedOpt !== null} onClick={() => selectOption(index)} className={`block w-full rounded-xl border p-[14px_18px] text-left font-semibold transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-blue disabled:cursor-wait ${selectedOpt === index ? 'border-brand-yellow bg-brand-yellow/10' : 'border-white/10 bg-paper hover:border-brand-yellow/60 hover:bg-brand-yellow/5'}`}>{option.label}</button>)}
            </div>
            {editingIndex !== null && <button type="button" onClick={() => { setEditingIndex(null); setReviewing(true); }} className="mt-4 rounded-full border border-ink/25 px-4 py-2 text-sm font-bold focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-blue">Cancelar alteração</button>}
          </div>
        ) : result ? (
          <div className="animate-in text-center fade-in zoom-in duration-300">
            <span className="mb-4 inline-block rounded-full bg-brand-yellow px-[18px] py-2 text-[13px] font-extrabold text-ink">Diagnóstico concluído</span>
            <h3 className="mb-2 text-3xl font-black">Trilha: {result.trackName}</h3>
            <p className="mx-auto max-w-xl text-[15px] text-ink/65">Suas respostas indicam estas áreas para explorar. As recomendações consideram as escolhas feitas nas dez perguntas.</p>
            <div className="mt-7 grid gap-6 text-left md:grid-cols-2">
              <div className="rounded-2xl bg-paper p-5"><h4 className="font-serif text-xl font-black">Cursos sugeridos</h4>{courses.map(course => <p key={course.id} className="mt-3 border-t border-ink/10 pt-3 text-sm font-bold">{course.title}<span className="mt-1 block text-xs font-medium text-ink/60">{course.provider}</span></p>)}</div>
              <div className="rounded-2xl bg-paper p-5"><h4 className="font-serif text-xl font-black">Áreas de trabalho</h4>{jobs.map(job => <p key={job.id} className="mt-3 border-t border-ink/10 pt-3 text-sm font-bold">{job.title}<span className="mt-1 block text-xs font-medium text-ink/60">{job.provider}</span></p>)}</div>
            </div>
            {isAuthenticated ? (
              <div className="mt-8"><Button onClick={() => onContinue('login')}>Ver minhas recomendações</Button></div>
            ) : (
              <div className="mt-8 border-t border-ink/10 pt-6"><p className="font-extrabold">Monte seu perfil e acesse as oportunidades</p><div className="mt-4 flex flex-wrap justify-center gap-3"><button onClick={() => onContinue('cadastro')} className="rounded-full border-0 bg-brand-red px-6 py-3 font-extrabold text-white cursor-pointer">Criar conta</button><button onClick={() => onContinue('login')} className="rounded-full border-2 border-ink bg-transparent px-6 py-[10px] font-extrabold text-ink cursor-pointer">Já tenho uma conta</button></div></div>
            )}
          </div>
        ) : null}
      </div>
      </div>
    </section>
  );
}
