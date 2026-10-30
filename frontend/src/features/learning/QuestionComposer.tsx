import { useEffect, useRef, useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { API_URL } from '../account/api';
import { MediaRecorderInput } from './MediaRecorderInput';
import { defaultLimits } from './discussionTypes';
import type { QuestionLimits } from './discussionTypes';
import { buttonClass, inputClass } from './types';
export type QuestionPayload = { clientKey: string; body: string; mediaId: string | null; transcript: string; captionsVtt: string };
export function QuestionComposer({ token, label, contextKey, children, recipients, limits = defaultLimits, onSend }: { token: string; label: string; contextKey: string; children?: ReactNode; recipients?: { id: number; label: string }[]; limits?: QuestionLimits; onSend: (payload: QuestionPayload) => Promise<void> }) {
  const [body, setBody] = useState(''); const [transcript, setTranscript] = useState(''); const [captions, setCaptions] = useState(''); const [file, setFile] = useState<File | null>(null); const [kind, setKind] = useState<'AUDIO' | 'VIDEO'>('AUDIO');
  const [asset, setAsset] = useState<string | null>(null); const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [phase, setPhase] = useState(''); const [percent, setPercent] = useState(0);
  const [reviewing, setReviewing] = useState(false); const [confirmed, setConfirmed] = useState(false); const [success, setSuccess] = useState('');
  const identity = useRef({ fingerprint: '', key: crypto.randomUUID() });
  useEffect(() => { setReviewing(false); setConfirmed(false); }, [contextKey]);
  function changed() { setReviewing(false); setConfirmed(false); setSuccess(''); }
  function upload(video: File) { return new Promise<string>((resolve, reject) => {
    const xhr = new XMLHttpRequest(); xhr.open('POST', `${API_URL}/capacita/learning/discussion/media?kind=${kind}`); xhr.setRequestHeader('Authorization', `Bearer ${token}`); xhr.timeout = 240000;
    xhr.upload.onprogress = event => { if (event.lengthComputable) { const value = Math.round(event.loaded / event.total * 100); setPercent(value); if (value === 100) setPhase('Validando mídia no servidor…'); } };
    xhr.onload = () => { let response: { id?: string; error?: string; detail?: string } = {}; try { response = JSON.parse(xhr.responseText); } catch { /* use safe fallback */ } if (xhr.status >= 200 && xhr.status < 300 && response.id) resolve(response.id); else reject(new Error(response.detail || response.error || 'Não foi possível enviar a mídia.')); };
    xhr.onerror = () => reject(new Error('Falha de conexão no envio. Sua mensagem foi preservada.')); xhr.ontimeout = () => reject(new Error('O envio demorou demais. Tente um arquivo menor.'));
    const form = new FormData(); form.append('file', video); xhr.send(form);
  }); }
  function valid() { if (!body.trim() && !file && !asset) { setError('Escreva uma mensagem ou anexe áudio/vídeo.'); return false; } if (recipients && recipients.length === 0) { setError('Selecione as dúvidas destinatárias.'); return false; } return true; }
  async function submit(event: FormEvent) {
    event.preventDefault(); if (busy || !valid() || recipients && (!reviewing || !confirmed)) return;
    setBusy(true); setError(''); setSuccess('');
    try {
      const fingerprint = JSON.stringify([contextKey, body, transcript, captions, file?.name, file?.size, file?.lastModified]);
      if (identity.current.fingerprint !== fingerprint) identity.current = { fingerprint, key: crypto.randomUUID() };
      let mediaId = asset; if (file && !mediaId) { setPhase('Enviando mídia…'); setPercent(0); mediaId = await upload(file); setAsset(mediaId); }
      setPhase('Salvando mensagem…'); await onSend({ clientKey: identity.current.key, body, mediaId, transcript, captionsVtt: captions });
      setBody(''); setTranscript(''); setCaptions(''); setFile(null); setAsset(null); setReviewing(false); setConfirmed(false); identity.current = { fingerprint: '', key: crypto.randomUUID() }; setSuccess('Envio concluído.');
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Não foi possível enviar.'); }
    finally { setBusy(false); setPhase(''); }
  }
  return <form onSubmit={submit} aria-label={label} className="space-y-4 rounded-2xl border border-brand-blue/25 bg-[#121c2b] p-4 md:p-5">
    <fieldset disabled={busy} className="space-y-4"><legend className="mb-3 text-xl font-bold">{label}</legend><p className="text-sm text-ink/70">Você pode participar somente por texto. Câmera e microfone são opcionais; descreva o conteúdo visual ou sonoro relevante quando anexar uma mídia.</p>{children}
      <label className="block font-bold">Mensagem ou descrição textual<textarea rows={4} maxLength={6000} className={inputClass} value={body} onChange={event => { setBody(event.target.value); changed(); }} /></label>
      <MediaRecorderInput file={file} kind={kind} limits={limits} disabled={busy} onChange={(selected, type) => { setFile(selected); setKind(type); setAsset(null); changed(); }} />
      <details><summary className="cursor-pointer text-sm font-bold">Adicionar transcrição ou legendas (opcional)</summary><p className="my-2 text-xs text-ink/65">Textos fornecidos pelo autor; não são transcrições automáticas verificadas.</p><label className="block font-bold">Transcrição fornecida<textarea rows={3} maxLength={20000} className={inputClass} value={transcript} onChange={event => { setTranscript(event.target.value); changed(); }} /></label><label className="block font-bold">Legendas WEBVTT<textarea rows={3} maxLength={50000} className={inputClass} value={captions} onChange={event => { setCaptions(event.target.value); changed(); }} /></label></details>
      {recipients && !reviewing && <button type="button" className={buttonClass} onClick={() => { setError(''); if (valid()) setReviewing(true); }}>Revisar resposta e destinatários</button>}
      {recipients && reviewing && <section className="space-y-3 rounded-xl border border-brand-blue/30 p-4" aria-label="Revisão do envio em lote"><h4 className="font-bold">Confirmar destinatários</h4><p className="whitespace-pre-line break-words text-sm">{body || 'Resposta com mídia anexada.'}</p><p className="text-xs">{recipients.length} dúvidas receberão uma cópia individual desta resposta. Não inclua dados privados de outros alunos.</p><ul className="max-h-48 overflow-auto text-sm">{recipients.map(item => <li key={item.id} className="py-1">#{item.id} · {item.label}</li>)}</ul><label className="flex gap-2 text-sm"><input type="checkbox" checked={confirmed} onChange={event => setConfirmed(event.target.checked)} />Confirmo a resposta e todas as dúvidas destinatárias.</label><button type="button" className={buttonClass} onClick={() => { setReviewing(false); setConfirmed(false); }}>Voltar à edição</button></section>}
      {(!recipients || reviewing) && <button className={buttonClass} disabled={busy || !!recipients && !confirmed}>{busy ? 'Enviando…' : label}</button>}
    </fieldset>
    {busy && <div aria-live="polite"><p>{phase}</p>{file && <progress max={100} value={percent} aria-label="Progresso do envio da mídia" className="w-full" />}</div>}
    {error && <p role="alert" className="text-sm text-red-200">{error}</p>}{success && <p role="status" className="text-sm text-brand-blue">{success}</p>}
  </form>;
}
