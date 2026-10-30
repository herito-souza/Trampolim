import { useEffect, useRef, useState } from 'react';
import { API_URL, api } from '../account/api';
import type { Lesson, Progress } from './types';
import { buttonClass } from './types';
import { useAccessibility } from './accessibilityContext';
type Playback = { sessionId: string; videoPath: string; captionsPath: string; positionSeconds: number };
export function LessonPlayer({ token, lessonId, preview = false, onBack, onNext }: { token: string; lessonId: number; preview?: boolean; onBack: () => void; onNext?: () => void }) {
  const video = useRef<HTMLVideoElement>(null);
  const { preferences } = useAccessibility();
  const [paused, setPaused] = useState(true);
  const [position, setPosition] = useState(0);
  const [speed, setSpeed] = useState(preferences.playbackSpeed);
  const [lesson, setLesson] = useState<Lesson | null>(null);
  const [playback, setPlayback] = useState<Playback | null>(null);
  const [progress, setProgress] = useState<Progress>({});
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  const saving = useRef(false);
  useEffect(() => { setSpeed(preferences.playbackSpeed); const element = video.current; if (element) { element.playbackRate = preferences.playbackSpeed; element.muted = preferences.muted; for (const track of element.textTracks) track.mode = preferences.captions ? 'showing' : 'disabled'; } }, [preferences.playbackSpeed, preferences.muted, preferences.captions, playback]);
  useEffect(() => {
    let active = true;
    setLesson(null); setPlayback(null); setError(''); setProgress({});
    Promise.all([api<Lesson>(`/capacita/learning/lessons/${lessonId}?manage=${preview}`, token), api<Playback>(`/capacita/learning/lessons/${lessonId}/playback?manage=${preview}`, token, { method: 'POST' })])
      .then(([content, session]) => { if (active) { setLesson(content); setProgress(content.progress || {}); setPlayback(session); } })
      .catch(failure => { if (active) setError(failure.message); });
    return () => { active = false; };
  }, [token, lessonId, preview, retry]);
  useEffect(() => {
    if (!playback || preview) return;
    async function save() {
      const element = video.current;
      if (!element || !playback || saving.current) return;
      saving.current = true;
      try {
        const result = await api<Progress>(`/capacita/learning/lessons/${lessonId}/progress`, token, { method: 'POST', body: JSON.stringify({ sessionId: playback.sessionId, positionSeconds: element.currentTime, playing: !element.paused || element.ended }) });
        setProgress(result);
      } catch (failure) { setError(failure instanceof Error ? failure.message : 'Não foi possível salvar seu progresso.'); }
      finally { saving.current = false; }
    }
    const timer = window.setInterval(() => void save(), 5000);
    const element = video.current;
    element?.addEventListener('ended', save);
    return () => { window.clearInterval(timer); element?.removeEventListener('ended', save); };
  }, [playback, lessonId, token, preview]);
  return <section className="space-y-5 rounded-3xl border border-white/15 bg-[#121c2b] p-5 md:p-8" aria-label={preview ? 'Pré-visualização da aula' : 'Videoaula'}>
    <button className={buttonClass} onClick={onBack}>Voltar ao curso</button>
    {error && <div role="alert"><p className="text-red-200">{error}</p><button className={buttonClass} onClick={() => setRetry(value => value + 1)}>Reabrir reprodução</button></div>}
    {!lesson && !error && <p role="status">Carregando videoaula…</p>}
    {lesson && playback && <><h3 className="text-3xl font-black">{lesson.title}</h3><p className="whitespace-pre-line text-ink/75">{lesson.description}</p>
      <video ref={video} aria-label={`Vídeo da aula ${lesson.title}`} key={playback.sessionId} controls playsInline preload="metadata" crossOrigin="anonymous" className="aspect-video w-full rounded-2xl bg-black" src={`${API_URL}${playback.videoPath}`} onPlay={() => setPaused(false)} onPause={() => setPaused(true)} onTimeUpdate={() => setPosition(video.current?.currentTime || 0)} onLoadedMetadata={() => { if (video.current) { video.current.currentTime = playback.positionSeconds; video.current.playbackRate = speed; video.current.muted = preferences.muted; for (const track of video.current.textTracks) track.mode = preferences.captions ? 'showing' : 'disabled'; } }} onError={() => setError('Não foi possível reproduzir o vídeo. Reabra a reprodução para renovar o acesso.')}>
        {lesson.captions_vtt && <track kind="captions" src={`${API_URL}${playback.captionsPath}`} srcLang="pt-BR" label="Português" default />}
      </video>
      <div role="group" aria-label="Controles de reprodução" className="flex flex-wrap items-center gap-3"><button className={buttonClass} onClick={() => { if (video.current) { if (video.current.paused) void video.current.play().catch(() => setError('Ative a reprodução novamente ou utilize os controles do vídeo.')); else video.current.pause(); } }}>{paused ? 'Reproduzir aula' : 'Pausar aula'}</button><button className={buttonClass} onClick={() => { if (video.current) video.current.currentTime = Math.max(0, video.current.currentTime - 10); }}>Voltar 10 segundos</button><button className={buttonClass} onClick={() => { if (video.current) video.current.currentTime = Math.min(video.current.duration || 0, video.current.currentTime + 10); }}>Avançar 10 segundos</button><label className="text-sm font-bold">Velocidade <select aria-label="Velocidade" value={speed} className="rounded-lg border border-white/20 bg-[#0b1421] p-2 text-ink" onChange={event => { setSpeed(Number(event.target.value)); if (video.current) video.current.playbackRate = Number(event.target.value); }}>{[0.75, 1, 1.25, 1.5, 2].map(value => <option key={value} value={value}>{value}×</option>)}</select></label></div><p className="text-sm">Posição: {Math.floor(position / 60)}min {Math.floor(position % 60)}s · Duração: {Math.floor((lesson.duration_seconds || 0) / 60)}min {Math.round((lesson.duration_seconds || 0) % 60)}s</p>{!preview && <p role="status">{progress.completed_at ? 'Aula concluída' : `${Math.min(100, Math.floor((progress.watched_seconds || 0) / (lesson.duration_seconds || 1) * 100))}% assistido`}</p>}
      {!lesson.captions_vtt && <p className="text-sm text-ink/70">Esta aula ainda não possui legendas fornecidas pelo professor.</p>}{!lesson.transcript && <p className="text-sm text-ink/70">Transcrição ainda não disponível. Você pode solicitar uma alternativa pelo mural ou pelo apoio educacional.</p>}
      {lesson.objectives && <div><h4 className="font-bold">Objetivos de aprendizagem</h4><p className="whitespace-pre-line text-ink/70">{lesson.objectives}</p></div>}
      {lesson.transcript && <details><summary className="cursor-pointer font-bold">Ler transcrição</summary><p className="mt-3 whitespace-pre-line leading-7 text-ink/75">{lesson.transcript}</p></details>}
      {!preview && <p className="text-xs text-ink/60">A conclusão exige assistir a pelo menos 90% da aula com progresso validado pelo servidor. Saltar diretamente ao final não conclui a aula.</p>}
      {onNext && <button className={buttonClass} onClick={onNext}>Próxima aula</button>}
    </>}
  </section>;
}
