import { useState } from 'react';
import { api } from './api';

export function CompanyAccountDeletion({ token, onDeleted }: { token: string; onDeleted: () => void }) {
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [confirmed, setConfirmed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!confirmed || !password || busy) return;
    setBusy(true); setError('');
    try {
      await api('/empresas/account', token, { method: 'DELETE', body: JSON.stringify({ password }) });
      setPassword(''); onDeleted();
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Não foi possível excluir sua conta.');
    } finally { setBusy(false); }
  }
  return <div className="text-sm">
    <button type="button" aria-expanded={open} onClick={() => { setOpen(!open); setPassword(''); setConfirmed(false); setError(''); }} className="rounded-full border border-red-300/30 px-4 py-2 font-bold text-red-200">Excluir meu acesso empresarial</button>
    {open && <form onSubmit={submit} className="mt-4 max-w-lg space-y-3 rounded-2xl border border-red-300/20 p-4">
      <p>Esta ação exclui sua conta pessoal em Empresas e encerra suas sessões nesta trilha. Contas responsáveis por organizações precisam transferir a responsabilidade com a administração antes de continuar.</p>
      <p>Vagas, pagamentos e registros da organização são preservados. Suas contas de Oportunidades e Capacita permanecem independentes.</p>
      <label className="block font-bold">Senha atual<input type="password" autoComplete="current-password" required value={password} onChange={event => setPassword(event.target.value)} className="mt-1 block w-full rounded-xl border border-white/20 bg-transparent p-3" /></label>
      <label className="flex gap-2"><input type="checkbox" required checked={confirmed} onChange={event => setConfirmed(event.target.checked)} />Confirmo a exclusão do meu acesso pessoal em Empresas.</label>
      {error && <p role="alert" className="text-red-200">{error}</p>}
      <div className="flex gap-3"><button disabled={busy || !confirmed || !password} className="rounded-full border border-red-300/40 px-4 py-2 font-bold disabled:opacity-50">{busy ? 'Excluindo…' : 'Confirmar exclusão'}</button><button type="button" disabled={busy} onClick={() => { setOpen(false); setPassword(''); }} className="rounded-full border border-white/20 px-4 py-2">Cancelar</button></div>
    </form>}
  </div>;
}
