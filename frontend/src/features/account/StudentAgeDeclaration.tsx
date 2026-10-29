import { useEffect, useState } from 'react';
import { api } from './api';
export function StudentAgeDeclaration({ token }: { token: string }) {
  const [age, setAge] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  useEffect(() => {
    let active = true;
    api<{ ageBand: string | null }>('/capacita/account/age-declaration', token)
      .then(data => { if (active) setAge(data.ageBand || ''); })
      .catch(cause => { if (active) setError(cause instanceof Error ? cause.message : 'Não foi possível consultar sua declaração.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [token]);
  async function save() {
    if (!age || busy) return;
    setBusy(true); setError(''); setMessage('');
    try { await api<void>('/capacita/account/age-declaration', token, { method: 'PUT', body: JSON.stringify({ ageBand: age }) }); setMessage('Faixa etária declarada salva.'); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível salvar.'); }
    finally { setBusy(false); }
  }
  return <section className="mt-5 w-full rounded-2xl border border-white/15 p-4"><h2 className="text-lg font-bold">Sua faixa etária</h2><p className="mt-2 text-sm">O Capacita atende pessoas a partir de 14 anos. Informe ou corrija sua faixa etária; esta declaração não verifica sua idade.</p>{loading ? <p role="status">Carregando declaração…</p> : <><label className="mt-3 block text-sm">Faixa etária declarada<select disabled={busy} value={age} onChange={event => setAge(event.target.value)} className="mt-2 block w-full rounded-xl border border-white/20 bg-[#101a29] p-3"><option value="">Selecione</option><option value="AGE_14_17">14 a 17 anos</option><option value="AGE_18_PLUS">18 anos ou mais</option></select></label><button disabled={!age || busy} onClick={() => void save()} className="mt-3 rounded-full border border-white/20 px-4 py-2">Salvar faixa etária</button></>}{message && <p role="status" className="mt-3">{message}</p>}{error && <p role="alert" className="mt-3">{error}</p>}</section>;
}
