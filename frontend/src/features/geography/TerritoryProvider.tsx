import {useEffect,useState} from 'react';
import type {ReactNode} from 'react';
import {API_URL} from '../account/api';
import type {TerritoryChoice,TerritorySettings} from './territory';
import {all,TerritoryContext} from './territoryContext';
export function TerritoryProvider({children}:{children:ReactNode}){
 const [choice,setChoice]=useState(all),[settings,setSettings]=useState<TerritorySettings|null>(null),[loading,setLoading]=useState(true),[error,setError]=useState('');
 useEffect(()=>{
  const abort=new AbortController();let active=true;
  const timeout=setTimeout(()=>abort.abort(),8000);
  fetch(API_URL+'/public/geography/settings',{signal:abort.signal}).then(async response=>{
   if(!response.ok)throw new Error();
   const config=await response.json() as TerritorySettings;
   if(!Array.isArray(config.regions)||!config.regions.some(r=>r.id===config.defaultRegion))throw new Error();
   if(!active)return;
   setSettings(config);let saved:TerritoryChoice|null=null;
   try{saved=JSON.parse(localStorage.getItem('trampolim_public_territory')||'null')}catch{/* Ignore an invalid public preference. */}
   if(saved&&typeof saved.scope==='string'&&typeof saved.label==='string'&&saved.label.length<180&&typeof saved.includeRemote==='boolean'&&(saved.scope==='all'||(saved.scope.startsWith('state:')&&config.states?.includes(saved.scope.slice(6)))||/^city:[0-9]{7}$/.test(saved.scope)||config.regions.some(r=>'region:'+r.id===saved?.scope)))setChoice(saved);
   else{const region=config.regions.find(r=>r.id===config.defaultRegion)!;setChoice({scope:'region:'+region.id,label:region.name,includeRemote:true})}
  }).catch(()=>{if(active)setError('Seleção regional indisponível. Os filtros tradicionais continuam disponíveis, sem restrição automática à região.')}).finally(()=>{clearTimeout(timeout);if(active)setLoading(false)});
  return()=>{active=false;clearTimeout(timeout);abort.abort()};
 },[]);
 function choose(next:TerritoryChoice){setChoice(next);try{localStorage.setItem('trampolim_public_territory',JSON.stringify(next))}catch{/* Selection still works without browser storage. */}}
 return <TerritoryContext value={{choice,settings,loading,error,choose}}>{children}</TerritoryContext>;
}
