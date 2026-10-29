import { AccountDataDownload } from './AccountDataDownload';
import { CompanyAccountDeletion } from './CompanyAccountDeletion';
import { useEffect, useState } from 'react';
import { CompanyDashboard } from './CompanyDashboard';
import { AssociationDashboardPage } from './AssociationDashboardPage';
import { api } from './api';
import type { Organization } from './SaasAdminPage';
import { RibbonStrip } from '../../components/ui/RibbonStrip';

type PortalTab = 'company' | 'association';

/** Routes verified Empresas sessions to company and/or association workspaces. */
export function CompanyPortal({ token, onLogout }: { token: string; onLogout: () => void }) {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [hasCompany, setHasCompany] = useState(false);
  const [tab, setTab] = useState<PortalTab>('company');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.allSettled([
      api<Organization[]>('/empresas/association/organizations', token),
      api('/company/dashboard', token),
    ]).then(([associations, company]) => {
      if (!active) return;
      if (associations.status === 'fulfilled') setOrganizations(associations.value);
      else setError(associations.reason instanceof Error ? associations.reason.message : 'Não foi possível consultar sua organização.');
      setHasCompany(company.status === 'fulfilled');
      setTab(associations.status === 'fulfilled' && associations.value.length ? 'association' : 'company');
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [token]);

  const privacy = <div className="flex flex-wrap items-start justify-end gap-3 border-b border-white/10 px-6 py-3"><AccountDataDownload trail="empresas" token={token} /><CompanyAccountDeletion token={token} onDeleted={onLogout} /></div>;
  if (loading) return <><RibbonStrip/><main className="grid min-h-[70vh] place-items-center p-8"><p role="status">Validando seu acesso a Empresas…</p></main></>;
  if (organizations.length && tab === 'association') return <>{privacy}
    {hasCompany && <nav aria-label="Ambientes Empresas" className="flex justify-center gap-3 border-b border-white/10 bg-[#0b1421] p-3"><button type="button" onClick={() => setTab('company')} className="rounded-full border border-white/20 px-4 py-2 text-sm font-bold">Painel da empresa</button><button type="button" aria-current="page" className="rounded-full bg-brand-blue px-4 py-2 text-sm font-bold text-[#101725]">Associação</button></nav>}
    <AssociationDashboardPage token={token} onBack={() => hasCompany ? setTab('company') : onLogout()} />
  </>;
  if (hasCompany) return <>{privacy}
    {organizations.length > 0 && <nav aria-label="Ambientes Empresas" className="flex justify-center gap-3 border-b border-white/10 bg-[#0b1421] p-3"><button type="button" aria-current="page" className="rounded-full bg-brand-blue px-4 py-2 text-sm font-bold text-[#101725]">Painel da empresa</button><button type="button" onClick={() => setTab('association')} className="rounded-full border border-white/20 px-4 py-2 text-sm font-bold">Associação</button></nav>}
    {error && <p role="alert" className="mx-auto max-w-5xl p-4 text-sm text-amber-200">{error}</p>}
    <CompanyDashboard token={token} onLogout={onLogout}/>
  </>;
  return <main className="grid min-h-screen place-items-center p-8 text-center"><section className="max-w-lg">{privacy}<h1 className="text-2xl font-black">Acesso empresarial indisponível</h1><p className="mt-3 text-sm text-ink/60">Sua conta não possui uma empresa vinculada nem acesso a uma associação. Entre em contato com a administração ou use o convite correto.</p><button type="button" onClick={onLogout} className="mt-6 rounded-full border border-white/20 px-5 py-3 font-bold">Sair</button></section></main>;
}
