import { useCallback, useEffect, useRef, useState } from 'react';
import { API_URL, api } from '../account/api';
import { buttonClass } from './types';
type Notice = { id: number; question_id: number; kind: string; read_at: string | null; created_at: string };
const root = '/capacita/learning/notifications';
export const questionEvent = 'capacita-question-notifications';
export function QuestionNotifications({ token, onOpen }: { token: string; onOpen: (id: number, inbox: boolean) => void }) {
  const [items, setItems] = useState<Notice[]>([]); const [unread, setUnread] = useState(0); const [open, setOpen] = useState(false); const [connection, setConnection] = useState('Conectando…'); const [more, setMore] = useState(false);
  const cursor = useRef(0); const current = useRef(true);
  const load = useCallback(async (before?: number) => { const data = await api<{ items?: Notice[]; unreadCount?: number }>(`${root}${before ? `?before=${before}` : ''}`, token); const rows = Array.isArray(data.items) ? data.items : []; if (current.current) { setItems(previous => [...new Map([...previous, ...rows].map(item => [item.id, item])).values()].sort((a, b) => b.id - a.id)); setUnread(data.unreadCount || 0); setMore(rows.length === 50); } return rows; }, [token]);
  useEffect(() => {
    current.current = true; cursor.current = 0; setItems([]); setUnread(0); const abort = new AbortController();
    async function connect() {
      try { const initial = await load(); cursor.current = initial.reduce((max, item) => Math.max(max, item.id), 0); } catch { /* Persisted history is retried after reconnection. */ }
      let delay = 1000;
      while (!abort.signal.aborted) {
        try {
          const response = await fetch(`${API_URL}${root}/stream?after=${cursor.current}`, { headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' }, signal: abort.signal });
          if (response.status === 401 || response.status === 403) { setConnection('Entre novamente para atualizar as notificações.'); break; }
          if (!response.ok || !response.headers.get('content-type')?.includes('text/event-stream') || !response.body) throw new Error('Stream indisponível');
          setConnection('Atualização ao vivo conectada'); delay = 1000; const reader = response.body.getReader(); const decoder = new TextDecoder(); let buffer = '';
          while (!abort.signal.aborted) {
            const chunk = await reader.read(); if (chunk.done) break; buffer += decoder.decode(chunk.value, { stream: true });
            const events = buffer.split(/\r?\n\r?\n/); buffer = events.pop() || '';
            for (const event of events) { const lines = event.split(/\r?\n/); const name = lines.find(line => line.startsWith('event:'))?.slice(6).trim(); const id = Number(lines.find(line => line.startsWith('id:'))?.slice(3).trim() || 0); if (name === 'notifications') { const data = JSON.parse(lines.filter(line => line.startsWith('data:')).map(line => line.slice(5).trim()).join('\n')); if (Number.isSafeInteger(id)) cursor.current = Math.max(cursor.current, id); void load().catch(() => undefined); window.dispatchEvent(new CustomEvent(questionEvent, { detail: (data.items || []).map((item: Notice) => item.question_id) })); } }
          }
        } catch { if (abort.signal.aborted) break; }
        if (abort.signal.aborted) break; setConnection('Reconectando… histórico preservado.');
        await new Promise<void>(resolve => { const stop = () => { clearTimeout(timer); resolve(); }; const timer = window.setTimeout(() => { abort.signal.removeEventListener('abort', stop); resolve(); }, delay); abort.signal.addEventListener('abort', stop, { once: true }); }); delay = Math.min(delay * 2, 30000);
      }
    }
    void connect(); return () => { current.current = false; abort.abort(); };
  }, [token, load]);
  async function read(item: Notice) { onOpen(item.question_id, item.kind === 'QUESTION' || item.kind === 'FOLLOW_UP'); try { await api(`${root}/${item.id}/read`, token, { method: 'PUT' }); setItems(previous => previous.map(row => row.id === item.id ? { ...row, read_at: new Date().toISOString() } : row)); await load(); } catch { setConnection('Não foi possível marcar como lida. Tente novamente.'); } }
  return <section className="rounded-xl border border-white/15 p-3" aria-label="Notificações do Capacita"><div className="flex flex-wrap items-center gap-3"><button className={buttonClass} aria-expanded={open} onClick={() => { setOpen(!open); void load().catch(() => setConnection('Histórico temporariamente indisponível.')); }}>Notificações ({unread})</button><span className="sr-only" role="status" aria-atomic="true">{unread} notificações não lidas.</span><span className="text-xs text-ink/60" aria-live="polite">{connection}</span></div>{open && <div className="mt-3 space-y-3"><ul className="max-h-80 overflow-auto">{items.map(item => <li key={item.id} className="border-b border-white/10 py-2"><button className="w-full text-left text-sm" onClick={() => void read(item)}><strong>{item.read_at ? '' : 'Não lida · '}{({ QUESTION: 'Nova dúvida recebida', FOLLOW_UP: 'Nova mensagem do aluno', REPLY: 'Resposta recebida', STATUS: 'Atendimento atualizado' } as Record<string, string>)[item.kind]}</strong><span className="block text-xs text-ink/60">Dúvida #{item.question_id} · {new Date(item.created_at).toLocaleString('pt-BR')}</span></button></li>)}</ul>{items.length === 0 && <p className="text-sm text-ink/65">Nenhuma notificação registrada.</p>}{more && <button className={buttonClass} onClick={() => void load(items[items.length - 1]?.id).catch(() => setConnection('Não foi possível carregar o histórico.'))}>Carregar notificações anteriores</button>}</div>}</section>;
}
