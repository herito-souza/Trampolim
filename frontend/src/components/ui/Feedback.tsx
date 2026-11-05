import { Suspense } from 'react';
import type { ReactNode } from 'react';
export function LoadingStatus({label='Carregando conteúdo…'}:{label?:string}){return <p role="status" aria-live="polite" className="flex min-h-24 items-center rounded-2xl border border-white/15 bg-surface p-4 text-sm font-semibold text-ink">{label}</p>}
export function AsyncPanel({children,label}:{children:ReactNode;label?:string}){return <Suspense fallback={<LoadingStatus label={label}/>}>{children}</Suspense>}
export function InlineNotice({children,error=false,onRetry,retryLabel='Tentar novamente'}:{children:ReactNode;error?:boolean;onRetry?:()=>void;retryLabel?:string}){return <div role={error?'alert':'status'} className={`rounded-xl border bg-paper p-4 text-sm leading-6 text-ink ${error?'border-brand-red/60':'border-brand-blue/50'}`}>{children}{onRetry&&<button type="button" className="mt-3 block min-h-11 rounded-full border border-white/30 px-4 py-2 font-bold" onClick={onRetry}>{retryLabel}</button>}</div>}
