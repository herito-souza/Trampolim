import { Component, lazy, Suspense, useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { useTerritory } from '../geography/territoryContext';
import { mapOpportunities, pointBounds, safeMapUrl } from './mapModel';
import type { Bounds, MapRecord } from './mapModel';
import { initialViewport, knownViewport, municipalViewport, nationalViewport } from './mapViewports';
const InteractiveMap=lazy(()=>import('./RegionalMap').then(module=>({default:module.RegionalMap})));
class MapBoundary extends Component<{children:ReactNode},{failed:boolean}>{state={failed:false};static getDerivedStateFromError(){return {failed:true}}render(){return this.state.failed?<p role="status">Não foi possível carregar o mapa. A listagem continua disponível abaixo.</p>:this.props.children}}
export function OpportunityMapView({rows,kind,loading,onOpen}:{rows:MapRecord[];kind:'job'|'course';loading:boolean;onOpen?:(id:number)=>void}){
 const {choice,settings}=useTerritory();const [open,setOpen]=useState(false),[selected,setSelected]=useState<number|null>(null),[bounds,setBounds]=useState<Bounds>(()=>knownViewport(choice.scope)||initialViewport),[framedScope,setFramedScope]=useState(choice.scope),[framing,setFraming]=useState(false),[notice,setNotice]=useState('');
 const points=useMemo(()=>loading?[]:mapOpportunities(rows,kind),[rows,kind,loading]);
 const pointsRef=useRef(points);pointsRef.current=points;
 const selectedItem=points.find(p=>p.id===selected);const url=selectedItem?safeMapUrl(selectedItem.url):null;
 useEffect(()=>{
  if(!open)return;
  const cached=!settings&&choice.scope==='all'?initialViewport:knownViewport(choice.scope);const region=settings?.regions.find(region=>'region:'+region.id===choice.scope);
  const boxes=region?.municipalityCodes.map(code=>knownViewport('city:'+code));
  const union=boxes?.length&&boxes.every((b):b is Bounds=>Boolean(b))?[Math.min(...boxes.map(b=>b[0])),Math.min(...boxes.map(b=>b[1])),Math.max(...boxes.map(b=>b[2])),Math.max(...boxes.map(b=>b[3]))] as Bounds:null;
  setSelected(null);setNotice('');
  if(cached||union){setFraming(false);setFramedScope(choice.scope);setBounds(cached||union!);return;}
  const abort=new AbortController();setFraming(true);let active=true;const timeout=setTimeout(()=>abort.abort(),10000);
  municipalViewport(region?'city:'+settings!.referenceMunicipality.code:choice.scope,abort.signal).then(value=>{if(active){setFramedScope(choice.scope);setBounds(value||pointBounds(pointsRef.current)||nationalViewport);if(!value)setNotice('Enquadramento pelos anúncios disponíveis. Mover o mapa não altera os filtros de localidade.')}}).catch(()=>{if(active){setFramedScope(choice.scope);setBounds(pointBounds(pointsRef.current)||nationalViewport);setNotice('Não foi possível obter os limites desta localidade. Use os anúncios disponíveis ou a lista.')}}).finally(()=>{clearTimeout(timeout);if(active)setFraming(false)});
  return()=>{active=false;clearTimeout(timeout);abort.abort()};
  // Filter changes update points; only a geographic selection changes the initial framing.
 },[open,choice.scope,settings]);
 function showInList(){setOpen(false);const id=selectedItem?.id;requestAnimationFrame(()=>{const card=id?document.getElementById('catalog-item-'+id):null;card?.scrollIntoView({block:'center'});card?.focus({preventScroll:true})});}
 return <section aria-label="Visualização geográfica das oportunidades" className="my-6 space-y-4 rounded-2xl border border-white/20 bg-surface p-4 md:p-6">
  <div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-2xl font-black">Lista e mapa</h2><div className="flex gap-2" role="group" aria-label="Modo de visualização"><button type="button" aria-pressed={!open} onClick={()=>setOpen(false)} className="min-h-11 rounded-full border border-white/30 px-4 py-2 font-bold">Somente lista</button><button type="button" aria-pressed={open} onClick={()=>setOpen(true)} className="min-h-11 rounded-full border border-brand-blue/70 px-4 py-2 font-bold">Abrir mapa regional</button></div></div>
  <p className="text-sm leading-6 text-ink/80">A lista é a opção inicial. Ao abrir o mapa, a base cartográfica externa recebe as consultas de mapa, sem seu token ou dados de perfil. Nenhuma localização do aparelho é solicitada.</p>
  {open&&<><p role="status" className="text-sm">{loading?'Atualizando os anúncios…':`${points.length} de ${rows.length} anúncios desta página podem ser localizados no mapa.`} A lista completa desta página permanece abaixo.</p><p className="text-sm text-ink/80">Somente coordenadas cadastradas são utilizadas. Anúncios sem coordenadas, vagas remotas e cursos que não sejam presenciais permanecem na lista. Vagas empresariais sem coordenadas também são consultadas pela lista.</p>
   {framing||framedScope!==choice.scope?<p role="status">Preparando enquadramento da localidade…</p>:<MapBoundary key={choice.scope}><Suspense fallback={<p role="status">Carregando o mapa sob demanda…</p>}><InteractiveMap points={points} scope={choice.scope} bounds={bounds} selected={selectedItem?.id||null} onSelect={setSelected}/></Suspense></MapBoundary>}
   {notice&&<p role="status">{notice}</p>}
   {!loading&&!points.length&&<p role="status">Nenhum anúncio desta página possui localização adequada ao mapa. Escolha outro município, amplie a região ou consulte a lista; não atribuímos coordenadas ao centro da cidade.</p>}
   {points.length>0&&<label className="block font-bold">Selecionar oportunidade no mapa<select className="mt-2 min-h-12 w-full rounded-xl border border-white/30 bg-paper p-3 text-ink" value={selectedItem?.id||''} onChange={e=>setSelected(e.target.value?Number(e.target.value):null)}><option value="">Selecione um anúncio</option>{points.map(item=><option key={item.id} value={item.id}>{item.title} · {item.provider}</option>)}</select></label>}
   {selectedItem&&<div role="region" aria-label="Resumo da oportunidade selecionada" className="space-y-3 rounded-xl border border-brand-blue/50 bg-paper p-4"><p className="text-lg font-extrabold">{selectedItem.title}</p><p>{selectedItem.provider} · {selectedItem.location} · {selectedItem.modality}</p><div className="flex flex-wrap gap-3">{url&&<a href={url} target="_blank" rel="noreferrer" onClick={()=>onOpen?.(selectedItem.id)} className="inline-flex min-h-11 items-center rounded-full border border-white/30 px-4 py-2 font-bold">{kind==='course'?'Consultar curso':'Consultar vaga'}</a>}<button type="button" className="min-h-11 rounded-full border border-white/30 px-4 py-2 font-bold" onClick={showInList}>Ver este anúncio na lista</button></div></div>}
   <p className="text-sm text-ink/80">Os filtros e as páginas da lista também atualizam o mapa. Arrastar o mapa não busca novos anúncios. Use os filtros para pesquisar em municípios próximos ou o raio, quando houver coordenadas suficientes.</p>
  </>}
 </section>;
}
