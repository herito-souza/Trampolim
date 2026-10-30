import { CareerCostForm } from '../career/CareerExtras';
import { HubAdminPanel } from '../hub/HubAdminPanel';
import { TrailAccountAdmin } from './TrailAccountAdmin';
import { useCallback, useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from './api';
import { RibbonStrip } from '../../components/ui/RibbonStrip';

type MonthlyRevenue = { month: string; paidPayments: number; revenueCents: number };
type Overview = { users: number; verifiedUsers: number; activeUsers30d: number; companies: number; associations: number; associationLinks: number; publishedJobs: number; publishedInternships: number; publishedLearningContents: number; applications: number; pendingPublications: number; paidInternshipPayments: number; internshipRevenueCents: number; revenueByMonth: MonthlyRevenue[] };
export type Organization = { id: number; name: string; type: string; status: string; legacyCompanyId: number | null; members: number; linkedCompanies: number; createdAt: string };
type LinkedCompany = { companyId: number; name: string; status: string };
type PlatformAdmin = { userId: number; name: string; email: string; grantedAt: string };
type LegacyMigrationPreview = { sourceCompanies: number; alreadyImported: number; importable: number; unmatchedOwners: number; invalidCnpj: number; targetCnpjConflicts: number; targetOwnerConflicts: number };
type LegacyMigrationResult = { imported: number; alreadyImported: number; companyErrors: string[] };
type LegacyOrganizationPreview = { organizations: number; alreadyImported: number; memberships: number; relationships: number; relationshipsWithoutMigratedCompany: number };
type LegacyOrganizationResult = { importedOrganizations: number; importedMemberships: number; importedRelationships: number; organizationErrors: string[] };
type OrganizationInvite = { email: string; inviteUrl: string };

const field = 'mt-2 w-full rounded-xl border border-white/15 bg-[#0b1421] px-4 py-3 text-ink';
const organizationTypes = [{ value: 'ASSOCIATION', label: 'Associação empresarial' }, { value: 'EDUCATIONAL', label: 'Instituição educacional' }, { value: 'QUALIFICATION', label: 'Parceiro de qualificação' }];
const statusNames: Record<string, string> = { ACTIVE: 'Ativa', PENDING: 'Pendente', SUSPENDED: 'Suspensa' };

export function SaasAdminPage({ token, onBack }: { token: string; onBack: () => void }) {
  const [overview, setOverview] = useState<Overview | null>(null);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [organizationOffset, setOrganizationOffset] = useState(0);
  const [companyLinks, setCompanyLinks] = useState<Record<number, LinkedCompany[]>>({});
  const [platformAdmins, setPlatformAdmins] = useState<PlatformAdmin[]>([]);
  const [legacyMigrationPreview, setLegacyMigrationPreview] = useState<LegacyMigrationPreview | null>(null);
  const [legacyMigrationResult, setLegacyMigrationResult] = useState<LegacyMigrationResult | null>(null);
  const [legacyOrganizationPreview, setLegacyOrganizationPreview] = useState<LegacyOrganizationPreview | null>(null);
  const [legacyOrganizationResult, setLegacyOrganizationResult] = useState<LegacyOrganizationResult | null>(null);
  const [organizationInviteUrl, setOrganizationInviteUrl] = useState('');
  const [adminEmail, setAdminEmail] = useState('');
  const [orgForm, setOrgForm] = useState({ name: '', type: 'ASSOCIATION' });
  const [linkForm, setLinkForm] = useState({ associationId: '', companyId: '' });
  const [memberForm, setMemberForm] = useState({ organizationId: '', email: '', role: 'ASSOCIATION_ADMIN' });
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const [stats, orgs, admins] = await Promise.all([api<Overview>('/admin/saas/overview', token), api<Organization[]>(`/admin/saas/organizations?offset=${organizationOffset}&limit=50`, token), api<PlatformAdmin[]>('/admin/saas/platform-roles', token)]);
      setOverview(stats); setOrganizations(orgs); setPlatformAdmins(admins);
      setCompanyLinks({});
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível carregar a administração SaaS.'); }
    finally { setLoading(false); }
  }, [token, organizationOffset]);

  useEffect(() => { void load(); }, [load]);

  const associations = useMemo(() => organizations.filter(org => org.type === 'ASSOCIATION'), [organizations]);
  const companies = useMemo(() => organizations.filter(org => org.type === 'COMPANY' && org.legacyCompanyId), [organizations]);
  const money = (cents: number) => (cents / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

  async function createOrganization(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('');
    try { await api('/admin/saas/organizations', token, { method: 'POST', body: JSON.stringify(orgForm) }); setOrgForm({ ...orgForm, name: '' }); setNotice('Organização cadastrada como pendente de ativação.'); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível cadastrar a organização.'); }
    finally { setBusy(false); }
  }

  async function linkCompany(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('');
    try { await api(`/admin/saas/organizations/${linkForm.associationId}/companies`, token, { method: 'POST', body: JSON.stringify({ companyId: Number(linkForm.companyId), status: 'PENDING' }) }); setNotice('Vínculo atualizado.'); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível solicitar o vínculo.'); }
    finally { setBusy(false); }
  }

  async function changeOrganizationStatus(org: Organization) {
    const status = org.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'; setBusy(true); setError(''); setNotice('');
    try { await api(`/admin/saas/organizations/${org.id}/status`, token, { method: 'PUT', body: JSON.stringify({ status }) }); setNotice(`Organização ${status === 'ACTIVE' ? 'ativada' : 'suspensa'}.`); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível atualizar o status.'); }
    finally { setBusy(false); }
  }

  async function changeLinkStatus(orgId: number, companyId: number, status: string) {
    setBusy(true); setError(''); setNotice('');
    try { await api(`/admin/saas/organizations/${orgId}/companies/${companyId}`, token, { method: 'PUT', body: JSON.stringify({ status }) }); setNotice('Vínculo atualizado.'); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível atualizar o vínculo.'); }
    finally { setBusy(false); }
  }

  async function addMember(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('');
    try { const invite = await api<OrganizationInvite>(`/admin/saas/organizations/${memberForm.organizationId}/members`, token, { method: 'POST', body: JSON.stringify({ email: memberForm.email, role: memberForm.role }) }); setOrganizationInviteUrl(invite.inviteUrl); setMemberForm({ ...memberForm, email: '' }); setNotice('Convite criado para cadastro independente na área Empresas.'); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível vincular a pessoa.'); }
    finally { setBusy(false); }
  }

  async function grantPlatformAdmin(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('');
    try { await api('/admin/saas/platform-roles', token, { method: 'POST', body: JSON.stringify({ email: adminEmail, role: 'SUPER_ADMIN' }) }); setAdminEmail(''); setNotice('Papel de superadministrador atribuído.'); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível atribuir o papel.'); }
    finally { setBusy(false); }
  }

  async function revokePlatformAdmin(admin: PlatformAdmin) {
    if (!window.confirm(`Remover o papel de superadministrador de ${admin.name}?`)) return;
    setBusy(true); setError(''); setNotice('');
    try { await api(`/admin/saas/platform-roles/${admin.userId}`, token, { method: 'DELETE' }); setNotice('Papel removido.'); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível remover o papel.'); }
    finally { setBusy(false); }
  }

  async function previewLegacyCompanies() {
    setBusy(true); setError(''); setNotice(''); setLegacyMigrationResult(null);
    try {
      const result = await api<LegacyMigrationPreview>('/admin/empresas/legacy-migration/preview', token);
      setLegacyMigrationPreview(result);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível conferir os dados empresariais legados.'); }
    finally { setBusy(false); }
  }

  async function executeLegacyCompanyMigration() {
    if (!legacyMigrationPreview || legacyMigrationPreview.importable < 1) return;
    const accepted = window.confirm(`Importar ${legacyMigrationPreview.importable} empresa(s) elegível(is)? A importação preserva os dados antigos, cria contas pendentes sem copiar senhas e mantém anúncios como rascunho.`);
    if (!accepted) return;
    setBusy(true); setError(''); setNotice('');
    try {
      const result = await api<LegacyMigrationResult>('/admin/empresas/legacy-migration/execute', token, { method: 'POST' });
      setLegacyMigrationResult(result); setLegacyMigrationPreview(null);
      setNotice(`${result.imported} empresa(s) importada(s); ${result.alreadyImported} já estavam importadas.`);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível importar os dados empresariais legados.'); }
    finally { setBusy(false); }
  }

  async function previewLegacyOrganizations() {
    setBusy(true); setError(''); setNotice(''); setLegacyOrganizationResult(null);
    try {
      setLegacyOrganizationPreview(await api<LegacyOrganizationPreview>('/admin/empresas/legacy-organizations/preview', token));
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível conferir associações e parceiros legados.'); }
    finally { setBusy(false); }
  }

  async function executeLegacyOrganizationMigration() {
    if (!legacyOrganizationPreview || legacyOrganizationPreview.organizations <= legacyOrganizationPreview.alreadyImported) return;
    if (legacyOrganizationPreview.relationshipsWithoutMigratedCompany > 0) {
      setError('Importe primeiro as empresas elegíveis e resolva os vínculos sem empresa correspondente.');
      return;
    }
    const pending = legacyOrganizationPreview.organizations - legacyOrganizationPreview.alreadyImported;
    const accepted = window.confirm(`Importar ${pending} organização(ões) e seus vínculos? Os membros serão preservados por e-mail, sem copiar senhas ou sessões; a origem não será apagada.`);
    if (!accepted) return;
    setBusy(true); setError(''); setNotice('');
    try {
      const result = await api<LegacyOrganizationResult>('/admin/empresas/legacy-organizations/execute', token, { method: 'POST' });
      setLegacyOrganizationResult(result); setLegacyOrganizationPreview(null);
      setNotice(`${result.importedOrganizations} organização(ões), ${result.importedMemberships} vínculo(s) de membro e ${result.importedRelationships} relação(ões) importados.`);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível importar as organizações legadas.'); }
    finally { setBusy(false); }
  }

  const cards: [string, number, string][] = overview ? [
    ['Usuários cadastrados', overview.users, `${overview.verifiedUsers} e-mails confirmados · ${overview.activeUsers30d} acessaram nos últimos 30 dias`],
    ['Organizações', overview.companies + overview.associations, `${overview.companies} empresas · ${overview.associations} associações · ${overview.associationLinks} vínculos ativos`],
    ['Oportunidades abertas', overview.publishedJobs + overview.publishedInternships, `${overview.publishedJobs} empregos · ${overview.publishedInternships} estágios`],
    ['Conteúdos publicados', overview.publishedLearningContents, 'Cursos e materiais aprovados disponíveis'],
    ['Candidaturas registradas', overview.applications, 'Contagem agregada, sem dados pessoais'],
    ['Publicações pendentes', overview.pendingPublications, 'Aguardando pagamento ou moderação'],
    ['Pagamentos de estágio', overview.paidInternshipPayments, `Receita confirmada: ${money(overview.internshipRevenueCents)}`],
  ] : [];

  return <><RibbonStrip/><header className="border-b border-white/10 bg-[#0b1421] px-[6vw] py-5"><div className="mx-auto flex max-w-7xl items-center justify-between gap-4"><div><p className="text-xs font-extrabold uppercase tracking-widest text-brand-yellow">Administração da plataforma</p><h1 className="mt-1 text-2xl font-black">Gestão SaaS</h1></div><button type="button" onClick={onBack} className="min-h-11 rounded-full border border-white/20 px-5 py-2 text-sm font-bold">Voltar</button></div></header>
    <main className="mx-auto max-w-7xl space-y-10 px-[6vw] py-10">{error && <p role="alert" className="rounded-xl border border-red-300/20 bg-red-400/10 p-4 text-sm font-bold">{error}</p>}{notice && <p role="status" className="rounded-xl bg-brand-green/10 p-4 text-sm font-bold text-brand-green">{notice}</p>}
      <section><div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xs font-extrabold uppercase tracking-widest text-brand-blue">Indicadores reais</p><h2 className="mt-1 text-3xl font-black">Visão geral</h2></div><button onClick={() => void load()} disabled={loading} className="rounded-full border border-white/20 px-4 py-2 text-sm font-bold disabled:opacity-50">Atualizar</button></div>{loading ? <p role="status" className="mt-5">Carregando indicadores…</p> : overview && <><div className="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{cards.map(([label, value, description]) => <article key={label} className="rounded-2xl border border-white/10 bg-[#121c2b] p-5"><p className="text-sm font-bold text-ink/55">{label}</p><p className="mt-2 text-3xl font-black">{value.toLocaleString('pt-BR')}</p><p className="mt-2 text-xs leading-5 text-ink/50">{description}</p></article>)}</div><section className="mt-5 rounded-2xl border border-white/10 bg-[#121c2b] p-5"><h3 className="font-extrabold">Receita confirmada por mês</h3><div className="mt-4 grid grid-cols-6 gap-3">{overview.revenueByMonth.map(item => { const max = Math.max(1, ...overview.revenueByMonth.map(month => month.revenueCents)); return <div key={item.month} className="text-center"><div className="flex h-24 items-end justify-center"><div title={money(item.revenueCents)} className="w-full max-w-14 rounded-t-lg bg-brand-yellow" style={{ height: `${Math.max(4, (item.revenueCents / max) * 100)}%` }}/></div><p className="mt-2 text-[10px] text-ink/55">{item.month.slice(5)}</p><p className="text-[10px] font-bold">{money(item.revenueCents)}</p></div>; })}</div><p className="mt-3 text-xs text-ink/45">Somente pagamentos confirmados pelo provedor.</p></section></>}</section>
      <HubAdminPanel token={token}/>
      <section><h2 className="text-2xl font-black">Organizações</h2><form onSubmit={createOrganization} className="mt-4 grid gap-3 rounded-2xl border border-white/10 bg-[#121c2b] p-5 md:grid-cols-[1fr_1fr_auto] md:items-end"><label className="text-sm font-bold">Nome da organização<input required maxLength={180} value={orgForm.name} onChange={event => setOrgForm({ ...orgForm, name: event.target.value })} className={field}/></label><label className="text-sm font-bold">Tipo<select value={orgForm.type} onChange={event => setOrgForm({ ...orgForm, type: event.target.value })} className={field}>{organizationTypes.map(type => <option key={type.value} value={type.value}>{type.label}</option>)}</select></label><button disabled={busy} className="min-h-11 rounded-full bg-brand-yellow px-5 py-3 font-extrabold text-[#101725] disabled:opacity-50">Cadastrar</button></form>
        {loading ? <p className="mt-4">Carregando organizações…</p> : organizations.length ? <div className="mt-4 overflow-x-auto rounded-2xl border border-white/10"><table className="w-full min-w-[650px] text-left text-sm"><thead className="bg-[#121c2b] text-xs uppercase text-ink/50"><tr><th className="p-4">Organização</th><th className="p-4">Tipo</th><th className="p-4">Status</th><th className="p-4">Membros</th><th className="p-4">Empresas vinculadas</th><th className="p-4">Ação</th></tr></thead><tbody>{organizations.map(org => <tr key={org.id} className="border-t border-white/10"><td className="p-4 font-bold">{org.name}</td><td className="p-4">{organizationTypes.find(type => type.value === org.type)?.label || 'Empresa'}</td><td className="p-4">{statusNames[org.status] || org.status}</td><td className="p-4">{org.members}</td><td className="p-4">{org.linkedCompanies}</td><td className="p-4">{org.type !== 'COMPANY' && <button disabled={busy} onClick={() => void changeOrganizationStatus(org)} className="rounded-full border border-white/20 px-3 py-2 text-xs font-bold">{org.status === 'ACTIVE' ? 'Suspender' : 'Ativar'}</button>}</td></tr>)}</tbody></table></div> : <p className="mt-4 rounded-2xl border border-dashed border-white/15 p-5 text-sm text-ink/55">Ainda não há organizações cadastradas.</p>}</section>
      <section className="space-y-5"><div className="grid gap-5 lg:grid-cols-2"><form onSubmit={linkCompany} className="space-y-3 rounded-2xl border border-white/10 bg-[#121c2b] p-5"><h3 className="text-xl font-black">Vincular empresa a associação</h3><p className="text-sm text-ink/55">A solicitação começa pendente. Somente a administração global pode ativar o vínculo.</p><label className="block text-sm font-bold">Associação<select required value={linkForm.associationId} onChange={event => setLinkForm({ ...linkForm, associationId: event.target.value })} className={field}><option value="">Selecione</option>{associations.map(org => <option key={org.id} value={org.id}>{org.name}</option>)}</select></label><label className="block text-sm font-bold">Empresa<select required value={linkForm.companyId} onChange={event => setLinkForm({ ...linkForm, companyId: event.target.value })} className={field}><option value="">Selecione</option>{companies.map(org => <option key={org.legacyCompanyId} value={org.legacyCompanyId ?? ''}>{org.name}</option>)}</select></label><button disabled={busy || !associations.length || !companies.length} className="rounded-full bg-brand-blue px-5 py-3 text-sm font-extrabold text-[#101725] disabled:opacity-50">Solicitar vínculo</button></form>
        <form onSubmit={addMember} className="space-y-3 rounded-2xl border border-white/10 bg-[#121c2b] p-5"><h3 className="text-xl font-black">Convidar usuário institucional</h3><p className="text-sm text-ink/55">A pessoa receberá um link para criar uma conta independente em Empresas.</p><label className="block text-sm font-bold">Organização<select required value={memberForm.organizationId} onChange={event => { const org = organizations.find(item => item.id === Number(event.target.value)); setMemberForm({ ...memberForm, organizationId: event.target.value, role: org?.type === 'ASSOCIATION' ? 'ASSOCIATION_ADMIN' : org?.type === 'EDUCATIONAL' ? 'EDUCATION_ADMIN' : 'PARTNER_MANAGER' }); }} className={field}><option value="">Selecione</option>{organizations.filter(org => org.type !== 'COMPANY').map(org => <option key={org.id} value={org.id}>{org.name}</option>)}</select></label><label className="block text-sm font-bold">E-mail<input required type="email" maxLength={180} value={memberForm.email} onChange={event => setMemberForm({ ...memberForm, email: event.target.value })} className={field}/></label><label className="block text-sm font-bold">Papel<select value={memberForm.role} onChange={event => setMemberForm({ ...memberForm, role: event.target.value })} className={field}>{organizations.find(org => org.id === Number(memberForm.organizationId))?.type === 'ASSOCIATION' ? <><option value="ASSOCIATION_ADMIN">Administrador da associação</option><option value="ASSOCIATION_MEMBER">Membro da associação</option></> : organizations.find(org => org.id === Number(memberForm.organizationId))?.type === 'EDUCATIONAL' ? <><option value="EDUCATION_ADMIN">Administrador educacional</option><option value="CONTENT_EDITOR">Editor de conteúdo</option><option value="PARTNER_MANAGER">Gestor de parceiro</option></> : <><option value="PARTNER_MANAGER">Gestor de parceiro</option><option value="CONTENT_EDITOR">Editor de conteúdo</option></>}</select></label><button disabled={busy} className="rounded-full bg-brand-blue px-5 py-3 text-sm font-extrabold text-[#101725] disabled:opacity-50">Criar convite</button>{organizationInviteUrl && <p role="status" className="break-all rounded-xl border border-brand-blue/30 p-3 text-xs">Link de cadastro: <a className="underline" href={organizationInviteUrl}>{organizationInviteUrl}</a></p>}</form></div>
        {associations.map(org => <article key={org.id} className="rounded-2xl border border-white/10 bg-[#121c2b] p-5"><h3 className="font-extrabold">Vínculos de {org.name}</h3><button className="my-2 rounded-full border border-white/20 px-4 py-2" onClick={() => void api<LinkedCompany[]>(`/admin/saas/organizations/${org.id}/companies`, token).then(rows => setCompanyLinks(current => ({ ...current, [org.id]: rows }))).catch(cause => setError(cause.message))}>Consultar vínculos desta associação</button>{(companyLinks[org.id] || []).length ? <div className="mt-3 divide-y divide-white/10">{companyLinks[org.id].map(link => <div key={link.companyId} className="flex flex-wrap items-center justify-between gap-3 py-3 text-sm"><span>{link.name} · {link.status === 'ACTIVE' ? 'Ativa' : link.status === 'PENDING' ? 'Pendente' : 'Encerrada'}</span><div className="flex gap-2">{link.status === 'PENDING' && <button disabled={busy} onClick={() => void changeLinkStatus(org.id, link.companyId, 'ACTIVE')} className="rounded-full bg-brand-green px-3 py-2 text-xs font-extrabold text-[#101725]">Aprovar</button>}{link.status !== 'ENDED' && <button disabled={busy} onClick={() => void changeLinkStatus(org.id, link.companyId, 'ENDED')} className="rounded-full border border-white/20 px-3 py-2 text-xs font-bold">Encerrar vínculo</button>}</div></div>)}</div> : <p className="mt-2 text-sm text-ink/50">Nenhum vínculo solicitado.</p>}</article>)}
      <p className="text-xs leading-5 text-ink/45">O painel mostra números agregados. A associação não recebe acesso a currículos, contatos, candidaturas individuais ou dados comerciais privados.</p>
      <div className="flex flex-wrap items-center gap-3"><p className="text-sm">Gestão básica: até 50 organizações por página. O Hub oferece busca própria de associações.</p><button disabled={organizationOffset===0||loading} className="rounded-full border border-white/20 px-4 py-3" onClick={()=>setOrganizationOffset(v=>Math.max(0,v-50))}>Página anterior de organizações</button><button disabled={organizations.length<50||loading} className="rounded-full border border-white/20 px-4 py-3" onClick={()=>setOrganizationOffset(v=>v+50)}>Próxima página de organizações</button></div>
      <section className="rounded-2xl border border-amber-300/20 bg-amber-400/5 p-5"><p className="text-xs font-extrabold uppercase tracking-widest text-brand-yellow">Migração controlada</p><h2 className="mt-1 text-xl font-black">Empresas da base antiga</h2><p className="mt-2 max-w-3xl text-sm leading-6 text-ink/60">Antes de executar, confirme que existem backups restauráveis dos dois bancos. A prévia é somente leitura. Empresas importadas ficam pendentes de verificação, anúncios em rascunho e responsáveis precisam criar novas credenciais com o e-mail e CNPJ correspondentes.</p><div className="mt-4 flex flex-wrap gap-3"><button type="button" disabled={busy} onClick={() => void previewLegacyCompanies()} className="rounded-full border border-white/20 px-4 py-2 text-sm font-bold disabled:opacity-50">{busy ? 'Aguarde…' : 'Consultar prévia'}</button>{legacyMigrationPreview && legacyMigrationPreview.importable > 0 && <button type="button" disabled={busy} onClick={() => void executeLegacyCompanyMigration()} className="rounded-full bg-brand-yellow px-4 py-2 text-sm font-extrabold text-[#101725] disabled:opacity-50">Importar empresas elegíveis</button>}</div>{legacyMigrationPreview && <dl className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">{[['Empresas na origem', legacyMigrationPreview.sourceCompanies], ['Já importadas', legacyMigrationPreview.alreadyImported], ['Elegíveis', legacyMigrationPreview.importable], ['Sem responsável válido', legacyMigrationPreview.unmatchedOwners], ['CNPJ inválido', legacyMigrationPreview.invalidCnpj], ['Conflito de CNPJ', legacyMigrationPreview.targetCnpjConflicts], ['Conflito de responsável', legacyMigrationPreview.targetOwnerConflicts]].map(([label, value]) => <div key={label} className="rounded-xl border border-white/10 bg-[#0b1421] p-3"><dt className="text-xs text-ink/55">{label}</dt><dd className="mt-1 text-xl font-black">{Number(value).toLocaleString('pt-BR')}</dd></div>)}</dl>}{legacyMigrationResult?.companyErrors.length ? <div role="alert" className="mt-4 rounded-xl border border-red-300/20 bg-red-400/10 p-4"><p className="font-bold">Algumas empresas não foram importadas:</p><ul className="mt-2 list-inside list-disc text-sm">{legacyMigrationResult.companyErrors.map(item => <li key={item}>{item}</li>)}</ul></div> : null}</section>
      <section className="rounded-2xl border border-amber-300/20 bg-amber-400/5 p-5"><p className="text-xs font-extrabold uppercase tracking-widest text-brand-yellow">Migração controlada</p><h2 className="mt-1 text-xl font-black">Associações e parceiros da base antiga</h2><p className="mt-2 max-w-3xl text-sm leading-6 text-ink/60">Execute depois da migração das empresas. Organizações, membros e vínculos são copiados para Empresas com IDs de origem para auditoria. E-mails e papéis são preservados, mas nenhuma senha, sessão ou credencial de Oportunidades é transferida; os dados antigos permanecem intactos.</p><div className="mt-4 flex flex-wrap gap-3"><button type="button" disabled={busy} onClick={() => void previewLegacyOrganizations()} className="rounded-full border border-white/20 px-4 py-2 text-sm font-bold disabled:opacity-50">Consultar prévia de organizações</button>{legacyOrganizationPreview && legacyOrganizationPreview.organizations > legacyOrganizationPreview.alreadyImported && <button type="button" disabled={busy || legacyOrganizationPreview.relationshipsWithoutMigratedCompany > 0} onClick={() => void executeLegacyOrganizationMigration()} className="rounded-full bg-brand-yellow px-4 py-2 text-sm font-extrabold text-[#101725] disabled:opacity-50">Importar organizações</button>}</div>{legacyOrganizationPreview && <dl className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{[['Organizações na origem', legacyOrganizationPreview.organizations], ['Já importadas', legacyOrganizationPreview.alreadyImported], ['Membros históricos', legacyOrganizationPreview.memberships], ['Vínculos empresariais', legacyOrganizationPreview.relationships], ['Sem empresa migrada', legacyOrganizationPreview.relationshipsWithoutMigratedCompany]].map(([label, value]) => <div key={label} className="rounded-xl border border-white/10 bg-[#0b1421] p-3"><dt className="text-xs text-ink/55">{label}</dt><dd className="mt-1 text-xl font-black">{Number(value).toLocaleString('pt-BR')}</dd></div>)}</dl>}{legacyOrganizationResult?.organizationErrors.length ? <div role="alert" className="mt-4 rounded-xl border border-red-300/20 bg-red-400/10 p-4"><p className="font-bold">Algumas organizações não foram importadas:</p><ul className="mt-2 list-inside list-disc text-sm">{legacyOrganizationResult.organizationErrors.map(item => <li key={item}>{item}</li>)}</ul></div> : null}</section>
      <section className="rounded-2xl border border-white/10 bg-[#121c2b] p-5"><h2 className="text-xl font-black">Administradores globais</h2><p className="mt-1 text-sm text-ink/55">A lista configurada em `ADMIN_EMAILS` segue como acesso de emergência. Papéis de banco de dados permitem delegar a administração global.</p><form onSubmit={grantPlatformAdmin} className="mt-4 flex flex-wrap gap-3"><label className="min-w-[16rem] flex-1 text-sm font-bold">Conta existente<input required type="email" value={adminEmail} onChange={event => setAdminEmail(event.target.value)} className={field}/></label><button disabled={busy} className="self-end rounded-full bg-brand-blue px-5 py-3 text-sm font-extrabold text-[#101725] disabled:opacity-50">Conceder acesso</button></form><ul className="mt-4 divide-y divide-white/10">{platformAdmins.map(admin => <li key={admin.userId} className="flex flex-wrap items-center justify-between gap-3 py-3 text-sm"><span><strong>{admin.name}</strong> · {admin.email}</span><button disabled={busy} onClick={() => void revokePlatformAdmin(admin)} className="rounded-full border border-white/20 px-3 py-2 text-xs font-bold">Remover</button></li>)}</ul></section>
      </section>
    <TrailAccountAdmin token={token} /><CareerCostForm token={token}/></main></>;
}
