import { useState } from 'react';
import { api } from './api';

export function AccountDataDownload({ trail, token }: { trail: 'empresas' | 'capacita'; token: string }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function download() {
    if (busy) return;
    setBusy(true); setError('');
    try {
      const data = await api<unknown>(`/${trail}/account/export`, token);
      const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }));
      const link = document.createElement('a');
      link.href = url; link.download = `trampolim-${trail}-meus-dados.json`;
      document.body.appendChild(link); link.click(); link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível exportar seus dados.'); }
    finally { setBusy(false); }
  }
  return <span className="inline-flex flex-wrap items-center gap-3"><button type="button" disabled={busy} onClick={() => void download()} className="rounded-full border border-white/15 px-4 py-2 text-xs font-bold">{busy ? 'Preparando arquivo…' : 'Baixar meus dados'}</button>{error && <span role="alert" className="text-sm">{error}</span>}</span>;
}
