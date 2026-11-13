import { useEffect, useMemo, useRef, useState } from 'react';
import type { Navigation } from './workspaceContext';
import type { KeyboardEvent as ReactKeyboardEvent } from 'react';
import {matchesSearch} from './universalSearchModel';

export type UniversalSearchResult = { id: string; category: string; title: string; description?: string; onSelect: () => void };
export type UniversalSearchProvider = (query: string, signal: AbortSignal) => Promise<UniversalSearchResult[]>;
type Entry = UniversalSearchResult;

export function UniversalSearch({ trail, navigations, provider }: { trail: string; navigations: Navigation[]; provider?: UniversalSearchProvider }) {
  const [open, setOpen] = useState(false), [query, setQuery] = useState(''), [resourceResults, setResourceResults] = useState<Entry[]>([]);
  const [loading, setLoading] = useState(false), [error, setError] = useState(''), [selected, setSelected] = useState(0);
  const dialog = useRef<HTMLDialogElement>(null), input = useRef<HTMLInputElement>(null), trigger = useRef<HTMLButtonElement>(null);
  const providerRef = useRef(provider); providerRef.current = provider;
  const providerEnabled=Boolean(provider);
  const entries = useMemo<Entry[]>(() => navigations.flatMap(nav => nav.groups.flatMap(group => group.items.filter(item => !item.disabled).map(item => ({
    id: `${nav.owner}-${group.id}-${item.id}`, category: group.label, title: item.label, onSelect: item.onSelect,
  })))).filter(entry => matchesSearch(query, entry.title, entry.category)), [navigations, query]);
  const groups = useMemo(() => {
    const all = [...entries, ...resourceResults];
    const labels = [...new Set(all.map(entry => entry.category))];
    return labels.map(label => ({ label, items: all.filter(entry => entry.category === label) }));
  }, [entries, resourceResults]);
  const flat = useMemo(() => groups.flatMap(group => group.items), [groups]);
  const activeId = flat[selected] ? `universal-search-option-${selected}` : undefined;

  useEffect(() => {
    const shortcut = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); setOpen(true); }
    };
    document.addEventListener('keydown', shortcut);
    return () => document.removeEventListener('keydown', shortcut);
  }, []);

  useEffect(() => {
    if (!open) return;
    const element = dialog.current;
    if (!element) return;
    if (!element.open) element.showModal();
    requestAnimationFrame(() => input.current?.focus());
    return () => { if (element.open) element.close(); };
  }, [open]);

  useEffect(() => {
    const search = providerRef.current;
    if (!open || !search || query.trim().length < 2) { setResourceResults([]); setLoading(false); setError(''); return; }
    const controller = new AbortController();
    setLoading(true); setError('');
    const timer = window.setTimeout(() => {
      search(query.trim(), controller.signal).then(results => {
        if (!controller.signal.aborted) setResourceResults(results.slice(0, 12));
      }).catch(() => { if (!controller.signal.aborted) { setResourceResults([]); setError('Não foi possível buscar conteúdos agora. Tente novamente.'); } })
        .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    }, 300);
    return () => { window.clearTimeout(timer); controller.abort(); };
  }, [open, query, providerEnabled]);

  useEffect(() => setSelected(0), [query, resourceResults.length]);
  function close() { setOpen(false); setQuery(''); requestAnimationFrame(() => trigger.current?.focus()); }
  function choose(entry: Entry) { close(); entry.onSelect(); }
  function keyDown(event: ReactKeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown' && flat.length) { event.preventDefault(); setSelected(index => (index + 1) % flat.length); }
    else if (event.key === 'ArrowUp' && flat.length) { event.preventDefault(); setSelected(index => (index - 1 + flat.length) % flat.length); }
    else if (event.key === 'Home' && flat.length) { event.preventDefault(); setSelected(0); }
    else if (event.key === 'End' && flat.length) { event.preventDefault(); setSelected(flat.length - 1); }
    else if (event.key === 'Enter' && flat[selected]) { event.preventDefault(); choose(flat[selected]); }
    else if (event.key === 'Escape') { event.preventDefault(); close(); }
  }

  return <>
    <button ref={trigger} type="button" aria-label={`Pesquisar no Trampolim ${trail} (Ctrl+K ou Cmd+K)`} title="Pesquisar · Ctrl/Cmd+K" onClick={() => setOpen(true)} className="workspace-search-trigger"><SearchIcon/><span>Pesquisar</span><kbd aria-hidden="true">Ctrl/⌘ K</kbd></button>
    <dialog ref={dialog} aria-label={`Pesquisa no Trampolim ${trail}`} onCancel={event => { event.preventDefault(); close(); }} onClick={event => { if (event.target === event.currentTarget) close(); }} className="workspace-search-dialog">
      <div className="workspace-search-panel">
        <div className="workspace-search-field"><SearchIcon/><input ref={input} type="search" role="combobox" aria-label={`Pesquisar no Trampolim ${trail}`} aria-autocomplete="list" aria-controls="universal-search-results" aria-expanded={flat.length > 0} aria-activedescendant={activeId} value={query} onChange={event => setQuery(event.target.value.slice(0, 120))} onKeyDown={keyDown} placeholder="Pesquise páginas, ações e conteúdos" autoComplete="off"/><button type="button" aria-label="Fechar pesquisa" onClick={close}>Esc</button></div>
        <div id="universal-search-results" role="listbox" aria-label="Resultados da pesquisa" className="workspace-search-results">
          {groups.map(group => <div role="group" aria-label={group.label} key={group.label} className="workspace-search-group"><h2>{group.label}</h2>{group.items.map(entry => { const index = flat.indexOf(entry); return <div key={entry.id} id={`universal-search-option-${index}`} role="option" aria-selected={selected === index} className="workspace-search-option" onMouseMove={() => setSelected(index)} onMouseDown={event => event.preventDefault()} onClick={() => choose(entry)}><span><strong>{entry.title}</strong>{entry.description&&<small>{entry.description}</small>}</span><span className="workspace-search-enter" aria-hidden="true">↵</span></div>; })}</div>)}
          {loading&&<p role="status" className="workspace-search-message">Buscando conteúdos…</p>}
          {error&&<p role="status" className="workspace-search-message">{error}</p>}
          {!loading&&!error&&query.trim().length<2&&<p className="workspace-search-message">Digite ao menos 2 letras para pesquisar páginas e conteúdos.</p>}
          {!loading&&!error&&query.trim().length>=2&&flat.length===0&&<p className="workspace-search-message">Nenhum resultado encontrado. Tente outro termo.</p>}
        </div>
        <p className="workspace-search-hint">Use ↑ ↓ para navegar, Enter para abrir e Esc para fechar.</p>
      </div>
    </dialog>
  </>;
}

function SearchIcon() { return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-5 w-5"><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 5 5"/></svg>; }
