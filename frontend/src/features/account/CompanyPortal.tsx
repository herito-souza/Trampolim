import { PrivacyRequests } from './PrivacyRequests';
import type { Company,Listing } from './CompanyDashboard';
import {AppLayout} from '../../components/workspace/AppLayout';
import type {ReactNode} from 'react';
import { AccountDataDownload } from './AccountDataDownload';
import { CompanyAccountDeletion } from './CompanyAccountDeletion';
import { useCallback, useEffect, useRef, useState } from 'react';
import { CompanyDashboard } from './CompanyDashboard';
import { lazy } from 'react';
import { AsyncPanel } from '../../components/ui/Feedback';
const AssociationDashboardPage=lazy(()=>import('./AssociationDashboardPage').then(module=>({default:module.AssociationDashboardPage})));
import { api } from './api';
import type { Organization } from './SaasAdminPage';
import { RibbonStrip } from '../../components/ui/RibbonStrip';
import type {UniversalSearchProvider,UniversalSearchResult} from '../../components/workspace/UniversalSearch';

type PortalTab = 'company' | 'association';

/** Routes verified Empresas sessions to company and/or association workspaces. */
export function CompanyPortal({ token, onLogout,organizations: suppliedOrganizations,organizationsError }: { token: string; onLogout: () => void;organizations?:Organization[];organizationsError?:string }) {
  const [initialCompany,setInitialCompany]=useState<Company|null>(null);
  const [companyName,setCompanyName]=useState('');
  const organizations=suppliedOrganizations||[];
  const selectedInitialTab=useRef(false);
  useEffect(()=>{if(suppliedOrganizations&&!selectedInitialTab.current){selectedInitialTab.current=true;setTab(suppliedOrganizations.length?'association':'company')}},[suppliedOrganizations]);
  const [hasCompany, setHasCompany] = useState(false);
  const [tab, setTab] = useState<PortalTab>('company');
  const [loading, setLoading] = useState(true);
  const listingSearchCache=useRef<{token:string;items:Listing[]}|null>(null);
  const error=organizationsError||'';

  useEffect(() => {
    let active = true;const abort=new AbortController();
    setLoading(true);
    api<Company>('/company/dashboard',token,{signal:abort.signal}).then(company=>{
      if(!active)return;
      setHasCompany(true);setInitialCompany(company);setCompanyName(company.tradeName);
    }).catch(()=>{if(active)setHasCompany(false)}).finally(()=>{if(active)setLoading(false)});
    return () => { active = false;abort.abort(); };
  }, [token]);

  const companyChanged=useCallback((company:Company)=>{setInitialCompany(company);setCompanyName(company.tradeName)},[]);
  const privacy = <div className="space-y-3"><AccountDataDownload trail="empresas" token={token} /><PrivacyRequests trail="empresas" token={token} /><CompanyAccountDeletion token={token} onDeleted={onLogout} /></div>;
  const searchCompanyResources:UniversalSearchProvider=async(query,signal)=>{
    const items=listingSearchCache.current?.token===token?listingSearchCache.current.items:await api<Listing[]>('/company/listings',token,{signal});
    if(!signal.aborted)listingSearchCache.current={token,items};
    const needle=query.normalize('NFD').replace(/\p{M}+/gu,'').toLocaleLowerCase('pt-BR');
    return items.filter(item=>(item.title+' '+item.area+' '+item.location).normalize('NFD').replace(/\p{M}+/gu,'').toLocaleLowerCase('pt-BR').includes(needle)).slice(0,10).map((item):UniversalSearchResult=>({id:`company-listing-${item.id}`,category:'Publicações da empresa',title:item.title,description:`${item.kind==='job'?'Vaga':'Curso'} · ${item.publicationStatus==='PUBLISHED'?'Publicado':item.publicationStatus==='AWAITING_PAYMENT'?'Aguardando pagamento':item.publicationStatus==='CLOSED'?'Encerrado':'Rascunho'}`,onSelect:()=>{try{sessionStorage.setItem('trampolim_company_search',item.title)}catch{}setTab('company');window.location.hash='empresa-divulgacoes';window.dispatchEvent(new CustomEvent('trampolim:company-search',{detail:item.title}));}}));
  };
  const onboardingRole=tab==='association'?'Gestor institucional':initialCompany?.role==='OWNER'?'Administrador da empresa':initialCompany?.role==='RECRUITER'?'Recrutador':'Analista';
  const onboardingSteps=tab==='association'?[{title:'Acesse o painel da associação',description:'Consulte as empresas e programas que sua conta está autorizada a acompanhar.',actionLabel:'Abrir associação',onAction:()=>setTab('association')},{title:'Acompanhe a participação',description:'Use os indicadores existentes para acompanhar informações agregadas disponíveis para sua organização.'},{title:'Gerencie sua conta',description:'Consulte as configurações e opções de privacidade no menu do seu perfil.'}]:initialCompany?.role==='OWNER'?[{title:'Revise os dados da empresa',description:'Confira as informações da organização e mantenha os dados de contato atualizados.',actionLabel:'Ver perfil empresarial',onAction:()=>{setTab('company');window.location.hash='empresa-perfil';}},{title:'Publique uma oportunidade',description:'Cadastre uma vaga ou curso pelo painel. A publicação seguirá as regras e validações já existentes.',actionLabel:'Gerenciar publicações',onAction:()=>{setTab('company');window.location.hash='empresa-divulgacoes';}},{title:'Acompanhe o recrutamento',description:'Consulte candidaturas e etapas disponíveis para sua empresa.',actionLabel:'Ver candidaturas',onAction:()=>{setTab('company');window.location.hash='empresa-candidaturas';}}]:[{title:'Conheça seu espaço de trabalho',description:'As opções disponíveis dependem do papel que a empresa concedeu à sua conta.',actionLabel:'Abrir painel',onAction:()=>{setTab('company');window.location.hash='empresa-visao-geral';}},{title:'Acompanhe as atividades autorizadas',description:'Consulte publicações ou candidaturas disponíveis para seu perfil empresarial.',actionLabel:'Ver candidaturas',onAction:()=>{setTab('company');window.location.hash='empresa-candidaturas';}},{title:'Peça ajuda quando precisar',description:'Use o menu do perfil para rever este tutorial ou acesse as opções de conta e privacidade.'}];
  const onboardingIdentity=localStorage.getItem('trampolim_empresas_email')||'';
  const frame=(children:ReactNode)=><AppLayout trail="empresas" title={tab==='association'?'Associação':'Empresas'} userName={companyName||undefined} onLogout={onLogout} onboarding={onboardingIdentity?{identity:onboardingIdentity,role:onboardingRole,steps:onboardingSteps}:undefined} searchProvider={hasCompany&&tab==='company'?searchCompanyResources:undefined} onProfile={hasCompany?()=>{setTab('company');window.location.hash='empresa-perfil'}:undefined} onNotice={hasCompany?()=>{setTab('company');window.location.hash='empresa-metricas'}:undefined} profileActions={privacy} active={tab} groups={[
    ...(organizations.length?[{id:'ambientes',label:'Institucional',items:[...(hasCompany?[{id:'company',label:'Painel da empresa',onSelect:()=>setTab('company')}]:[]),{id:'association',label:organizations.some(item=>item.type==='EDUCATIONAL')?'Instituição / associação':'Associação',onSelect:()=>setTab('association')}]}]:[]),
    {id:'ecossistema',label:'Ecossistema',items:[{id:'home',label:'Início',onSelect:()=>{window.location.hash='inicio'}},{id:'capacita',label:'Capacita',onSelect:()=>{window.location.hash='capacita'}},{id:'opportunities',label:'Oportunidades',onSelect:()=>{window.location.hash='oportunidades'}}]}
  ]}>{children}</AppLayout>;
  if (loading || suppliedOrganizations===undefined) return <><RibbonStrip/><main className="grid min-h-[70vh] place-items-center p-8"><p role="status">Validando seu acesso a Empresas…</p></main></>;
  if (organizations.length && tab === 'association') return frame(<>

    <AsyncPanel><AssociationDashboardPage token={token} onBack={() => hasCompany ? setTab('company') : onLogout()} /></AsyncPanel>
  </>);
  if (hasCompany) return frame(<>

    {error && <p role="alert" className="mx-auto max-w-5xl p-4 text-sm text-amber-200">{error}</p>}
    <CompanyDashboard token={token} initialCompany={initialCompany} onCompanyChange={companyChanged} onLogout={onLogout}/>
  </>);
  return <main className="grid min-h-screen place-items-center p-8 text-center"><section className="max-w-lg">{privacy}<h1 className="text-2xl font-black">Acesso empresarial indisponível</h1><p className="mt-3 text-sm text-ink/60">Sua conta não possui uma empresa vinculada nem acesso a uma associação. Entre em contato com a administração ou use o convite correto.</p><button type="button" onClick={onLogout} className="mt-6 rounded-full border border-white/20 px-5 py-3 font-bold">Sair</button></section></main>;
}
