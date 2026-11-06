import {createContext,useContext} from 'react';
export type NavItem={id:string;label:string;onSelect:()=>void;icon?:string;disabled?:boolean};
export type NavGroup={id:string;label:string;items:NavItem[]};
export type Navigation={owner:string;label:string;groups:NavGroup[];active:string};
export type WorkspaceContextValue={register:(navigation:Navigation)=>void;remove:(owner:string)=>void;notificationHost:HTMLElement|null;returnTarget:()=>HTMLElement|null;select:(item:NavItem)=>void};
export const WorkspaceContext=createContext<WorkspaceContextValue|null>(null);
export const useWorkspace=()=>useContext(WorkspaceContext);

export function navigationIcon(id:string){if(/profile|account|details|team|perfil/.test(id))return 'user';if(/overview|home|opportunities|welcome/.test(id))return 'home';if(/course|learning|library|assessment|adaptive|passport|materials/.test(id))return 'book';if(/job|listing|future|application|career|company|companies/.test(id))return 'work';if(/metric|radar|performance|observatory|admin/.test(id))return 'chart';if(/question|inbox|support/.test(id))return 'help';return 'page';}
