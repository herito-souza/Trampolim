import { useEffect, useRef, useState } from 'react';
import { radarButton as button } from '../radar/radarUi';
/** Optional local rehearsal: no upload, speech analysis or automatic transcript. */
export function InterviewAudioPractice() { const recorder = useRef<MediaRecorder | null>(null), stream = useRef<MediaStream | null>(null), timer = useRef<ReturnType<typeof setTimeout> | null>(null), urlRef = useRef(''), mounted = useRef(true); const [url, setUrl] = useState(''), [recording, setRecording] = useState(false), [error, setError] = useState(''); useEffect(() => { mounted.current = true; return () => { mounted.current = false; if (timer.current)
    clearTimeout(timer.current); if (recorder.current?.state === 'recording')
    recorder.current.stop(); stream.current?.getTracks().forEach(t => t.stop()); if (urlRef.current)
    URL.revokeObjectURL(urlRef.current); }; }, []); async function start() { setError(''); try {
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined')
        throw new Error('Gravação indisponível neste navegador. Você pode responder por texto.');
    const media = await navigator.mediaDevices.getUserMedia({ audio: true });
    if (!mounted.current) {
        media.getTracks().forEach(t => t.stop());
        return;
    }
    stream.current = media;
    const chunks: BlobPart[] = [];
    const rec = new MediaRecorder(media);
    recorder.current = rec;
    rec.ondataavailable = e => { if (e.data.size)
        chunks.push(e.data); };
    rec.onstop = () => { media.getTracks().forEach(t => t.stop()); if (timer.current)
        clearTimeout(timer.current); if (!mounted.current)
        return; if (urlRef.current)
        URL.revokeObjectURL(urlRef.current); urlRef.current = URL.createObjectURL(new Blob(chunks, { type: rec.mimeType })); setUrl(urlRef.current); setRecording(false); };
    rec.start(1000);
    setRecording(true);
    timer.current = setTimeout(() => { if (rec.state === 'recording')
        rec.stop(); }, 120000);
}
catch (e) {
    stream.current?.getTracks().forEach(t => t.stop());
    setError(e instanceof Error ? e.message : 'Não foi possível acessar o microfone. Use a resposta textual.');
} } return <section aria-label="Prática de áudio local" className="space-y-3 rounded-xl border border-white/15 p-4"><h3 className="font-bold">Ensaiar por áudio (opcional)</h3><p>A gravação dura até dois minutos e fica somente nesta página. Ouça seu ensaio e escreva ou transcreva a resposta no campo textual para obter feedback. Não há transcrição automática nem análise biométrica.</p><button className={button} disabled={recording} onClick={start}>Autorizar microfone e gravar ensaio</button> <button className={button} disabled={!recording} onClick={() => { if (recorder.current?.state === 'recording')
    recorder.current.stop(); }}>Parar gravação</button>{recording && <p role="status">Gravando ensaio local…</p>}{url && <><audio controls src={url} aria-label="Ouvir meu ensaio local" className="max-w-full"/><button className={button} onClick={() => { URL.revokeObjectURL(urlRef.current); urlRef.current = ''; setUrl(''); }}>Apagar ensaio local</button></>}{error && <p role="alert">{error}</p>}</section>; }
