import { useEffect, useRef, useState } from 'react';
import { DashboardActions } from './DashboardActions';
import { useTerritory } from '../geography/territoryContext';
import { fold } from '../geography/territory';
import type { Municipality } from '../geography/territory';
import { API_URL } from '../account/api';
import type { Profile } from '../account/api';
import type { AccountDestination } from '../account/AccountNavigation';

export function CandidateNextStep({profile,hasDiagnostic,onProfile,onDiagnostic,onCatalog,onNavigate}:{profile:Profile;hasDiagnostic:boolean;onProfile:()=>void;onDiagnostic:()=>void;onCatalog:(kind:'job'|'course')=>void;onNavigate:(destination:AccountDestination)=>void}) {
 const territory=useTerritory();const [busy,setBusy]=useState(false),[error,setError]=useState('');const lookup=useRef<AbortController|null>(null);
 useEffect(()=>{const holder=lookup;return()=>holder.current?.abort()},[]);
 const place=(profile.location||'').replace(/\s*[,/—]\s*[A-Z]{2}\b.*$/i,'').replace(/\s+-\s+[A-Z]{2}\b.*$/i,'').trim();
 const missing=!profile.name?.trim()||!profile.summary?.trim()||!profile.phone?.trim()||!profile.location?.trim();
 function catalog(mode:'local'|'region'|'remote'|'internship') {
  try{sessionStorage.setItem('trampolim_catalog_search_job',JSON.stringify({query:'',area:'',provider:'',city:'',modality:mode==='remote'?'Remoto':'',experienceLevel:'',contractType:mode==='internship'?'Estágio':'',workSchedule:'',minimumSalary:'',maximumSalary:'',extraSerialized:'{}'}))}catch{/* Navigation remains available without storage. */}
  onCatalog('job');
 }
 async function localJobs(){
  if(!place){onProfile();return;}
  lookup.current?.abort();const abort=new AbortController();lookup.current=abort;setBusy(true);setError('');
  try{
   const state=profile.location?.match(/(?:[,/—]|\s-\s)\s*([A-Z]{2})\b/i)?.[1]?.toUpperCase();
   const response=await fetch(API_URL+'/public/geography/municipalities?'+new URLSearchParams({q:place,limit:'50',...(state?{state}:{})}),{signal:abort.signal});if(!response.ok)throw new Error();
   const cities:Municipality[]=await response.json();const matches=cities.filter(c=>fold(c.name)===fold(place));
   if(matches.length!==1){setError('Não identificamos um município único no seu perfil. Atualize a localidade ou use a seleção de município na busca.');return;}
   const city=matches[0];territory.choose({scope:'city:'+city.code,label:city.name+' — '+city.state,includeRemote:false});catalog('local');
  }catch{if(!abort.signal.aborted)setError('Não foi possível identificar o município agora. Tente novamente ou escolha a localidade na busca.');}finally{if(!abort.signal.aborted)setBusy(false);}
 }
 return <><DashboardActions title="Seu próximo passo" description="Escolha uma ação para avançar. As sugestões consideram os dados do seu perfil e o diagnóstico disponível; não representam garantia de vaga ou contratação." actions={[
  {label:missing?'Completar meu perfil':'Revisar meu perfil',description:missing?'Confira contato, município e resumo profissional. Não é necessário ter experiência anterior.':'Mantenha seu contato, formação e interesses atualizados.',onSelect:onProfile},
  {label:hasDiagnostic?'Consultar meu diagnóstico':'Descobrir meu caminho profissional',description:hasDiagnostic?'Reveja suas recomendações na área de diagnóstico.':'Conheça interesses e possíveis caminhos profissionais.',onSelect:onDiagnostic},
  {label:place?'Buscar vagas em '+place:'Informar meu município',description:'Local do anúncio, sem usar sua localização precisa. Você pode alterar os filtros.',onSelect:()=>void localJobs(),disabled:busy},
  {label:fold(place)===fold(territory.settings?.referenceMunicipality.name)?'Ampliar para municípios próximos':'Explorar uma região',description:territory.settings?'Consultar anúncios em '+(territory.settings.regions.find(r=>r.id===territory.settings?.defaultRegion)?.name||'uma região configurada')+'.':'Abrir a busca e escolher uma região.',onSelect:()=>{const region=territory.settings?.regions.find(r=>r.id===territory.settings?.defaultRegion);if(region)territory.choose({scope:'region:'+region.id,label:region.name,includeRemote:true});catalog('region');}},
  {label:'Encontrar vagas remotas',description:'Pesquisar trabalho remoto em todas as localidades.',onSelect:()=>{territory.choose({scope:'all',label:'Todas as localidades',includeRemote:false});catalog('remote');}},
  {label:'Explorar estágios',description:'Aplicar o filtro de estágio na área de busca selecionada.',onSelect:()=>catalog('internship')},
  {label:'Consultar cursos disponíveis',description:'Explorar o catálogo de cursos sem transferir sua conta para outra trilha.',onSelect:()=>onCatalog('course')},
  {label:'Desenvolver minhas competências',description:'Consultar o Radar, revisar competências declaradas e conhecer lacunas.',onSelect:()=>onNavigate('radar')}
 ]}/>{busy&&<p role="status">Identificando o município informado…</p>}{error&&<div role="status" className="my-4 rounded-xl border border-brand-blue/30 p-4"><p>{error}</p><button type="button" className="mt-2 min-h-11 underline" onClick={()=>onCatalog('job')}>Abrir busca para escolher município</button></div>}</>;
}
