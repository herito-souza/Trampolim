import { useEffect, useState } from 'react';
import { api } from './api';
type Entry = { id: number; userId: number; actor: string; previousStatus: string; newStatus: string; reason: string; createdAt: string };
export function AccountAudit({ token, trail, userId, email, onClose }: { token: string; trail: 'empresas' | 'capacita'; userId: number; email: string; onClose: () => void }) {
  const [offset, setOffset] = useState(0);
  const [rows, setRows] = useState<Entry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    let active = true;
    setLoading(true); setError('');
    api<Entry[]>(`/admin/trail-accounts/${trail}/${userId}/audit?offset=${offset}&limit=20`, token)
      .then(data => { if (active) setRows(data); })
      .catch(cause => { if (active) { setRows([]); setError(cause instanceof Error ? cause.message : 'Não foi possível consultar o histórico.'); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [trail, userId, token, offset]);
  return <section aria-label="Histórico da conta" className="mt-5 rounded-xl border border-white/20 p-4"><h3 className="text-xl font-bold">Histórico de {email}</h3><p className="mt-1 text-sm">Trilha: {trail}</p><button type="button" onClick={onClose} className="mt-3 rounded-full border border-white/20 px-4 py-2">Fechar histórico</button>{loading ? <p role="status">Carregando histórico…</p> : error ? <p role="alert">{error}</p> : <ul className="mt-4 divide-y divide-white/10">{rows.map(row => <li key={row.id} className="py-3"><p><time dateTime={row.createdAt}>{new Date(row.createdAt).toLocaleString('pt-BR')}</time> · {row.actor}</p><p>{row.previousStatus} → {row.newStatus}</p><p className="mt-1 text-sm">{row.reason}</p></li>)}{!rows.length && <li>Nenhuma alteração registrada nesta página.</li>}</ul>}<div className="mt-3 flex gap-4"><button disabled={loading || offset===0} onClick={() => setOffset(value => Math.max(0,value-20))}>Histórico anterior</button><button disabled={loading || rows.length<20} onClick={() => setOffset(value => value+20)}>Mais registros</button></div></section>;
}
