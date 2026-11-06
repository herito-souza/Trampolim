import {useId,useLayoutEffect,useRef} from 'react';
import {useWorkspace} from './workspaceContext';
import type {NavGroup} from './workspaceContext';
/** Registers existing destinations in the enclosing shell, without duplicating menus or routing. */
export function WorkspaceNavigation({label,groups,active}:{label:string;groups:NavGroup[];active:string}){
 const workspace=useWorkspace(),owner=useId(),latest=useRef(groups);latest.current=groups;
 const register=workspace?.register,remove=workspace?.remove;
 const signature=JSON.stringify(groups.map(group=>({id:group.id,label:group.label,items:group.items.map(({id,label,icon,disabled})=>({id,label,icon,disabled}))})));
 useLayoutEffect(()=>{if(!register||!remove)return;register({owner,label,active,groups:latest.current.map(group=>({...group,items:group.items.map(item=>({...item,onSelect:()=>latest.current.flatMap(g=>g.items).find(i=>i.id===item.id)?.onSelect()}))}))});return()=>remove(owner)},[register,remove,owner,label,active,signature]);
 if(workspace)return null;
 return <nav aria-label={label} className="flex flex-wrap gap-2 border-b border-white/10 p-3">{groups.flatMap(g=>g.items).map(item=><button key={item.id} type="button" disabled={item.disabled} aria-current={active===item.id?'page':undefined} className="min-h-11 rounded-full border border-white/20 px-4 py-2 font-bold" onClick={item.onSelect}>{item.label}</button>)}</nav>;
}
