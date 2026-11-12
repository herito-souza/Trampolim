import {useState} from 'react';
/** Bound DOM work without discarding loaded results, export data, or filter coverage. */
export function useProgressiveList<T>(items:T[],scope:string,batch=20){
 const [window,setWindow]=useState({scope,count:batch});
 const count=window.scope===scope?window.count:batch;
 return {items:items.slice(0,count),visible:Math.min(count,items.length),total:items.length,more:()=>setWindow({scope,count:count+batch})};
}
