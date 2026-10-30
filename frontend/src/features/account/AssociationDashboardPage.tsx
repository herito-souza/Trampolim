import { ObservatoryPanel } from '../career/ObservatoryPanel';
import { HubPanel } from '../hub/HubPanel';
import { useCallback, useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from './api';
import { RibbonStrip } from '../../components/ui/RibbonStrip';
import type { Organization } from './SaasAdminPage';

type AssociationDashboard = { organizationId: number; name: string; companies: number; activeCompanies: number; openJobs: number; openInternships: number; applications: number; publicLearningContents: number | null };
type OrganizationMember = { userId: number; name: string; email: string; role: string; createdAt: string };
type CompanyOption = { companyId: number; name: string; city: string; state: string; industry: string };

export function AssociationDashboardPage({ token, onBack }: { token: string; onBack: () => void }) {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [dashboard, setDashboard] = useState<AssociationDashboard | null>(null);
  const [members, setMembers] = useState<OrganizationMember[]>([]);
  const [companies, setCompanies] = useState<CompanyOption[]>([]);
  const [companyLinks, setCompanyLinks] = useState<{ companyId: number; name: string; status: string }[]>([]);
  const [canManage, setCanManage] = useState(false);
  const [memberEmail, setMemberEmail] = useState('');
  const [memberInviteUrl, setMemberInviteUrl] = useState('');
  const [companyId, setCompanyId] = useState('');
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadVersion = useRef(0);
  const load = useCallback(async (id: number) => {
    const version=++loadVersion.current;setLoading(true); setError('');setDashboard(null);setMembers([]);setCompanies([]);setCompanyLinks([]);setCanManage(false);
    try {
      const nextDashboard=await api<AssociationDashboard>(`/empresas/association/organizations/${id}/dashboard`, token);if(version!==loadVersion.current)return;setDashboard(nextDashboard);
      try {
        const [nextMembers, nextCompanies, nextLinks] = await Promise.all([
          api<OrganizationMember[]>(`/empresas/association/organizations/${id}/members`, token),
          api<CompanyOption[]>(`/empresas/association/organizations/${id}/available-companies`, token),
          api<{ companyId: number; name: string; status: string }[]>(`/empresas/association/organizations/${id}/companies`, token),
        ]);
        if(version!==loadVersion.current)return;setMembers(nextMembers); setCompanies(nextCompanies); setCompanyLinks(nextLinks); setCanManage(true);
      } catch { if(version!==loadVersion.current)return;setMembers([]); setCompanies([]); setCompanyLinks([]); setCanManage(false); }
    }
    catch (cause) { if(version===loadVersion.current){setError(cause instanceof Error ? cause.message : 'Não foi possível carregar os indicadores.'); setDashboard(null);} }
    finally { if(version===loadVersion.current)setLoading(false); }
  }, [token]);

  async function addMember(event: FormEvent) {
    event.preventDefault(); if (!selectedId) return;
    setBusy(true); setError('');
    try { const invite = await api<{ inviteUrl: string }>(`/empresas/association/organizations/${selectedId}/members/invites`, token, { method: 'POST', body: JSON.stringify({ email: memberEmail, role: 'ASSOCIATION_MEMBER' }) }); setMemberInviteUrl(invite.inviteUrl); setMemberEmail(''); await load(selectedId); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível adicionar o membro.'); }
    finally { setBusy(false); }
  }

  async function removeMember(member: OrganizationMember) {
    if (!selectedId || !window.confirm(`Remover ${member.name} da associação?`)) return;
    setBusy(true); setError('');
    try { await api(`/empresas/association/organizations/${selectedId}/members/${member.userId}`, token, { method: 'DELETE' }); await load(selectedId); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível remover o membro.'); }
    finally { setBusy(false); }
  }

  async function requestCompany(event: FormEvent) {
    event.preventDefault(); if (!selectedId || !companyId) return;
    setBusy(true); setError('');
    try { await api(`/empresas/association/organizations/${selectedId}/companies`, token, { method: 'POST', body: JSON.stringify({ companyId: Number(companyId), status: 'PENDING' }) }); setCompanyId(''); await load(selectedId); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível solicitar o vínculo.'); }
    finally { setBusy(false); }
  }

  useEffect(() => {
    let active = true;
    api<Organization[]>('/empresas/association/organizations', token).then(items => {
      if (!active) return;
      setOrganizations(items);
      if (items.length) { setSelectedId(items[0].id); void load(items[0].id); }
      else { setLoading(false); setError('Sua conta não tem acesso a um painel de associação.'); }
    }).catch(cause => { if (active) { setLoading(false); setError(cause instanceof Error ? cause.message : 'Não foi possível validar seu acesso.'); } });
    return () => { active = false; };
  }, [token, load]);

  const metrics = dashboard ? [
    ['Empresas participantes', dashboard.companies, `${dashboard.activeCompanies} ativas`],
    ['Vagas de emprego abertas', dashboard.openJobs, 'Publicadas pelas empresas vinculadas'],
    ['Vagas de estágio abertas', dashboard.openInternships, 'Disponíveis no Capacita'],
    ['Candidaturas registradas', dashboard.applications, 'Contagem agregada, sem dados pessoais'],
    ['Conteúdos de aprendizagem — catálogo geral', dashboard.publicLearningContents, 'Materiais públicos de toda a plataforma; não são conclusões da associação'],
  ] : [];

  return <><RibbonStrip/><header className="border-b border-white/10 bg-[#0b1421] px-[6vw] py-5"><div className="mx-auto flex max-w-7xl items-center justify-between gap-4"><div><p className="text-xs font-extrabold uppercase tracking-widest text-brand-blue">Área institucional</p><h1 className="mt-1 text-2xl font-black">Painel da associação</h1></div><button onClick={onBack} className="min-h-11 rounded-full border border-white/20 px-5 py-2 text-sm font-bold">Voltar</button></div></header>
    <main className="mx-auto max-w-7xl px-[6vw] py-10">{organizations.length > 1 && <label className="block max-w-xl text-sm font-bold">Associação<select value={selectedId || ''} onChange={event => { const id = Number(event.target.value); setSelectedId(id); void load(id); }} className="mt-2 w-full rounded-xl border border-white/15 bg-[#121c2b] px-4 py-3">{organizations.map(org => <option key={org.id} value={org.id}>{org.name}</option>)}</select></label>}{selectedId && <HubPanel key={selectedId} token={token} organizationId={selectedId}/>}<h2 className="mt-8 text-xl font-bold">Gestão e indicadores básicos existentes</h2>{error && <p role="alert" className="mt-5 rounded-xl border border-red-300/20 bg-red-400/10 p-4 text-sm font-bold">{error}</p>}{loading ? <p role="status" className="mt-6">Carregando indicadores agregados…</p> : dashboard && <><div className="mt-3"><p className="text-xs font-extrabold uppercase tracking-widest text-brand-blue">{dashboard.name}</p><h2 className="mt-2 text-3xl font-black">Acompanhamento institucional</h2><p className="mt-2 max-w-3xl text-sm leading-6 text-ink/55">Indicadores agregados das empresas vinculadas. Este painel não mostra currículos, contatos, candidaturas individuais ou informações comerciais privadas.</p></div><div className="mt-7 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{metrics.map(([label, value, detail]) => <article key={label} className="rounded-2xl border border-white/10 bg-[#121c2b] p-6"><p className="text-sm font-bold text-ink/55">{label}</p><p className="mt-2 text-4xl font-black">{value == null ? 'Indisponível' : Number(value).toLocaleString('pt-BR')}</p><p className="mt-2 text-xs text-ink/50">{detail}</p></article>)}</div>{dashboard.companies === 0 && <p className="mt-6 rounded-2xl border border-dashed border-white/15 p-6 text-sm text-ink/55">Ainda não há empresas vinculadas.</p>}{canManage && <section className="mt-10 grid gap-5 lg:grid-cols-2"><form onSubmit={requestCompany} className="space-y-3 rounded-2xl border border-white/10 bg-[#121c2b] p-5"><h3 className="text-xl font-black">Solicitar participação de uma empresa</h3><p className="text-sm text-ink/55">A solicitação fica pendente até a revisão da administração da Trampolim.</p><label className="block text-sm font-bold">Empresa<select required value={companyId} onChange={event => setCompanyId(event.target.value)} className="mt-2 w-full rounded-xl border border-white/15 bg-[#0b1421] px-4 py-3"><option value="">Selecione uma empresa pública</option>{companies.map(company => <option key={company.companyId} value={company.companyId}>{company.name} · {company.city}/{company.state} · {company.industry}</option>)}</select></label><button disabled={busy || !companies.length} className="rounded-full bg-brand-blue px-5 py-3 text-sm font-extrabold text-[#101725] disabled:opacity-50">Solicitar vínculo</button></form><form onSubmit={addMember} className="space-y-3 rounded-2xl border border-white/10 bg-[#121c2b] p-5"><h3 className="text-xl font-black">Convidar membro da associação</h3><p className="text-sm text-ink/55">A pessoa receberá acesso por convite e criará uma conta independente em Empresas.</p><label className="block text-sm font-bold">E-mail<input required type="email" maxLength={180} value={memberEmail} onChange={event => setMemberEmail(event.target.value)} className="mt-2 w-full rounded-xl border border-white/15 bg-[#0b1421] px-4 py-3"/></label><button disabled={busy} className="rounded-full bg-brand-blue px-5 py-3 text-sm font-extrabold text-[#101725] disabled:opacity-50">Criar convite</button>{memberInviteUrl && <p role="status" className="break-all rounded-xl border border-brand-blue/30 p-3 text-xs">Convite criado: <a className="underline" href={memberInviteUrl}>{memberInviteUrl}</a></p>}</form><section className="rounded-2xl border border-white/10 bg-[#121c2b] p-5 lg:col-span-2"><h3 className="text-xl font-black">Empresas e vínculos</h3>{companyLinks.length ? <div className="mt-3 divide-y divide-white/10">{companyLinks.map(company => <p key={company.companyId} className="py-3 text-sm">{company.name} · {company.status === 'ACTIVE' ? 'Vínculo ativo' : company.status === 'PENDING' ? 'Aguardando aprovação' : 'Vínculo encerrado'}</p>)}</div> : <p className="mt-2 text-sm text-ink/55">Nenhum vínculo cadastrado.</p>}<h4 className="mt-5 font-bold">Membros</h4>{members.length ? <ul className="mt-2 divide-y divide-white/10">{members.map(member => <li key={member.userId} className="flex flex-wrap items-center justify-between gap-3 py-3 text-sm"><span>{member.name} · {member.email} · {member.role === 'ASSOCIATION_ADMIN' ? 'Administrador' : 'Membro'}</span>{member.role !== 'ASSOCIATION_ADMIN' && <button type="button" disabled={busy} onClick={() => void removeMember(member)} className="rounded-full border border-white/20 px-3 py-2 text-xs font-bold">Remover</button>}</li>)}</ul> : <p className="mt-2 text-sm text-ink/55">Nenhum membro cadastrado.</p>}</section></section>}</>}<details className="mt-6 rounded-2xl border border-white/15 p-5"><summary className="cursor-pointer font-bold">Observatório público da plataforma (amostra geral)</summary><p className="my-3">Estes dados públicos não representam exclusivamente sua associação.</p><ObservatoryPanel/></details></main></>;
}
