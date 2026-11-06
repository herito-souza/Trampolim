import type {AccountDestination} from './AccountNavigation';
import type {NavGroup} from '../../components/workspace/workspaceContext';

export function accountGroups(onNavigate:(destination:AccountDestination)=>void,canManage:boolean,hasAssociation:boolean):NavGroup[]{
 const item=(id:AccountDestination,label:string)=>({id,label,onSelect:()=>onNavigate(id)});
 return [
  {id:'principal',label:'Principal',items:[item('opportunities','Oportunidades'),item('jobs','Vagas'),item('courses','Cursos')]},
  {id:'carreira',label:'Carreira e desenvolvimento',items:[item('diagnostic','Diagnóstico'),item('radar','Radar de competências'),item('career','Carreira e projetos'),item('future','Talentos futuros'),item('premium','Premium')]},
  ...(canManage?[{id:'administracao',label:'Administração',items:[item('admin','Administração'),item('admin-capacita','Conteúdos Capacita'),item('admin-saas','Gestão SaaS')]}]:[]),
  ...(hasAssociation?[{id:'institucional',label:'Institucional',items:[item('association','Associação')]}]:[]),
  {id:'conta',label:'Conta',items:[item('profile','Perfil profissional')]},
  {id:'ecossistema',label:'Ecossistema',items:[item('home','Início'),item('companies','Empresas'),item('capacita','Capacita')]}
 ];
}

export const accountViews=['career','future','radar','welcome','details','opportunities','diagnostic','catalog-courses','catalog-jobs','premium','admin','admin-capacita','admin-saas'] as const;
export type AccountView=typeof accountViews[number];
export type AccountSurface=AccountView|'association'|'capacita'|'ecosystem'|'company-entry';
export function isAccountView(value:unknown):value is AccountView{return typeof value==='string'&&(accountViews as readonly string[]).includes(value)}
export function readAccountView(hash:string,stored:string|null):AccountView{if(hash==='#diagnostico')return 'diagnostic';if(hash.startsWith('#oportunidades/')){const view=hash.split('/')[1];return isAccountView(view)?view:'opportunities'}return isAccountView(stored)?stored:'welcome'}
