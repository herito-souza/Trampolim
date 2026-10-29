import { AccountAudit } from './AccountAudit';
import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from './api';
type Account = { id: number; email: string; name: string; status: string; emailVerified: boolean; createdAt: string };
export function TrailAccountAdmin({ token }: { token: string }) {
  const [auditAccount, setAuditAccount] = useState<Account | null>(null);
  const [trail, setTrail] = useState<'empresas' | 'capacita'>('empresas');
  const [offset, setOffset] = useState(0);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState<Account | null>(null);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let active = true;
    setLoading(true); setError(''); setSelected(null); setAuditAccount(null); setReason('');
    api<Account[]>(`/admin/trail-accounts/${trail}?offset=${offset}&limit=50`, token)
      .then(rows => { if (active) setAccounts(rows); })
      .catch(cause => { if (active) { setAccounts([]); setError(cause instanceof Error ? cause.message : 'Não foi possível consultar as contas.'); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [trail, offset, token, revision]);
  async function changeStatus(event: FormEvent) {
    event.preventDefault();
    if (!selected || busy || !reason.trim()) return;
    setBusy(true); setError('');
    try {
      await api<void>(`/admin/trail-accounts/${trail}/${selected.id}/status`, token, { method: 'PUT', body: JSON.stringify({ status: selected.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED', reason: reason.trim() }) });
      setRevision(value => value + 1);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível alterar a conta.'); }
    finally { setBusy(false); }
  }
  return <section className="rounded-2xl border border-white/10 p-5"><h2 className="text-2xl font-black">Contas por trilha</h2><label className="mt-4 block text-sm font-bold">Trilha administrada<select disabled={busy} value={trail} onChange={event => { setTrail(event.target.value as typeof trail); setOffset(0); }} className="ml-3 rounded-xl border border-white/20 bg-[#121c2b] p-3"><option value="empresas">Empresas</option><option value="capacita">Capacita</option></select></label>{error && <p role="alert" className="mt-4">{error}</p>}{loading ? <p role="status" className="mt-4">Carregando contas…</p> : <ul className="mt-4 divide-y divide-white/10">{accounts.map(account => <li key={account.id} className="flex flex-wrap items-center justify-between gap-3 py-4"><span><strong>{account.name}</strong> · {account.email} · {account.status}</span><button disabled={busy} type="button" onClick={() => { setSelected(account); setReason(''); }} className="rounded-full border border-white/20 px-4 py-2">{account.status === 'SUSPENDED' ? 'Reativar' : 'Suspender'}</button><button type="button" disabled={busy} onClick={() => setAuditAccount(account)} className="rounded-full border border-white/20 px-4 py-2">Histórico</button></li>)}{!accounts.length && <li className="py-4">Nenhuma conta nesta página.</li>}</ul>}{auditAccount && <AccountAudit key={`${trail}-${auditAccount.id}`} token={token} trail={trail} userId={auditAccount.id} email={auditAccount.email} onClose={() => setAuditAccount(null)} />}{selected && <form onSubmit={changeStatus} className="mt-4 rounded-xl border border-white/20 p-4"><p className="font-bold">Confirmar alteração de {selected.email}</p><p className="mt-2 text-sm">A alteração será registrada e encerrará as sessões desta conta.</p><label className="mt-3 block">Justificativa<textarea required maxLength={500} value={reason} onChange={event => setReason(event.target.value)} className="mt-2 block w-full rounded-xl border border-white/20 bg-transparent p-3" /></label><button disabled={busy || !reason.trim()} className="mt-3 rounded-full bg-brand-yellow px-5 py-3 font-bold text-[#101725]">Confirmar alteração</button><button type="button" disabled={busy} onClick={() => setSelected(null)} className="ml-3 px-4 py-3">Cancelar</button></form>}<div className="mt-4 flex gap-4"><button disabled={loading || busy || offset===0} onClick={() => setOffset(value => Math.max(0,value-50))}>Anterior</button><button disabled={loading || busy || accounts.length<50} onClick={() => setOffset(value => value+50)}>Próxima</button></div></section>;
}
