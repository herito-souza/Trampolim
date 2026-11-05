import { useEffect, useState } from 'react';
import { api } from '../account/api';
import { MunicipalitySearch } from './MunicipalitySearch';
import type { Municipality } from './territory';
import { radarButton as button } from '../radar/radarUi';
export function MunicipalityAssignment({ token, path }: {
    token: string;
    path: string;
}) { const [selected, setSelected] = useState<Municipality | null>(null), [clear, setClear] = useState(false), [open, setOpen] = useState(false), [current, setCurrent] = useState(''), [busy, setBusy] = useState(false), [notice, setNotice] = useState(''), [error, setError] = useState(''); useEffect(() => { const abort = new AbortController(); setSelected(null); setClear(false); setNotice(''); setError(''); api<{
    municipality: Municipality | null;
}>(path, token, { signal: abort.signal }).then(result => { if (!abort.signal.aborted)
    setCurrent(result.municipality ? result.municipality.name + ' — ' + result.municipality.state : 'Sem município padronizado'); }).catch(e => { if (!abort.signal.aborted)
    setError(e.message); }); return () => abort.abort(); }, [token, path]); return <section className="space-y-3 rounded-xl border border-white/15 p-4"><h3 className="font-bold">Município do local da atividade</h3><p>{current}</p><p>Selecione o local efetivo da vaga ou oferta presencial. Não utilize a sede da empresa como substituto. O texto original será preservado.</p><button type="button" className={button} onClick={() => setOpen(!open)}>Escolher município do anúncio</button>{open && <MunicipalitySearch onSelect={city => { setSelected(city); setClear(false); setOpen(false); }}/>}{selected && <p>Seleção para salvar: {selected.name} — {selected.state}</p>}<label className="flex gap-3"><input type="checkbox" checked={clear} onChange={e => { setClear(e.target.checked); setSelected(null); }}/>Remover apenas a associação municipal padronizada</label><button type="button" className={button} disabled={busy || !selected && !clear} onClick={async () => { setBusy(true); setError(''); setNotice(''); try {
    await api(path, token, { method: 'PUT', body: JSON.stringify({ municipalityCode: clear ? null : selected?.code }) });
    setCurrent(clear ? 'Sem município padronizado' : selected!.name + ' — ' + selected!.state);
    setSelected(null);
    setClear(false);
    setNotice('Município atualizado; o texto e a publicação foram preservados.');
}
catch (e) {
    setError(e instanceof Error ? e.message : 'Falha ao salvar município.');
}
finally {
    setBusy(false);
} }}>Confirmar município do anúncio</button>{notice && <p role="status">{notice}</p>}{error && <p role="alert">{error}</p>}</section>; }
