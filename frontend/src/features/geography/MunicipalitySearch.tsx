import { publicSearch } from './publicSearch';
import { useEffect, useState } from 'react';
import { API_URL } from '../account/api';
import type { Municipality } from './territory';
import { radarButton as button, radarField as field } from '../radar/radarUi';
export function MunicipalitySearch({ onSelect }: {
    onSelect: (city: Municipality) => void;
}) { const [query, setQuery] = useState(''), [cities, setCities] = useState<Municipality[]>([]), [error, setError] = useState(''), [busy, setBusy] = useState(false); useEffect(() => { const abort = new AbortController(); setBusy(true); const timer = setTimeout(() => { publicSearch<Municipality[]>(API_URL + '/public/geography/municipalities?q=' + encodeURIComponent(query) + '&limit=20', abort.signal).then(rows => { if (!Array.isArray(rows))
    throw new Error(); if (!abort.signal.aborted) {
    setCities(rows);
    setError('');
} }).catch(() => { if (!abort.signal.aborted)
    setError('Não foi possível buscar municípios. Revise a conexão e tente novamente.'); }).finally(() => { if (!abort.signal.aborted)
    setBusy(false); }); }, 300); return () => { clearTimeout(timer); abort.abort(); }; }, [query]); return <div className="space-y-3"><label className="block">Buscar município pelo nome<input type="search" className={field} maxLength={120} value={query} onChange={e => setQuery(e.target.value)} placeholder="Ex.: Irecê, Lapão, Salvador"/></label>{busy && <p role="status">Buscando municípios…</p>}{error && <p role="alert">{error}</p>}<ul className="max-h-64 space-y-2 overflow-y-auto" aria-label="Municípios encontrados">{cities.map(city => <li key={city.code}><button type="button" className={button} onClick={() => onSelect(city)}>{city.name} — {city.state}</button></li>)}</ul>{!busy && !error && !cities.length && <p>Nenhum município encontrado. Revise o nome.</p>}</div>; }
