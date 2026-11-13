import { useRef, useState } from 'react';
import { api } from './api';
type Trail = 'oportunidades' | 'empresas' | 'capacita';
type Request = { id: string; state: string; basis_code: string; revision: number; review_after: string | null };
const labels: Record<string, string> = { OPEN: 'Solicitação recebida', REVIEWING: 'Em análise', READY: 'Análise concluída: pode prosseguir', RETAINED: 'Retenção em análise', COMPLETED: 'Conta removida; registros necessários podem ser preservados', CANCELLED: 'Solicitação cancelada' };
const basisLabels: Record<string, string> = { USER_REQUEST: 'A pedido do titular', ORGANIZATION_RESPONSIBILITY: 'Responsabilidade por organização ou conteúdo', RETENTION_REVIEW: 'Revisão da necessidade de conservação', SECURITY_REVIEW: 'Revisão de segurança', SELF_SERVICE_DELETION: 'Exclusão confirmada da conta' };
export function PrivacyRequests({ trail, token }: { trail: Trail; token: string }) {
  const [rows, setRows] = useState<Request[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [loaded, setLoaded] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const key = useRef<string | null>(null);
  const path = trail === 'oportunidades' ? '/profile/privacy' : `/${trail}/account/privacy`;
  async function run(action?: () => Promise<unknown>) {
    if (busy) return;
    setBusy(true); setError('');
    try { if (action) await action(); setRows(await api<Request[]>(`${path}/requests`, token)); setLoaded(true); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível consultar a solicitação.'); }
    finally { setBusy(false); }
  }
  function cancel(row: Request) {
    void run(async () => { await api(path + '/requests/' + row.id + '/cancel', token, { method: 'POST', body: JSON.stringify({ expectedRevision: row.revision }) }); key.current = null; setConfirmed(false); });
  }
  function create() {
    if (!confirmed) return;
    key.current ??= crypto.randomUUID();
    void run(() => api(`${path}/requests`, token, { method: 'POST', body: JSON.stringify({ clientKey: key.current, confirmed: true }) }));
  }
  return <section className="w-full space-y-3 rounded-2xl border border-current/20 p-4" aria-label="Solicitações de privacidade">
    <h3 className="font-bold">Solicitar análise de exclusão dos meus dados</h3>
    <p className="text-sm">A solicitação vale somente para esta área. Nenhum dado é apagado automaticamente. Registros necessários podem precisar de conservação, e contas responsáveis por empresas ou cursos exigem análise.</p>
    <label className="flex items-start gap-2 text-sm"><input type="checkbox" checked={confirmed} onChange={event => setConfirmed(event.target.checked)} />Quero solicitar a análise dos meus dados nesta área.</label>
    <div className="flex flex-wrap gap-3"><button type="button" disabled={busy || !confirmed} onClick={create} className="min-h-11 rounded-full border border-current/30 px-4 py-2 font-bold disabled:opacity-50">Enviar solicitação</button><button type="button" disabled={busy} onClick={() => void run()} className="min-h-11 rounded-full border border-current/30 px-4 py-2">Consultar solicitações</button></div>
    {busy && <p role="status">Consultando…</p>}{error && <p role="alert">{error}</p>}
    {loaded && rows.length === 0 && <p role="status">Nenhuma solicitação registrada nesta área.</p>}
    <ul className="space-y-3">{rows.map(row => <li key={row.id} className="space-y-2 border-t border-current/15 pt-3"><p className="font-bold">{labels[row.state] || row.state}</p><p className="text-sm">Motivo: {basisLabels[row.basis_code] || 'Consulte o atendimento de privacidade'}</p><p className="break-all text-xs">Protocolo: {row.id}</p>{row.review_after && <p className="text-sm">Reavaliação prevista: {new Date(row.review_after).toLocaleDateString('pt-BR')}</p>}{['OPEN', 'READY'].includes(row.state) && <button type="button" disabled={busy} className="min-h-11 rounded-full border border-current/30 px-4 py-2" onClick={() => cancel(row)}>Cancelar solicitação</button>}</li>)}</ul>
    <p className="text-xs">Uma análise concluída não exclui a conta. Use a confirmação de exclusão existente se decidir prosseguir. A análise não substitui a orientação do responsável por privacidade.</p>
  </section>;
}
