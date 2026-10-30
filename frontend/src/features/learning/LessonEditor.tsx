import { useErrorFocus } from './accessibleFocus';
import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { API_URL, api } from '../account/api';
import type { Lesson } from './types';
import { buttonClass, inputClass } from './types';
export function LessonEditor({ token, moduleId, lessonId, onSaved, onCancel }: { token: string; moduleId: number; lessonId?: number; onSaved: () => void; onCancel: () => void }) {
  const [form, setForm] = useState({ title: '', description: '', objectives: '', transcript: '', captionsVtt: '', sortOrder: 0, status: 'DRAFT' });
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState('');
  const [error, setError] = useState('');
  const errorFocus = useErrorFocus(error);
  const [busy, setBusy] = useState(false);
  const [percent, setPercent] = useState(0);
  const [phase, setPhase] = useState('');
  const [currentId, setCurrentId] = useState(lessonId);
  useEffect(() => { if (lessonId) api<Lesson>(`/capacita/learning/lessons/${lessonId}?manage=true`, token).then(lesson => setForm({ title: lesson.title, description: lesson.description, objectives: lesson.objectives, transcript: lesson.transcript || '', captionsVtt: lesson.captions_vtt || '', sortOrder: lesson.sort_order, status: lesson.status })).catch(failure => setError(failure.message)); }, [lessonId, token]);
  useEffect(() => { if (!file) { setPreview(''); return; } const url = URL.createObjectURL(file); setPreview(url); return () => URL.revokeObjectURL(url); }, [file]);
  function upload(id: number, video: File) {
    return new Promise<void>((resolve, reject) => {
      const xhr = new XMLHttpRequest(); xhr.open('POST', `${API_URL}/capacita/learning/lessons/${id}/video`); xhr.setRequestHeader('Authorization', `Bearer ${token}`); xhr.timeout = 300000;
      xhr.upload.onprogress = event => { if (event.lengthComputable) { const value = Math.round(event.loaded / event.total * 100); setPercent(value); if (value === 100) setPhase('Validando e preparando o vídeo no servidor…'); } };
      xhr.onload = () => { if (xhr.status >= 200 && xhr.status < 300) resolve(); else { let message = 'Não foi possível enviar o vídeo.'; try { const body = JSON.parse(xhr.responseText); message = body.detail || body.error || message; } catch { /* Use the accessible fallback. */ } reject(new Error(message)); } };
      xhr.onerror = () => reject(new Error('Falha de conexão durante o upload. O rascunho foi preservado.'));
      xhr.ontimeout = () => reject(new Error('O envio demorou demais. Tente um vídeo menor.'));
      const data = new FormData(); data.append('file', video); xhr.send(data);
    });
  }
  async function save(event: FormEvent) {
    event.preventDefault(); if (busy) return; setBusy(true); setError(''); setPercent(0);
    try {
      let id = currentId;
      if (!id) { const created = await api<{ id: number }>(`/capacita/learning/modules/${moduleId}/lessons`, token, { method: 'POST', body: JSON.stringify({ ...form, status: 'DRAFT' }) }); id = created.id; setCurrentId(id); }
      setPhase('Salvando rascunho…');
      await api(`/capacita/learning/lessons/${id}`, token, { method: 'PUT', body: JSON.stringify({ ...form, status: 'DRAFT' }) });
      if (file) { setPhase('Enviando vídeo…'); await upload(id, file); setFile(null); }
      await api(`/capacita/learning/lessons/${id}`, token, { method: 'PUT', body: JSON.stringify(form) });
      onSaved();
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Não foi possível salvar a aula.'); }
    finally { setBusy(false); setPhase(''); }
  }
  return <form onSubmit={save} className="space-y-4 rounded-3xl border border-brand-blue/30 bg-[#121c2b] p-5" aria-label="Publicação de videoaula">
    <h3 className="text-2xl font-black">{currentId ? 'Editar videoaula' : 'Nova videoaula'}</h3><p className="text-sm text-ink/70">Inclua legendas revisadas e transcrição, identificando falantes e sons relevantes. Descreva no texto imagens, gráficos e demonstrações importantes. Esses recursos só serão indicados como disponíveis quando forem fornecidos.</p>
    <label className="block font-bold">Título da aula<input required maxLength={180} value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} className={inputClass} /></label>
    {(['description', 'objectives', 'transcript', 'captionsVtt'] as const).map(field => <label key={field} className="block font-bold">{{ description: 'Descrição da aula', objectives: 'Objetivos de aprendizagem', transcript: 'Transcrição (opcional)', captionsVtt: 'Legendas WEBVTT (opcional)' }[field]}<textarea maxLength={field === 'transcript' || field === 'captionsVtt' ? 100000 : 4000} rows={3} value={form[field]} onChange={event => setForm({ ...form, [field]: event.target.value })} className={inputClass} /></label>)}
    <label className="block font-bold">Ordem da aula<input type="number" min={0} required value={form.sortOrder} onChange={event => setForm({ ...form, sortOrder: Number(event.target.value) })} className={inputClass} /></label>
    <label className="block font-bold">Vídeo MP4, MOV ou WebM<input type="file" accept=".mp4,.mov,.webm,video/mp4,video/quicktime,video/webm" disabled={busy} onChange={event => { const selected = event.target.files?.[0] || null; if (selected && selected.size > 128 * 1024 * 1024) { setError('Envie um arquivo de até 128 MB.'); return; } setFile(selected); setError(''); }} className={inputClass} /></label>
    <p className="text-sm text-ink/65">A duração real deve ser de 3 a 5 minutos. O servidor valida o arquivo antes de permitir a publicação. Substituir o vídeo devolve a aula a rascunho e reinicia seu progresso.</p>
    {preview && <video controls playsInline preload="metadata" src={preview} className="aspect-video w-full rounded-xl bg-black" aria-label="Prévia do arquivo selecionado" />}
    <label className="block font-bold">Situação<select value={form.status} onChange={event => setForm({ ...form, status: event.target.value })} className={inputClass}><option value="DRAFT">Rascunho</option><option value="PUBLISHED">Publicar após validação</option><option value="HIDDEN">Oculta</option></select></label>
    {busy && <div role="status"><p>{phase}</p>{file && <><progress max={100} value={percent} aria-label="Progresso do upload" className="w-full" /><p>{percent}% enviado</p></>}</div>}
    {error && <p ref={errorFocus} tabIndex={-1} role="alert" className="text-red-200">{error}</p>}
    <div className="flex gap-3"><button disabled={busy} className={buttonClass}>{busy ? 'Processando…' : 'Salvar aula'}</button><button type="button" disabled={busy} onClick={onCancel} className={buttonClass}>Voltar sem salvar</button></div>
  </form>;
}
