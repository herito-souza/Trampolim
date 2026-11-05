import type { FeatureCollection, Point } from 'geojson';
export type MapOpportunity = { id: number; title: string; provider: string; location: string; modality: string; kind: 'job'|'course'; url: string; latitude: number; longitude: number };
export type MapRecord = { id:number; title:string; provider:string; location?:string|null; municipalityLabel?:string|null; modality?:string|null; url:string; latitude?:number|null; longitude?:number|null; active?:boolean; moderationStatus?:string|null; expiresAt?:string|null };
export type Bounds = [number,number,number,number];
export const MAX_MAP_POINTS=50;
export function validCoordinates(latitude:unknown,longitude:unknown):boolean{return typeof latitude==='number'&&typeof longitude==='number'&&Number.isFinite(latitude)&&Number.isFinite(longitude)&&Math.abs(latitude)<=85.05112878&&Math.abs(longitude)<=180;}
export function safeMapUrl(raw:string):string|null{try{const url=new URL(raw);return ['https:','http:'].includes(url.protocol)?url.href:null}catch{return null}}
export function mapOpportunities(rows:MapRecord[],kind:'job'|'course',today=new Intl.DateTimeFormat('sv-SE',{timeZone:'America/Sao_Paulo'}).format(new Date())):MapOpportunity[]{
 const unique=new Map<number,MapOpportunity>();
 for(const row of rows){const mode=(row.modality||'').normalize('NFD').replace(/\p{M}/gu,'').toLowerCase().trim();
 if(row.active===false||row.moderationStatus&&row.moderationStatus!=='APPROVED'||row.expiresAt&&row.expiresAt.slice(0,10)<today||!validCoordinates(row.latitude,row.longitude)||['remoto','online'].includes(mode)||kind==='course'&&mode!=='presencial')continue;
 unique.set(row.id,{id:row.id,title:row.title,provider:row.provider,location:row.municipalityLabel||row.location||'Localidade não informada',modality:row.modality||'Modalidade não informada',kind,url:row.url,latitude:row.latitude!,longitude:row.longitude!});if(unique.size>=MAX_MAP_POINTS)break;
 }
 return [...unique.values()];
}
export function featureCollection(items:MapOpportunity[]):FeatureCollection<Point>{return {type:'FeatureCollection',features:items.slice(0,MAX_MAP_POINTS).map(item=>({type:'Feature',id:item.id,properties:{id:item.id,kind:item.kind},geometry:{type:'Point',coordinates:[item.longitude,item.latitude]}}))};}
export function pointBounds(items:MapOpportunity[]):Bounds|null{return items.length?[Math.min(...items.map(i=>i.longitude)),Math.min(...items.map(i=>i.latitude)),Math.max(...items.map(i=>i.longitude)),Math.max(...items.map(i=>i.latitude))]:null;}
export function validBounds(value:unknown):value is Bounds{return Array.isArray(value)&&value.length===4&&value.every(n=>typeof n==='number'&&Number.isFinite(n))&&value[0]<value[2]&&value[1]<value[3]&&value[0]>=-180&&value[2]<=180&&value[1]>=-85.05112878&&value[3]<=85.05112878;}
