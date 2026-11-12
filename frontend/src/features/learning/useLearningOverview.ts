import {useEffect,useState} from 'react';
import {api} from '../account/api';
import type {Performance} from './performanceTypes';
import type {Attempt} from './assessmentTypes';
export function useLearningOverview(token:string,enabled:boolean,revision:number){
 const [snapshot,setSnapshot]=useState<{token:string;revision:number;summary:Performance|null;attempts:Attempt[]|null;error:string}|null>(null);
 const [retry,setRetry]=useState(0);
 useEffect(()=>{const refresh=()=>{setSnapshot(null);setRetry(v=>v+1)};window.addEventListener('capacita-performance-changed',refresh);return()=>window.removeEventListener('capacita-performance-changed',refresh)},[]);
 const valid=snapshot?.token===token&&snapshot.revision===revision?snapshot:null;
 useEffect(()=>{if(!enabled||valid)return;const abort=new AbortController();
 Promise.allSettled([api<Performance>('/capacita/learning/performance',token,{signal:abort.signal}),api<Attempt[]>('/capacita/learning/assessment-attempts',token,{signal:abort.signal})]).then(([a,b])=>{if(abort.signal.aborted)return;const summary=a.status==='fulfilled'&&Array.isArray(a.value.courses)&&Number.isFinite(a.value.competenciesEarned)?a.value:null;const attempts=b.status==='fulfilled'&&Array.isArray(b.value)?b.value:null;setSnapshot({token,revision,summary,attempts,error:!summary||!attempts?'Parte do resumo não está disponível. Os demais recursos continuam acessíveis.':''})});return()=>abort.abort();
 },[token,enabled,revision,retry,valid]);
 return {summary:valid?.summary,attempts:valid?.attempts,error:valid?.error,loading:enabled&&!valid,retry:()=>{setSnapshot(null);setRetry(v=>v+1)}};
}
