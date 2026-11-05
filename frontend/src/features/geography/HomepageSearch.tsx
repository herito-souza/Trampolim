import { lazy, Suspense, useEffect, useState } from 'react';
import { useTerritory } from './territoryContext';
import { MunicipalitySearch } from './MunicipalitySearch';
import { SearchRefinementFields } from './SearchRefinementFields';
import { searchSectors, searchField } from './searchOptions';
import type { SearchRefinements, TerritorialFilters } from './territory';
import type { Municipality } from './territory';
const Results = lazy(() => import('../account/CompanyOpportunities').then(module => ({ default: module.CompanyOpportunities })));
function savedDraft(): {query?:string;modality?:string;scope?:string;selected?:Municipality;area?:string;extra?:SearchRefinements;kind?:string} {
 try {
  const value=JSON.parse(sessionStorage.getItem('trampolim_search_draft')||'{}');if(!value||typeof value!=='object'||Array.isArray(value))return {};
  for(const key of ['query','modality','scope','area','kind'])if(value[key]!==undefined&&(typeof value[key]!=='string'||value[key].length>120))return {};
  if(value.selected&&(!/^[0-9]{7}$/.test(value.selected.code)||typeof value.selected.name!=='string'||!/^[A-Z]{2}$/.test(value.selected.state)))return {};
  if(value.extra&&(!value.extra||typeof value.extra!=='object'||Object.values(value.extra).some(v=>typeof v!=='string'||v.length>120)))return {};
  return value;
 }catch{return {}}
}
const field = 'mt-2 block min-h-12 w-full rounded-xl border border-ink/30 bg-paper px-4 py-3 text-base text-ink';
export function HomepageSearch() {
  const territory = useTerritory();
  const [query, setQuery] = useState(savedDraft().query||''), [modality, setModality] = useState(savedDraft().modality||'');
  const [selected, setSelected] = useState<Municipality | null>(savedDraft().selected||null);
  const [kind,setKind]=useState(savedDraft().kind||'job'); const [scope,setScope] = useState(savedDraft().scope||'city'); const all=scope==='all'; const [area,setArea]=useState(savedDraft().area||''); const [extra,setExtra]=useState<SearchRefinements>(savedDraft().extra||{}); const [editing, setEditing] = useState(false);
  const [filters, setFilters] = useState<(TerritorialFilters & {provider:string}) | null>(null);
  useEffect(()=>{try{sessionStorage.setItem('trampolim_search_draft',JSON.stringify({query,modality,scope,selected,area,extra,kind}))}catch{/* Search remains usable without browser storage. */}},[query,modality,scope,selected,area,extra,kind]);
  const city = selected || territory.settings?.referenceMunicipality;
  return <section id="busca-rapida" aria-labelledby="quick-search-title" className="scroll-mt-4 px-[6vw] pb-12"><div className="mx-auto max-w-7xl rounded-[28px] border border-ink/20 bg-surface p-5 md:p-8">
    <h2 id="quick-search-title" className="text-3xl font-black">Seu próximo passo pode estar perto</h2><p className="mt-2 text-sm text-ink/80">Busque empregos e estágios publicados. Você pode pesquisar sem criar uma conta.</p>
    <form className="mt-6 grid items-start gap-4 md:grid-cols-[1.3fr_1fr_1fr_auto]" onSubmit={event => { event.preventDefault(); if (territory.loading) return; territory.choose({ scope: scope==='city' ? city ? 'city:'+city.code : 'all' : scope, label: scope==='city' && city ? city.name+' — '+city.state : territory.settings?.regions.find(r=>'region:'+r.id===scope)?.name || (scope.startsWith('state:')?'Estado: '+scope.slice(6):'Todas as localidades'), includeRemote: false }); setFilters({ query: query.trim(), city: !territory.settings && !all ? selected?.name || 'Irecê' : '', kind, area, modality, provider: '', ...extra }); }}>
      <label className="md:col-span-4">O que você procura?<select className={field} value={kind} onChange={e=>{setKind(e.target.value);setExtra({});}}><option value="job">Empregos e estágios</option><option value="course">Cursos publicados por empresas</option></select></label>
      <label className="min-w-0 text-sm font-bold">Cargo ou área profissional<input className={field} type="search" maxLength={120} placeholder="Ex.: atendimento, vendas" value={query} onChange={event => setQuery(event.target.value)} /></label>
      <div className="min-w-0"><label className="text-sm font-bold">Município<select className={field} value={scope} onChange={event => setScope(event.target.value)}><option value="city">{city ? city.name + ' — ' + city.state : 'Irecê — BA (sugestão)'}</option>{territory.settings?.regions.map(r=><option key={r.id} value={'region:'+r.id}>{r.name}</option>)}{territory.settings?.states?.map(v=><option key={v} value={'state:'+v}>Estado: {v}</option>)}<option value="all">Todas as localidades</option></select></label><button type="button" aria-expanded={editing} onClick={() => setEditing(!editing)} className="mt-1 min-h-11 text-sm font-bold underline underline-offset-4">{editing ? 'Fechar escolha de município' : 'Alterar município'}</button></div>
      <label className="min-w-0 text-sm font-bold">Modalidade<select className={field} value={modality} onChange={event => setModality(event.target.value)}><option value="">Todas as modalidades</option><option>Presencial</option><option>Híbrido</option><option>Remoto</option></select></label>
      <button type="submit" disabled={territory.loading} className="min-h-12 rounded-xl bg-brand-yellow px-6 py-3 font-extrabold text-paper hover:bg-brand-green disabled:opacity-60 md:mt-7">{territory.loading ? 'Preparando busca…' : 'Pesquisar oportunidades'}</button>
      <details className="md:col-span-4"><summary className="min-h-11 cursor-pointer py-3 font-bold">Refinar por setor, contratação e requisitos</summary><div className="space-y-4 pt-3"><label className="block">Setor ou área informada<select className={searchField} value={area} onChange={e=>setArea(e.target.value)}><option value="">Todos os setores</option>{searchSectors.map(([v,label])=><option key={v} value={v}>{label}</option>)}</select></label>{kind==='job'&&<SearchRefinementFields value={extra} onChange={setExtra}/>}<p className="text-sm text-ink/80">As categorias organizam anúncios existentes; não indicam ranking de demanda regional. Cursos e materiais educacionais estão na trilha Capacita e no catálogo de cursos de Oportunidades.</p></div></details>
      <button type="button" className="min-h-11 text-left font-bold underline md:col-span-4" onClick={()=>{setQuery('');setModality('');setArea('');setExtra({});setScope('all');setSelected(null);setFilters(null);}}>Limpar filtros</button>
    </form>
    {editing && <div className="mt-4 max-w-xl"><MunicipalitySearch onSelect={value => { setSelected(value); setScope('city'); setEditing(false); }} /></div>}
    <p className="mt-3 text-xs text-ink/75">A localização é uma sugestão. Não utilizamos sua localização precisa. Para vagas remotas, escolha a modalidade Remoto e todas as localidades.</p>
    {territory.error && <p role="status" className="mt-3 text-sm">{territory.error}</p>}
    {filters && <button type="button" className="mt-4 min-h-11 font-bold underline" onClick={()=>{setScope('all');territory.choose({scope:'all',label:'Todas as localidades',includeRemote:true});}}>Ampliar busca para todas as localidades</button>}
    {filters && <div aria-label="Resultados da busca rápida"><Suspense fallback={<p role="status">Carregando a busca…</p>}><Results homepage filters={filters} /></Suspense></div>}
  </div></section>;
}
