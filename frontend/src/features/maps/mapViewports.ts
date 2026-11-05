import official from './officialViewports.json';
import { validBounds } from './mapModel';
import type { Bounds } from './mapModel';
const stored=new Map<string,Bounds>();
const known=official.viewports as Record<string,number[]>;
export function knownViewport(scope:string):Bounds|null{const value=known[scope];return validBounds(value)?value:null;}
export const nationalViewport=knownViewport('all')!;
export const initialViewport=knownViewport('region:irece-ba')!;
/** Optional geographic framing only. Never produces opportunity coordinates. */
export async function municipalViewport(scope:string,signal:AbortSignal):Promise<Bounds|null>{
 if(knownViewport(scope))return knownViewport(scope);
 if(stored.has(scope))return stored.get(scope)!;
 const path=/^city:[0-9]{7}$/.test(scope)?'municipios/'+scope.slice(5):/^state:[A-Z]{2}$/.test(scope)?'estados/'+scope.slice(6):null;
 if(!path)return null;
 const response=await fetch('https://servicodados.ibge.gov.br/api/v3/malhas/'+path+'?formato=application/vnd.geo+json&qualidade=minima',{signal,credentials:'omit',headers:{Accept:'application/vnd.geo+json'}});
 if(!response.ok)throw new Error('Limites municipais indisponíveis.');
 const raw=await response.text();if(raw.length>2000000)throw new Error('Limites excedem o tamanho permitido.');
 const data=JSON.parse(raw);const points:number[][]=[];
 function coordinates(value:unknown){if(!Array.isArray(value))return;if(value.length>=2&&typeof value[0]==='number'&&typeof value[1]==='number'){points.push(value.slice(0,2));return;}for(const child of value)coordinates(child);}
 const geometries=data.type==='FeatureCollection'?data.features.map((f:{geometry:unknown})=>f.geometry):data.type==='Feature'?[data.geometry]:[data];
 for(const geometry of geometries)coordinates(geometry?.coordinates);
 if(!points.length)throw new Error('Limites geográficos inválidos.');
 const bounds:Bounds=[Infinity,Infinity,-Infinity,-Infinity];for(const p of points){bounds[0]=Math.min(bounds[0],p[0]);bounds[1]=Math.min(bounds[1],p[1]);bounds[2]=Math.max(bounds[2],p[0]);bounds[3]=Math.max(bounds[3],p[1]);}
 if(!validBounds(bounds))throw new Error('Limites geográficos inválidos.');signal.throwIfAborted();stored.set(scope,bounds);while(stored.size>40)stored.delete(stored.keys().next().value!);return bounds;
}
