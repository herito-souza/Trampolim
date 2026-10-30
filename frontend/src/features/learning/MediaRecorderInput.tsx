import { useCallback, useEffect, useRef, useState } from 'react';
import { buttonClass, inputClass } from './types';
import type { QuestionLimits } from './discussionTypes';
export function MediaRecorderInput({ file, kind, onChange, limits, disabled }: { file: File | null; kind: 'AUDIO' | 'VIDEO'; onChange: (file: File | null, kind: 'AUDIO' | 'VIDEO') => void; limits: QuestionLimits; disabled: boolean }) {
  const recorder = useRef<MediaRecorder | null>(null); const stream = useRef<MediaStream | null>(null); const previewElement = useRef<HTMLVideoElement>(null);
  const [recording, setRecording] = useState(false); const [requesting, setRequesting] = useState(false); const [seconds, setSeconds] = useState(0); const [error, setError] = useState(''); const [preview, setPreview] = useState('');
  const active = useRef(true); const requested = useRef(0);
  const maxSeconds = kind === 'AUDIO' ? limits.audioMaxSeconds : limits.videoMaxSeconds;
  const dispose = useCallback(() => { active.current = false; requested.current++; if (recorder.current?.state === 'recording') recorder.current.stop(); stream.current?.getTracks().forEach(track => track.stop()); }, []);
  useEffect(() => { active.current = true; return dispose; }, [dispose]);
  useEffect(() => { if (!file) { setPreview(''); return; } const url = URL.createObjectURL(file); setPreview(url); return () => URL.revokeObjectURL(url); }, [file]);
  useEffect(() => { if (!recording) return; const timer = window.setInterval(() => setSeconds(value => value + 1), 1000); const stop = window.setTimeout(() => recorder.current?.state === 'recording' && recorder.current.stop(), Math.max(1, maxSeconds - 1) * 1000); return () => { clearInterval(timer); clearTimeout(stop); }; }, [recording, maxSeconds]);
  function choose(selected: File | null) { setError(''); if (selected && selected.size > limits.maxBytes) { setError('Arquivo acima do limite permitido.'); return; } onChange(selected, kind); }
  async function start() {
    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) { setError('A gravação não está disponível neste navegador. Use o envio de arquivo ou escreva sua mensagem.'); return; }
    const request = ++requested.current; setRequesting(true); setError('');
    try {
      const media = await navigator.mediaDevices.getUserMedia({ audio: true, video: kind === 'VIDEO' ? { width: { ideal: 1280 }, height: { ideal: 720 } } : false });
      if (!active.current || request !== requested.current) { media.getTracks().forEach(track => track.stop()); return; }
      stream.current = media;
      const formats = kind === 'VIDEO' ? ['video/webm;codecs=vp8,opus', 'video/webm', 'video/mp4'] : ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4', 'audio/ogg'];
      const mimeType = formats.find(type => MediaRecorder.isTypeSupported(type)); const device = new MediaRecorder(media, mimeType ? { mimeType } : undefined); recorder.current = device;
      const chunks: Blob[] = []; let total = 0; let tooLarge = false;
      device.ondataavailable = event => { if (event.data.size) { chunks.push(event.data); total += event.data.size; if (total > limits.maxBytes) { tooLarge = true; if (device.state === 'recording') device.stop(); } } };
      device.onstop = () => {
        media.getTracks().forEach(track => track.stop()); stream.current = null;
        if (!active.current) return; setRecording(false);
        if (tooLarge) { setError('A gravação atingiu o limite de tamanho. Grave uma mensagem mais curta.'); return; }
        const type = device.mimeType || mimeType || (kind === 'VIDEO' ? 'video/webm' : 'audio/webm'); const extension = type.includes('mp4') ? kind === 'AUDIO' ? 'm4a' : 'mp4' : type.includes('ogg') ? 'ogg' : 'webm';
        onChange(new File(chunks, `gravacao-${Date.now()}.${extension}`, { type }), kind);
      };
      device.onerror = () => { if (active.current) setError('A gravação falhou. Você pode enviar um arquivo ou escrever.'); media.getTracks().forEach(track => track.stop()); };
      onChange(null, kind); setSeconds(0); setRecording(true); device.start(1000);
      window.setTimeout(() => { if (previewElement.current) previewElement.current.srcObject = media; }, 0);
    } catch { if (active.current && request === requested.current) setError('Não foi possível acessar microfone/câmera. Verifique a permissão ou use texto/envio de arquivo.'); }
    finally { if (active.current && request === requested.current) setRequesting(false); }
  }
  return <fieldset className="space-y-3 rounded-xl border border-white/15 p-4" disabled={disabled}>
    <legend className="px-2 font-bold">Áudio ou vídeo (opcional)</legend>
    <label className="block text-sm font-bold">Tipo de gravação<select disabled={recording || requesting} value={kind} onChange={event => onChange(null, event.target.value as 'AUDIO' | 'VIDEO')} className={inputClass}><option value="AUDIO">Áudio</option><option value="VIDEO">Vídeo</option></select></label>
    <p className="text-xs text-ink/65">Até {maxSeconds} segundos e {Math.floor(limits.maxBytes / 1024 / 1024)} MB. Texto continua disponível sem microfone ou câmera. A gravação requer HTTPS ou localhost.</p>
    <div className="flex flex-wrap gap-2">{recording ? <button type="button" className={buttonClass} onClick={() => recorder.current?.stop()}>Parar gravação ({seconds}s)</button> : <button type="button" disabled={requesting} className={buttonClass} onClick={() => void start()}>{requesting ? 'Aguardando permissão…' : kind === 'AUDIO' ? 'Gravar áudio' : 'Gravar vídeo'}</button>}{requesting && <button type="button" className={buttonClass} onClick={() => { requested.current++; setRequesting(false); }}>Cancelar solicitação</button>}</div>
    {recording && kind === 'VIDEO' && <video ref={previewElement} muted autoPlay playsInline className="aspect-video w-full rounded-lg bg-black" aria-label="Prévia da câmera" />}
    <label className="block text-sm font-bold">Ou enviar arquivo<input disabled={recording || requesting} type="file" accept={kind === 'AUDIO' ? 'audio/*,.webm,.m4a' : 'video/*,.mp4,.mov,.webm'} onChange={event => choose(event.target.files?.[0] || null)} className={inputClass} /></label>
    {file && <div className="space-y-2"><p className="break-all text-xs">{file.name}</p>{preview && (kind === 'VIDEO' ? <video controls playsInline src={preview} className="aspect-video w-full rounded-lg bg-black" aria-label="Prévia do vídeo da mensagem" /> : <audio controls src={preview} className="w-full" aria-label="Prévia do áudio da mensagem" />)}<button type="button" className={buttonClass} onClick={() => onChange(null, kind)}>Remover anexo</button></div>}
    {error && <p role="alert" className="text-sm text-red-200">{error}</p>}
  </fieldset>;
}
