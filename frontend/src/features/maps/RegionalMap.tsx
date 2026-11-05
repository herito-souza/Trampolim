import { useEffect, useRef, useState } from 'react';
import { Map as LibreMap, setWorkerCount, setWorkerUrl } from 'maplibre-gl';
import type { GeoJSONSource } from 'maplibre-gl';
import workerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url';
import 'maplibre-gl/dist/maplibre-gl.css';
import './regionalMap.css';
import { featureCollection, pointBounds, validCoordinates } from './mapModel';
import type { Bounds, MapOpportunity } from './mapModel';
setWorkerUrl(workerUrl);setWorkerCount(1);
const style=import.meta.env.VITE_MAP_STYLE_URL||'https://tiles.openfreemap.org/styles/liberty';
const cameraKey='trampolim_public_map_camera';
function camera(scope:string):{center:[number,number];zoom:number}|null{try{const value=JSON.parse(sessionStorage.getItem(cameraKey)||'null');return value?.scope===scope&&Array.isArray(value.center)&&validCoordinates(value.center[1],value.center[0])&&Number.isFinite(value.zoom)&&value.zoom>=0&&value.zoom<=18?value:null}catch{return null}}
function allowedStyle(){try{const url=new URL(style,window.location.origin);return url.protocol==='https:'||url.origin===window.location.origin}catch{return false}}
export function RegionalMap({points,scope,bounds,selected,onSelect}:{points:MapOpportunity[];scope:string;bounds:Bounds;selected:number|null;onSelect:(id:number)=>void}){
 const host=useRef<HTMLDivElement>(null),mapRef=useRef<LibreMap|null>(null),pointsRef=useRef(points),selectRef=useRef(onSelect);pointsRef.current=points;selectRef.current=onSelect;
 const [ready,setReady]=useState(false),[error,setError]=useState(''),[groups,setGroups]=useState(0),[retry,setRetry]=useState(0);
 const motion=()=>window.matchMedia('(prefers-reduced-motion: reduce)').matches?0:350;
 useEffect(()=>{
  if(!host.current)return;setReady(false);setError('');setGroups(0);let active=true;let map:LibreMap|null=null;
  const timeout=setTimeout(()=>{if(active)setError('A base do mapa está demorando a responder. Você pode continuar pela lista.')},12000);
  try{
   if(!allowedStyle())throw new Error('Configuração de estilo inválida.');
   const saved=camera(scope);
   map=new LibreMap({container:host.current,style,...(saved?{center:saved.center,zoom:saved.zoom}:{bounds,fitBoundsOptions:{padding:25,maxZoom:12}}),minZoom:2,maxZoom:18,renderWorldCopies:false,cooperativeGestures:true,dragRotate:false,pitchWithRotate:false,attributionControl:{compact:false},transformRequest:url=>({url,credentials:'same-origin'}),locale:{'AttributionControl.ToggleAttribution':'Mostrar créditos do mapa','CooperativeGesturesHandler.WindowsHelpText':'Use Ctrl e a rolagem para aproximar o mapa','CooperativeGesturesHandler.MacHelpText':'Use ⌘ e a rolagem para aproximar o mapa','CooperativeGesturesHandler.MobileHelpText':'Use dois dedos para mover o mapa'}});
   mapRef.current=map;map.getCanvas().setAttribute('aria-label','Mapa interativo de anúncios. Use as setas para mover e os botões para ampliar. A lista oferece a alternativa textual.');
   map.on('load',()=>{
    if(!active||!map)return;clearTimeout(timeout);
    map.addSource('trampolim-opportunities',{type:'geojson',data:featureCollection(pointsRef.current),cluster:true,clusterRadius:55,clusterMaxZoom:14});
    map.addLayer({id:'trampolim-clusters',type:'circle',source:'trampolim-opportunities',filter:['has','point_count'],paint:{'circle-color':'#91B8DC','circle-radius':['step',['get','point_count'],20,10,26,30,32],'circle-stroke-color':'#0B1421','circle-stroke-width':3}});
    map.addLayer({id:'trampolim-cluster-count',type:'symbol',source:'trampolim-opportunities',filter:['has','point_count'],layout:{'text-field':['get','point_count_abbreviated'],'text-font':['Noto Sans Regular'],'text-size':14},paint:{'text-color':'#0B1421'}});
    map.addLayer({id:'trampolim-points',type:'circle',source:'trampolim-opportunities',filter:['!',['has','point_count']],paint:{'circle-color':['match',['get','kind'],'course','#91B8DC','#D6FF38'],'circle-radius':9,'circle-stroke-color':'#0B1421','circle-stroke-width':3}});
    setReady(true);setError('');
   });
   map.on('click','trampolim-clusters',event=>{const feature=event.features?.[0];if(!feature||feature.geometry.type!=='Point'||!map)return;const center=feature.geometry.coordinates as [number,number];const source=map.getSource('trampolim-opportunities') as GeoJSONSource;source.getClusterExpansionZoom(Number(feature.properties.cluster_id)).then(zoom=>{if(active)map?.easeTo({center,zoom,duration:motion()})}).catch(()=>{if(active)setError('Não foi possível ampliar o grupo. Use os controles ou a seleção textual.');});});
   map.on('click','trampolim-points',event=>{const id=Number(event.features?.[0]?.properties?.id);if(pointsRef.current.some(item=>item.id===id))selectRef.current(id)});
   for(const layer of ['trampolim-clusters','trampolim-points']){map.on('mouseenter',layer,()=>{if(map)map.getCanvas().style.cursor='pointer'});map.on('mouseleave',layer,()=>{if(map)map.getCanvas().style.cursor=''})}
   map.on('idle',()=>{if(active&&map?.getLayer('trampolim-clusters'))setGroups(map.queryRenderedFeatures({layers:['trampolim-clusters']}).length)});
   map.on('moveend',()=>{if(!active||!map)return;const c=map.getCenter();try{sessionStorage.setItem(cameraKey,JSON.stringify({scope,center:[Number(c.lng.toFixed(3)),Number(c.lat.toFixed(3))],zoom:map.getZoom()}))}catch{/* Camera still works without storage. */}});
   map.on('error',()=>{if(active)setError('Parte da base cartográfica não pôde ser carregada. Os anúncios e a lista continuam disponíveis.')});
  }catch{clearTimeout(timeout);setError('Este navegador não conseguiu iniciar o mapa. Use a lista ou tente outro navegador com WebGL habilitado.');}
  return()=>{active=false;clearTimeout(timeout);mapRef.current=null;map?.remove()};
 },[scope,bounds,retry]);
 useEffect(()=>{if(ready)(mapRef.current?.getSource('trampolim-opportunities') as GeoJSONSource|undefined)?.setData(featureCollection(points));},[points,ready]);
 useEffect(()=>{if(!ready||!selected)return;const item=pointsRef.current.find(item=>item.id===selected);if(item)mapRef.current?.easeTo({center:[item.longitude,item.latitude],zoom:Math.max(mapRef.current.getZoom(),12),duration:motion()})},[selected,ready]);
 return <div className="trampolim-map space-y-3">
  <div role="group" aria-label="Controles do mapa" className="flex flex-wrap gap-2"><button type="button" disabled={!ready} onClick={()=>mapRef.current?.zoomIn({duration:motion()})}>Aproximar mapa</button><button type="button" disabled={!ready} onClick={()=>mapRef.current?.zoomOut({duration:motion()})}>Afastar mapa</button><button type="button" disabled={!ready||!points.length} onClick={()=>{const box=pointBounds(points);if(box)mapRef.current?.fitBounds(box,{padding:45,maxZoom:14,duration:motion()})}}>Enquadrar anúncios desta página</button><button type="button" disabled={!ready} onClick={()=>mapRef.current?.fitBounds(bounds,{padding:25,maxZoom:12,duration:motion()})}>Enquadrar localidade</button></div>
  {!ready&&!error&&<p role="status">Preparando base cartográfica…</p>}
  {error&&<div role="status" className="space-y-2 rounded-xl border border-brand-blue/40 p-3"><p>{error}</p><button type="button" onClick={()=>setRetry(v=>v+1)}>Tentar carregar mapa novamente</button></div>}
  <div ref={host} data-map-ready={ready?'true':'false'} aria-label="Mapa regional de oportunidades" role="region" className="h-[min(65vh,30rem)] min-h-80 w-full overflow-hidden rounded-xl border border-white/20"/>
  <p role="status" className="text-sm">{points.length} anúncios com coordenadas nesta página · {groups} agrupamentos visíveis. Círculos agrupados podem ser ampliados; selecione anúncios também pelo controle textual.</p>
  <p className="text-xs text-ink/80">Base: <a href="https://openfreemap.org/" target="_blank" rel="noreferrer" className="underline">OpenFreeMap</a> · <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noreferrer" className="underline">OpenStreetMap e colaboradores</a>. Limites de enquadramento: IBGE.</p>
 </div>;
}
