import { InlineNotice, LoadingStatus } from '../../components/ui/Feedback';
import { useEffect, useRef, useState } from 'react';
import { API_URL, api } from '../account/api';
import type { Lesson, Progress } from './types';
import { buttonClass } from './types';
import { useAccessibility } from './accessibilityContext';
type Playback = { sessionId: string; videoPath: string; captionsPath: string; positionSeconds: number };
export function LessonPlayer({ token, lessonId, preview = false, onBack, onNext }: { token: string; lessonId: number; preview?: boolean; onBack: () => void; onNext?: () => void }) {
  const video = useRef<HTMLVideoElement>(null);
  const { preferences } = useAccessibility();
  const [started,setStarted]=useState(false),[buffering,setBuffering]=useState(false),[offline,setOffline]=useState(!navigator.onLine),[saveError,setSaveError]=useState(''),[progressRetry,setProgressRetry]=useState(0);
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
    const abort=new AbortController();setStarted(false);setPaused(true);setBuffering(false);setSaveError('');setProgressRetry(0);
    setLesson(null); setPlayback(null); setError(''); setProgress({});
    Promise.all([api<Lesson>(`/capacita/learning/lessons/${lessonId}?manage=${preview}`, token,{signal:abort.signal}), api<Playback>(`/capacita/learning/lessons/${lessonId}/playback?manage=${preview}`, token, { method: 'POST',signal:abort.signal })])
      .then(([content, session]) => { if (active) { setLesson(content); setProgress(content.progress || {}); setPosition(session.positionSeconds);setPlayback(session); } })
      .catch(failure => { if (active) setError(failure.message); });
    return () => { active = false;abort.abort(); };
  }, [token, lessonId, preview, retry]);
  useEffect(()=>{const online=()=>setOffline(false),lost=()=>setOffline(true);window.addEventListener('online',online);window.addEventListener('offline',lost);return()=>{window.removeEventListener('online',online);window.removeEventListener('offline',lost)}},[]);
  useEffect(() => {
    if (!playback || preview || !started) return;
    let active=true;const abort=new AbortController();let lastSent='';
    async function save() {
      const element=video.current;if(!element||!playback||saving.current)return;
      if(!navigator.onLine){if(active)setSaveError('Sem conexão. Sua posição continua nesta reprodução; tente salvar quando a conexão voltar. Somente o progresso confirmado pelo servidor vale para conclusão.');return;}
      const stamp=element.currentTime.toFixed(1)+':'+String(element.paused);if(stamp===lastSent&&!element.ended)return;
      saving.current=true;
      try{
        const result=await api<Progress>(`/capacita/learning/lessons/${lessonId}/progress`,token,{method:'POST',signal:abort.signal,body:JSON.stringify({sessionId:playback.sessionId,positionSeconds:element.currentTime,playing:!element.paused||element.ended})});
        if(active){lastSent=stamp;setProgress(result);setSaveError('');}
      }catch{if(active&&!abort.signal.aborted)setSaveError('Não foi possível confirmar o progresso. Sua posição continua nesta reprodução. Tente salvar novamente; não é necessário reabrir o vídeo.');}
      finally{saving.current=false;}
    }
    const timer=window.setInterval(()=>void save(),5000);const element=video.current;const persist=()=>void save();const hidden=()=>{if(document.visibilityState==='hidden')persist()};
    element?.addEventListener('ended',persist);element?.addEventListener('pause',persist);window.addEventListener('online',persist);document.addEventListener('visibilitychange',hidden);if(progressRetry)persist();
    return()=>{active=false;window.clearInterval(timer);element?.removeEventListener('ended',persist);element?.removeEventListener('pause',persist);window.removeEventListener('online',persist);document.removeEventListener('visibilitychange',hidden);abort.abort()};
  },[playback,lessonId,token,preview,started,progressRetry]);
  return <section className="space-y-5 rounded-3xl border border-white/15 bg-[#121c2b] p-5 md:p-8" aria-label={preview ? 'Pré-visualização da aula' : 'Videoaula'}>
    <button className={buttonClass} onClick={onBack}>Voltar ao curso</button>
    {error&&<InlineNotice error onRetry={()=>setRetry(value=>value+1)} retryLabel="Reabrir reprodução">{error}</InlineNotice>}
    {offline&&<InlineNotice>Você está sem conexão. O conteúdo já carregado pode continuar disponível; novos dados e confirmações dependem da conexão.</InlineNotice>}
    {saveError&&<InlineNotice error onRetry={()=>setProgressRetry(value=>value+1)} retryLabel="Tentar salvar progresso">{saveError}</InlineNotice>}
    {buffering&&!paused&&<InlineNotice>O vídeo está aguardando dados. Você pode pausar para carregar e consultar a transcrição enquanto isso.</InlineNotice>}
    {!lesson&&!error&&<LoadingStatus label="Carregando videoaula…"/>}
    {lesson && playback && <><h3 className="text-3xl font-black">{lesson.title}</h3><p className="whitespace-pre-line text-ink/75">{lesson.description}</p>
      <video ref={video} aria-label={`Vídeo da aula ${lesson.title}`} key={playback.sessionId} controls playsInline preload="none" crossOrigin="anonymous" className="aspect-video w-full rounded-2xl bg-black" src={`${API_URL}${playback.videoPath}`} onPlay={() => {setPaused(false);setStarted(true)}} onWaiting={()=>setBuffering(true)} onStalled={()=>setBuffering(true)} onPlaying={()=>setBuffering(false)} onCanPlay={()=>setBuffering(false)} onPause={() => {setPaused(true);setBuffering(false)}} onTimeUpdate={() => setPosition(video.current?.currentTime || 0)} onLoadedMetadata={() => { if (video.current) { video.current.currentTime = playback.positionSeconds; video.current.playbackRate = speed; video.current.muted = preferences.muted; for (const track of video.current.textTracks) track.mode = preferences.captions ? 'showing' : 'disabled'; } }} onError={() => setError('Não foi possível reproduzir o vídeo. Reabra a reprodução para renovar o acesso.')}>
        {lesson.captions_vtt && <track kind="captions" src={`${API_URL}${playback.captionsPath}`} srcLang="pt-BR" label="Português" default />}
      </video>
      {!started&&<p className="text-sm text-ink/80">O vídeo só começa a carregar ao acionar a reprodução. Legendas e transcrição, quando fornecidas, continuam disponíveis.</p>}
      <div role="group" aria-label="Controles de reprodução" className="flex flex-wrap items-center gap-3"><button className={buttonClass} onClick={() => { if (video.current) { if (video.current.paused) void video.current.play().catch(() => setError('Ative a reprodução novamente ou utilize os controles do vídeo.')); else video.current.pause(); } }}>{paused ? 'Reproduzir aula' : 'Pausar aula'}</button><button className={buttonClass} onClick={() => { if (video.current) video.current.currentTime = Math.max(0, video.current.currentTime - 10); }}>Voltar 10 segundos</button><button className={buttonClass} onClick={() => { if (video.current) video.current.currentTime = Math.min(video.current.duration || 0, video.current.currentTime + 10); }}>Avançar 10 segundos</button><label className="text-sm font-bold">Velocidade <select aria-label="Velocidade" value={speed} className="rounded-lg border border-white/20 bg-[#0b1421] p-2 text-ink" onChange={event => { setSpeed(Number(event.target.value)); if (video.current) video.current.playbackRate = Number(event.target.value); }}>{[0.75, 1, 1.25, 1.5, 2].map(value => <option key={value} value={value}>{value}×</option>)}</select></label></div><p className="text-sm">Posição: {Math.floor(position / 60)}min {Math.floor(position % 60)}s · Duração: {Math.floor((lesson.duration_seconds || 0) / 60)}min {Math.round((lesson.duration_seconds || 0) % 60)}s</p>{!preview && <p role="status">{progress.completed_at ? 'Aula concluída' : `${Math.min(100, Math.floor((progress.watched_seconds || 0) / (lesson.duration_seconds || 1) * 100))}% assistido`}</p>}
      {!lesson.captions_vtt && <p className="text-sm text-ink/70">Esta aula ainda não possui legendas fornecidas pelo professor.</p>}{!lesson.transcript && <p className="text-sm text-ink/70">Transcrição ainda não disponível. Você pode solicitar uma alternativa pelo mural ou pelo apoio educacional.</p>}
      {lesson.objectives && <div><h4 className="font-bold">Objetivos de aprendizagem</h4><p className="whitespace-pre-line text-ink/70">{lesson.objectives}</p></div>}
      {lesson.transcript && <details><summary className="cursor-pointer font-bold">Ler transcrição</summary><p className="mt-3 whitespace-pre-line leading-7 text-ink/75">{lesson.transcript}</p></details>}
      {!preview && <p className="text-xs text-ink/60">A conclusão exige assistir a pelo menos 90% da aula com progresso validado pelo servidor. Saltar diretamente ao final não conclui a aula.</p>}
      {onNext && <button className={buttonClass} onClick={onNext}>Próxima aula</button>}
    </>}
  </section>;
}
