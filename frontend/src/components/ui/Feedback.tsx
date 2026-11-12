import { Suspense, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
export function LoadingStatus({label='Carregando conteúdo…'}:{label?:string}){
 const [slow,setSlow]=useState(false),[offline,setOffline]=useState(!navigator.onLine);
 useEffect(()=>{const timer=setTimeout(()=>setSlow(true),3000),online=()=>setOffline(false),lost=()=>setOffline(true);window.addEventListener('online',online);window.addEventListener('offline',lost);return()=>{clearTimeout(timer);window.removeEventListener('online',online);window.removeEventListener('offline',lost)}},[]);
 return <p role="status" aria-live="polite" className="min-h-24 rounded-2xl border border-white/15 bg-surface p-4 text-sm font-semibold text-ink">{label}<span className="mt-2 block min-h-6 text-xs font-normal">{offline?'Sem conexão. O carregamento poderá continuar quando a internet voltar.':slow?'O carregamento está demorando. Verifique sua conexão; os dados já salvos permanecem disponíveis.':' '}</span></p>;
}
export function AsyncPanel({children,label}:{children:ReactNode;label?:string}){return <Suspense fallback={<LoadingStatus label={label}/>}>{children}</Suspense>}
export function InlineNotice({children,error=false,onRetry,retryLabel='Tentar novamente'}:{children:ReactNode;error?:boolean;onRetry?:()=>void;retryLabel?:string}){return <div role={error?'alert':'status'} className={`rounded-xl border bg-paper p-4 text-sm leading-6 text-ink ${error?'border-brand-red/60':'border-brand-blue/50'}`}>{children}{onRetry&&<button type="button" className="mt-3 block min-h-11 rounded-full border border-white/30 px-4 py-2 font-bold" onClick={onRetry}>{retryLabel}</button>}</div>}
