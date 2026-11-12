import type { Company } from './CompanyDashboard';
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
  const privacy = <div className="space-y-3"><AccountDataDownload trail="empresas" token={token} /><CompanyAccountDeletion token={token} onDeleted={onLogout} /></div>;
  const frame=(children:ReactNode)=><AppLayout trail="empresas" title={tab==='association'?'Associação':'Empresas'} userName={companyName||undefined} onLogout={onLogout} onProfile={hasCompany?()=>{setTab('company');window.location.hash='empresa-perfil'}:undefined} onNotice={hasCompany?()=>{setTab('company');window.location.hash='empresa-metricas'}:undefined} profileActions={privacy} active={tab} groups={[
    ...(organizations.length?[{id:'ambientes',label:'Institucional',items:[...(hasCompany?[{id:'company',label:'Painel da empresa',onSelect:()=>setTab('company')}]:[]),{id:'association',label:'Associação',onSelect:()=>setTab('association')}]}]:[]),
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
