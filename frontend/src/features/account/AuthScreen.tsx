import { useState } from 'react';
import type { FormEvent } from 'react';
import { acceptCompanyInvite, acceptOrganizationInvite, authenticate, authenticateTrail, registerOrganizationInvite, requestPasswordReset, resendEmailConfirmation, resetPassword } from './api';
import type { AuthTrail } from './api';
import type { AuthResponse } from './api';
import { RibbonStrip } from '../../components/ui/RibbonStrip';
import { TrailUsageNotice } from './TrailUsageNotice';

interface AuthScreenProps {
  initialMode: 'login' | 'cadastro';
  onSuccess: (result: AuthResponse, mode: 'login' | 'cadastro') => void;
  onBack: () => void;
  initialNotice?: string;
  initialError?: string;
  initialCompanySignup?: boolean;
  trail?: AuthTrail;
}

export function AuthScreen({ initialMode, onSuccess, onBack, initialNotice = '', initialError = '', initialCompanySignup = false, trail = 'oportunidades' }: AuthScreenProps) {
  const [mode, setMode] = useState(initialMode);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState(initialError);
  const [busy, setBusy] = useState(false);
  const [recovery, setRecovery] = useState(false);
  const [resetToken, setResetToken] = useState(() => new URLSearchParams(`${window.location.search}&${window.location.hash.split('?')[1] || ''}`).get('resetToken') || new URLSearchParams(`${window.location.search}&${window.location.hash.split('?')[1] || ''}`).get('token') || '');
  const [newPassword, setNewPassword] = useState('');
  const [success, setSuccess] = useState(initialNotice);
  const [companySignup] = useState(trail === 'empresas' || initialCompanySignup);
  const [companyInvite] = useState(() => new URLSearchParams(window.location.search).get('companyInvite') || '');
  const [organizationInvite] = useState(() => new URLSearchParams(window.location.search).get('organizationInvite') || '');
  const [company, setCompany] = useState({ legalName: '', tradeName: '', cnpj: '', phone: '', website: '', industry: '', address: '', city: '', state: '' });
  const [ageBand, setAgeBand] = useState('');
  const [educationStage, setEducationStage] = useState('HIGH_SCHOOL');
  const [institutionName, setInstitutionName] = useState('');
  const [termsAccepted, setTermsAccepted] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setSuccess('');
    const invalid = (message: string) => { setError(message); return; };
    if (resetToken && new TextEncoder().encode(newPassword).length > 72) return invalid('Nova senha: use uma senha mais curta (limite de 72 bytes).');
    if (resetToken && newPassword.length < 8) return invalid('Nova senha: informe pelo menos 8 caracteres.');
    if (resetToken && newPassword !== confirmPassword) return invalid('A confirmação da nova senha não confere.');
    if (!resetToken && !email.trim()) return invalid('E-mail: preencha este campo.');
    if (!resetToken && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) return invalid('E-mail: informe um endereço válido, como voce@email.com.');
    if (!resetToken && !recovery && mode === 'cadastro' && !name.trim()) return invalid('Nome completo: preencha este campo.');
    if (!resetToken && !recovery && mode === 'cadastro' && trail === 'capacita' && !ageBand) return invalid('Selecione sua faixa etária. O cadastro atende pessoas a partir de 14 anos.');
    if (!resetToken && !recovery && mode === 'cadastro' && !termsAccepted) return invalid('Aceite os termos para criar sua conta.');
    if (!resetToken && !recovery && !companyInvite && !organizationInvite && companySignup && mode === 'cadastro') {
      const companyFields: [keyof typeof company, string][] = [
        ['legalName', 'Razão social'], ['tradeName', 'Nome fantasia'], ['cnpj', 'CNPJ'], ['phone', 'Telefone'],
        ['industry', 'Setor de atuação'], ['address', 'Endereço'], ['city', 'Cidade'], ['state', 'UF'],
      ];
      for (const [key, label] of companyFields) if (!company[key].trim()) return invalid(`${label}: preencha este campo.`);
      if (!/^[A-Z]{2}$/i.test(company.state.trim())) return invalid('UF: informe a sigla de dois caracteres do estado.');
      if (company.website.trim()) {
        try { const url = new URL(company.website); if (!['http:', 'https:'].includes(url.protocol)) throw new Error(); }
        catch { return invalid('Site da empresa: informe um endereço válido começando com http:// ou https://.'); }
      }
    }
    if (!resetToken && !recovery && !password) return invalid('Senha: preencha este campo.');
    if (!resetToken && !recovery && mode === 'cadastro' && new TextEncoder().encode(password).length > 72) return invalid('Senha: use uma senha mais curta (limite de 72 bytes).');
    if (!resetToken && !recovery && mode === 'cadastro' && password.length < 8) return invalid('Senha: use pelo menos 8 caracteres.');
    if (!resetToken && !recovery && mode === 'cadastro' && password !== confirmPassword) return invalid('A confirmação da senha não confere.');
    setBusy(true);
    try {
      if (resetToken) {
        const message = await resetPassword(resetToken, newPassword, trail);
        setSuccess(message);
        setResetToken('');
        setNewPassword('');
        setConfirmPassword('');
        window.history.replaceState({}, '', window.location.pathname);
        setMode('login');
        return;
      }
      if (recovery) {
        setSuccess(await requestPasswordReset(email, trail));
        return;
      }
      let result: AuthResponse;
      if (organizationInvite) {
        if (mode === 'login') {
          result = await authenticateTrail('empresas', 'login', { email, password });
          await acceptOrganizationInvite(organizationInvite, result.token);
        } else {
          result = await registerOrganizationInvite({ token: organizationInvite, name, email, password, termsAccepted });
        }
      } else if (companyInvite && mode === 'login') {
        result = trail === 'oportunidades' ? await authenticate('login', { email, password }) : await authenticateTrail(trail, 'login', { email, password });
        await acceptCompanyInvite(companyInvite, result.token);
        result.accountType = 'COMPANY';
      } else {
        const payload: Record<string, string | boolean> = companyInvite && mode === 'cadastro'
          ? { name, email, password, termsAccepted }
          : trail === 'capacita'
          ? mode === 'cadastro' ? { name, email, password, educationStage, institutionName, termsAccepted, ageBand } : { email, password }
          : mode === 'cadastro' ? { name, email, password, ...(companySignup ? company : {}), termsAccepted } : { email, password };
        if (trail === 'oportunidades') {
          const legacyPayload = Object.fromEntries(Object.entries(payload).map(([key, value]) => [key, String(value)]));
          result = await authenticate(companyInvite ? 'company-join' : companySignup && mode === 'cadastro' ? 'company-cadastro' : mode, companyInvite ? { token: companyInvite, ...legacyPayload } : legacyPayload);
        } else {
          result = await authenticateTrail(trail, companyInvite ? 'company-join' : mode, companyInvite ? { ...payload, token: companyInvite } : payload);
        }
      }
      if (!result.token && mode === 'cadastro') {
        setSuccess('Conta criada. Enviamos um link de confirmação para seu e-mail. Confirme o endereço para entrar.');
        setMode('login');
        return;
      }
      onSuccess(result, mode);
    } catch (cause) {
      setError(cause instanceof TypeError
        ? 'Não foi possível conectar ao backend. Confira se o servidor está ativo e tente novamente.'
        : cause instanceof Error ? cause.message : 'Não foi possível concluir.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <RibbonStrip />
      <main className="min-h-screen px-[6vw] py-8">
      <a href="#auth-form" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand-white focus:px-4 focus:py-3 focus:font-bold">Pular para o formulário</a>
      <button onClick={onBack} className="border-0 bg-transparent font-bold text-ink cursor-pointer">← Voltar ao início</button>
      <div className="mx-auto mt-12 grid max-w-5xl overflow-hidden rounded-[28px] bg-brand-white shadow-xl md:grid-cols-2">
        <section className="bg-ink p-9 text-brand-white md:p-12">
          <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-brand-yellow">Trampolim {trail === 'empresas' ? 'Empresas' : trail === 'capacita' ? 'Capacita' : 'Oportunidades'}</p>
          <h1 className="mt-8 text-4xl font-black leading-tight">{trail === 'empresas' ? 'Conecte sua empresa a novos talentos.' : trail === 'capacita' ? 'Aprenda no seu ritmo e acompanhe sua jornada.' : 'Seu próximo passo começa por aqui.'}</h1>
          <p className="mt-5 text-white/75">{trail === 'empresas' ? 'Gerencie sua organização e divulgue oportunidades em um ambiente exclusivo para empresas.' : trail === 'capacita' ? 'Crie sua conta educacional para salvar seu progresso e acessar conteúdos por etapa de ensino.' : 'Crie seu perfil, reúna suas experiências e deixe suas conquistas prontas para novas oportunidades.'}</p>
        </section>
        <section className="p-8 md:p-12">
          {!resetToken && !recovery && !companyInvite && <div className="mb-8 flex gap-5 border-b border-ink/10">
            <button onClick={() => { setMode('login'); setError(''); }} className={`border-0 border-b-2 bg-transparent px-1 pb-3 font-extrabold cursor-pointer ${mode === 'login' ? 'border-brand-red text-ink' : 'border-transparent text-ink/50'}`}>Entrar</button>
            <button onClick={() => { setMode('cadastro'); setError(''); }} className={`border-0 border-b-2 bg-transparent px-1 pb-3 font-extrabold cursor-pointer ${mode === 'cadastro' ? 'border-brand-red text-ink' : 'border-transparent text-ink/50'}`}>Criar conta</button>
          </div>}
          {companyInvite && <div className="mb-4 rounded-xl border border-brand-yellow/20 bg-brand-yellow/5 p-3 text-sm font-bold"><p>Você recebeu um convite para fazer parte de uma equipe empresarial. Use o e-mail convidado.</p><button type="button" onClick={() => { setMode(mode === 'login' ? 'cadastro' : 'login'); setError(''); }} className="mt-2 border-0 bg-transparent p-0 font-extrabold text-brand-yellow underline">{mode === 'login' ? 'Criar uma conta nova' : 'Já tenho uma conta'}</button></div>}
          {organizationInvite && <div className="mb-4 rounded-xl border border-brand-blue/20 bg-brand-blue/5 p-3 text-sm font-bold"><p>Convite institucional Trampolim Empresas. Use o e-mail que recebeu este link.</p><button type="button" onClick={() => { setMode(mode === 'login' ? 'cadastro' : 'login'); setError(''); }} className="mt-2 border-0 bg-transparent p-0 font-extrabold text-brand-blue underline">{mode === 'login' ? 'Criar uma conta' : 'Já tenho uma conta Empresas'}</button></div>}
          <h2 className="text-3xl font-black">{organizationInvite ? mode === 'login' ? 'Entre para a organização.' : 'Crie seu acesso institucional.' : companyInvite ? 'Entre para a equipe.' : resetToken ? 'Crie uma nova senha.' : recovery ? 'Recupere seu acesso.' : mode === 'login' ? 'Que bom ter você de volta.' : 'Vamos começar?'}</h2>
          <p className="mt-2 text-sm text-ink/65">{organizationInvite ? 'Esta conta terá acesso somente às funções autorizadas da organização.' : companyInvite ? 'Crie sua conta para acessar o portal da empresa.' : resetToken ? 'Escolha uma senha com pelo menos 8 caracteres.' : recovery ? 'Enviaremos um link seguro para o e-mail cadastrado.' : mode === 'login' ? 'Entre para continuar seu perfil.' : 'Seus dados ficam salvos para você voltar quando quiser.'}</p>
          <form id="auth-form" noValidate onSubmit={submit} className="mt-7 space-y-4">
            {!resetToken && !recovery && mode === 'cadastro' && <label className="block text-sm font-bold">Nome completo<input autoComplete="name" value={name} onChange={e => setName(e.target.value)} className="mt-2 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal" placeholder="Seu nome" /></label>}
            {!resetToken && !recovery && mode === 'cadastro' && trail === 'capacita' && <div className="space-y-4"><label className="block text-sm font-bold">Faixa etária<select required value={ageBand} onChange={event => setAgeBand(event.target.value)} className="mt-2 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal"><option value="">Selecione sua faixa etária</option><option value="AGE_14_17">Tenho de 14 a 17 anos</option><option value="AGE_18_PLUS">Tenho 18 anos ou mais</option></select><span className="mt-2 block text-xs font-normal">O cadastro é destinado a pessoas a partir de 14 anos. Esta informação é autodeclarada.</span></label><label className="block text-sm font-bold">Etapa de ensino<select required value={educationStage} onChange={event => setEducationStage(event.target.value)} className="mt-2 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal"><option value="HIGH_SCHOOL">Ensino médio</option><option value="UNIVERSITY">Ensino superior</option><option value="CAREER_START">Conclusão do ensino médio e início profissional</option></select></label><label className="block text-sm font-bold">Instituição de ensino (opcional)<input maxLength={180} value={institutionName} onChange={event => setInstitutionName(event.target.value)} className="mt-2 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal" /></label></div>}
            {!resetToken && !recovery && !companyInvite && !organizationInvite && mode === 'cadastro' && companySignup && <div className="space-y-4 rounded-2xl border border-white/10 bg-[#0b1421] p-4"><p className="text-xs font-extrabold uppercase tracking-widest text-brand-red">Dados da empresa</p>
              <label className="block text-sm font-bold">Razão social<input required value={company.legalName} onChange={e => setCompany({ ...company, legalName: e.target.value })} className="mt-2 w-full rounded-xl border border-ink/20 px-4 py-3 font-normal" /></label>
              <label className="block text-sm font-bold">Nome fantasia<input required value={company.tradeName} onChange={e => setCompany({ ...company, tradeName: e.target.value })} className="mt-2 w-full rounded-xl border border-ink/20 px-4 py-3 font-normal" /></label>
              <div className="grid gap-4 sm:grid-cols-2"><label className="block text-sm font-bold">CNPJ<input required inputMode="numeric" maxLength={18} value={company.cnpj} onChange={e => setCompany({ ...company, cnpj: e.target.value })} placeholder="00.000.000/0000-00" className="mt-2 w-full rounded-xl border border-ink/20 px-4 py-3 font-normal" /></label><label className="block text-sm font-bold">Telefone<input required type="tel" value={company.phone} onChange={e => setCompany({ ...company, phone: e.target.value })} className="mt-2 w-full rounded-xl border border-ink/20 px-4 py-3 font-normal" /></label></div>
              <label className="block text-sm font-bold">Setor de atuação<input required value={company.industry} onChange={e => setCompany({ ...company, industry: e.target.value })} placeholder="Tecnologia, saúde…" className="mt-2 w-full rounded-xl border border-ink/20 px-4 py-3 font-normal" /></label>
              <label className="block text-sm font-bold">Endereço<input required value={company.address} onChange={e => setCompany({ ...company, address: e.target.value })} className="mt-2 w-full rounded-xl border border-ink/20 px-4 py-3 font-normal" /></label>
              <div className="grid gap-4 sm:grid-cols-[1fr_100px]"><label className="block text-sm font-bold">Cidade<input required value={company.city} onChange={e => setCompany({ ...company, city: e.target.value })} className="mt-2 w-full rounded-xl border border-ink/20 px-4 py-3 font-normal" /></label><label className="block text-sm font-bold">UF<input required minLength={2} maxLength={2} value={company.state} onChange={e => setCompany({ ...company, state: e.target.value.toUpperCase() })} className="mt-2 w-full rounded-xl border border-ink/20 px-4 py-3 font-normal" /></label></div>
              <label className="block text-sm font-bold">Site (opcional)<input type="url" value={company.website} onChange={e => setCompany({ ...company, website: e.target.value })} placeholder="https://" className="mt-2 w-full rounded-xl border border-ink/20 px-4 py-3 font-normal" /></label>
            </div>}
            {!resetToken && <label className="block text-sm font-bold">{trail === 'empresas' ? 'E-mail corporativo ou do responsável' : 'E-mail'}<input type="email" autoComplete="email" value={email} onChange={e => setEmail(e.target.value)} className="mt-2 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal" placeholder="voce@email.com" /></label>}
            {resetToken ? <><label className="block text-sm font-bold">Nova senha<input required type="password" minLength={8} autoComplete="new-password" value={newPassword} onChange={e => setNewPassword(e.target.value)} className="mt-2 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal" /></label><label className="block text-sm font-bold">Confirme a nova senha<input required type="password" minLength={8} autoComplete="new-password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} className="mt-2 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal" /></label></> : !recovery && <><label className="block text-sm font-bold">Senha<input required type="password" minLength={mode === 'cadastro' ? 8 : undefined} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={e => setPassword(e.target.value)} className="mt-2 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal" placeholder={mode === 'cadastro' ? 'Pelo menos 8 caracteres' : 'Sua senha'} /></label>{mode === 'cadastro' && <label className="block text-sm font-bold">Confirme sua senha<input required type="password" minLength={8} autoComplete="new-password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} className="mt-2 w-full rounded-xl border border-ink/20 bg-white px-4 py-3 font-normal" /></label>}</>}
            {!resetToken && !recovery && mode === 'cadastro' && <><TrailUsageNotice trail={trail} /><label className="flex items-start gap-3 text-sm font-medium"><input required type="checkbox" checked={termsAccepted} onChange={event => setTermsAccepted(event.target.checked)} className="mt-1" />Li o aviso de uso e privacidade e concordo com as condições apresentadas para esta área.</label></>}
            {error && <p role="alert" className="rounded-xl bg-red-50 p-3 text-sm font-semibold text-brand-red">{error}</p>}
            {success && <p role="status" className="rounded-xl bg-green-50 p-3 text-sm font-semibold text-brand-green">{success}</p>}
            <button disabled={busy} className="w-full rounded-full border-0 bg-brand-yellow px-6 py-4 font-extrabold text-[#101725] cursor-pointer disabled:opacity-60">{busy ? 'Aguarde...' : organizationInvite ? mode === 'login' ? 'Entrar e aceitar convite' : 'Criar acesso e aceitar convite' : companyInvite ? 'Aceitar convite e criar conta' : resetToken ? 'Salvar nova senha' : recovery ? 'Enviar link de recuperação' : mode === 'login' ? 'Entrar na minha conta' : companySignup ? 'Criar conta empresarial' : 'Criar meu perfil'}</button>
          </form>
          {!resetToken && !companyInvite && <button type="button" onClick={() => { setRecovery(!recovery); setError(''); setSuccess(''); }} className="mt-5 border-0 bg-transparent text-sm font-bold text-brand-blue underline cursor-pointer">{recovery ? 'Voltar ao login' : 'Esqueci minha senha'}</button>}
          {mode === 'login' && !recovery && !resetToken && <button type="button" onClick={() => resendEmailConfirmation(email, trail).then(setSuccess).catch(cause => setError(cause.message))} className="mt-4 border-0 bg-transparent text-sm font-bold text-brand-blue underline cursor-pointer">Não recebeu o link? Reenviar confirmação</button>}
        </section>
      </div>
      </main>
    </>
  );
}
