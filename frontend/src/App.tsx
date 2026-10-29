import { ApiError } from './features/account/api';
import { useEffect, useState } from 'react';
import { Home } from './pages/Home';
import { AuthScreen } from './features/account/AuthScreen';
import { api, confirmEmail, confirmTrailEmail, API_URL, revokeTrailSession } from './features/account/api';
import type { AuthResponse, AuthTrail, Profile, ProfileResponse } from './features/account/api';
import { ProfilePage } from './features/account/ProfilePage';
import { WelcomeScreen } from './features/account/WelcomeScreen';
import { OpportunitiesPage } from './features/account/OpportunitiesPage';
import { CatalogPage } from './features/account/CatalogPage';
import { AdminCatalogPage } from './features/account/AdminCatalogPage';
import { PublicProfilePage } from './features/account/PublicProfilePage';
import { AccountNavigation } from './features/account/AccountNavigation';
import type { AccountDestination } from './features/account/AccountNavigation';
import { loadDiagnosticResult } from './features/diagnostic/recommendations';
import type { DiagnosticResult } from './features/diagnostic/recommendations';
import { RibbonStrip } from './components/ui/RibbonStrip';
import { CompanyPublicPage } from './features/account/CompanyPublicPage';
import { PremiumPage } from './features/account/PremiumPage';
import { EcosystemHome } from './pages/EcosystemHome';
import { CapacitaPage } from './pages/CapacitaPage';
import { CompanyEntryPage } from './pages/CompanyEntryPage';
import { AdminCapacitaPage } from './features/account/AdminCapacitaPage';
import { SaasAdminPage, type Organization } from './features/account/SaasAdminPage';
import { AssociationDashboardPage } from './features/account/AssociationDashboardPage';
import { CompanyPortal } from './features/account/CompanyPortal';

type PublicArea = 'home' | 'oportunidades' | 'empresas' | 'capacita';
function readPublicArea(): PublicArea {
  const hash = window.location.hash.replace(/^#/, '').split('/')[0];
  if (new URLSearchParams(window.location.search).has('companyInvite')) return 'empresas';
  if (new URLSearchParams(window.location.search).has('organizationInvite')) return 'empresas';
  if (!hash && localStorage.getItem('trampolim_account_type') === 'COMPANY') return 'empresas';
  if (hash === 'como-funciona' || hash === 'diagnostico' || hash === 'vagas') return 'oportunidades';
  return hash === 'oportunidades' || hash === 'empresas' || hash === 'capacita' ? hash : 'home';
}

function App() {
  const [token, setToken] = useState(() => localStorage.getItem('trampolim_account_type') === 'COMPANY' ? null : localStorage.getItem('trampolim_token'));
  const [companyToken, setCompanyToken] = useState(() => localStorage.getItem('trampolim_empresas_token'));
  const [studentToken, setStudentToken] = useState(() => localStorage.getItem('trampolim_capacita_token'));
  const [profileReady, setProfileReady] = useState(false);
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [profileView, setProfileView] = useState<'welcome' | 'details' | 'opportunities' | 'diagnostic' | 'catalog-courses' | 'catalog-jobs' | 'premium' | 'admin' | 'admin-capacita' | 'admin-saas' | 'association' | 'capacita' | 'ecosystem' | 'company-entry'>(() => {
    const step = localStorage.getItem('trampolim_profile_step');
    return step === 'details' || step === 'opportunities' || step === 'diagnostic' || step === 'catalog-courses' || step === 'catalog-jobs' || step === 'premium' || step === 'admin' ? step : 'welcome';
  });
  const [authMode, setAuthMode] = useState<'login' | 'cadastro' | null>(() => {
    const params = new URLSearchParams(window.location.search);
    const hashParams = new URLSearchParams(window.location.hash.split('?')[1] || '');
    return params.has('resetToken') || hashParams.has('token') ? 'login' : params.has('companyInvite') || params.has('organizationInvite') ? 'cadastro' : null;
  });
  const [authTrail, setAuthTrail] = useState<AuthTrail>(() => {
    const area = readPublicArea();
    return area === 'home' ? 'oportunidades' : area;
  });
  const shareId = new URLSearchParams(window.location.search).get('perfil');
  const currentParams = new URLSearchParams(window.location.search);
  const companySlug = currentParams.get('empresa');
  const initialHashParams = new URLSearchParams(window.location.hash.split('?')[1] || '');
  const verifyToken = new URLSearchParams(window.location.search).get('verifyToken') || (window.location.hash.includes('/verify') ? initialHashParams.get('token') : null);
  const [emailConfirmation, setEmailConfirmation] = useState<{ message: string; error: boolean } | null>(null);
  const [publicArea, setPublicArea] = useState<PublicArea>(readPublicArea);
  const [companySignupIntent, setCompanySignupIntent] = useState(false);
  const [canManageSaas, setCanManageSaas] = useState(false);
  const [managedAssociations, setManagedAssociations] = useState<Organization[]>([]);

  useEffect(() => {
    if (!token) { setCanManageSaas(false); setManagedAssociations([]); return; }
    api<unknown>('/admin/saas/overview', token).then(() => setCanManageSaas(true)).catch(() => setCanManageSaas(false));
  }, [token]);

  useEffect(() => {
    if (!companyToken) { setManagedAssociations([]); return; }
    api<Organization[]>('/empresas/association/organizations', companyToken).then(setManagedAssociations).catch(() => setManagedAssociations([]));
  }, [companyToken]);

  useEffect(() => {
    const syncArea = () => setPublicArea(readPublicArea());
    window.addEventListener('hashchange', syncArea);
    window.addEventListener('popstate', syncArea);
    return () => { window.removeEventListener('hashchange', syncArea); window.removeEventListener('popstate', syncArea); };
  }, []);

  function navigatePublicArea(area: PublicArea) {
    const hash = area === 'home' ? '#inicio' : `#${area}`;
    window.history.pushState(null, '', `${window.location.pathname}${window.location.search}${hash}`);
    setPublicArea(area);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function openAuth(trail: AuthTrail, mode: 'login' | 'cadastro') {
    setAuthTrail(trail);
    setAuthMode(mode);
    if (trail !== 'oportunidades') navigatePublicArea(trail);
  }

  useEffect(() => {
    const report = (eventType: 'runtime' | 'unhandled-rejection') => {
      const payload = JSON.stringify({ eventType, path: window.location.pathname });
      void fetch(`${API_URL}/monitoring/client-errors`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: payload, keepalive: true }).catch(() => undefined);
    };
    const onError = (event: ErrorEvent) => { if (event.error || event.message) report('runtime'); };
    const onRejection = () => report('unhandled-rejection');
    window.addEventListener('error', onError);
    window.addEventListener('unhandledrejection', onRejection);
    return () => { window.removeEventListener('error', onError); window.removeEventListener('unhandledrejection', onRejection); };
  }, []);

  useEffect(() => {
    if (!verifyToken) return;
    const verification = authTrail === 'oportunidades' ? confirmEmail(verifyToken) : confirmTrailEmail(verifyToken, authTrail);
    verification.then(message => setEmailConfirmation({ message, error: false })).catch(cause => setEmailConfirmation({ message: cause instanceof Error ? cause.message : 'Não foi possível confirmar este e-mail.', error: true }));
    window.history.replaceState({}, '', window.location.pathname);
  }, [verifyToken, authTrail]);

  useEffect(() => {
    if (!token) { setProfile(null); setProfileReady(true); return; }
    if (localStorage.getItem('trampolim_account_type') === 'COMPANY') { setProfile(null); setProfileReady(true); return; }
    api<ProfileResponse>('/profile', token)
      .then(async result => {
        setProfile(result);
        try {
          const savedDiagnostic = await api<DiagnosticResult | null>('/profile/diagnostic', token);
          if (savedDiagnostic) {
            sessionStorage.setItem('trampolim_diagnostic_result', JSON.stringify(savedDiagnostic));
          } else {
            const pendingDiagnostic = loadDiagnosticResult();
            if (pendingDiagnostic) {
              await api<DiagnosticResult>('/profile/diagnostic', token, { method: 'PUT', body: JSON.stringify(pendingDiagnostic) });
            }
          }
        } catch {
          // O perfil continua acessível se a consulta ao diagnóstico falhar.
        }
        setProfileReady(true);
      })
      .catch((error: unknown) => {
        if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
          localStorage.removeItem('trampolim_token'); setToken(null);
        }
        setProfile(null); setProfileReady(true);
      });
  }, [token]);

  function onSuccess(result: AuthResponse, mode: 'login' | 'cadastro') {
    if (authTrail === 'empresas') {
      localStorage.setItem('trampolim_empresas_token', result.token);
      setCompanyToken(result.token);
      setAuthMode(null);
      setCompanySignupIntent(false);
      navigatePublicArea('empresas');
      return;
    }
    if (authTrail === 'capacita') {
      localStorage.setItem('trampolim_capacita_token', result.token);
      setStudentToken(result.token);
      setAuthMode(null);
      navigatePublicArea('capacita');
      return;
    }
    const accountType = result.accountType || 'PERSON';
    localStorage.setItem('trampolim_account_type', accountType);
    if (accountType === 'COMPANY') {
      localStorage.setItem('trampolim_token', result.token);
      setProfileReady(true); setProfile(null); setToken(result.token); setAuthMode(null);
      return;
    }
    const nextView = mode === 'cadastro' ? 'welcome' : 'diagnostic';
    localStorage.setItem('trampolim_token', result.token);
    localStorage.setItem('trampolim_profile_step', nextView);
    setProfileReady(false);
    setProfileView(nextView);
    setToken(result.token);
    setAuthMode(null);
  }

  function logout() {
    const activeToken = localStorage.getItem('trampolim_token');
    if (activeToken) void revokeTrailSession('oportunidades', activeToken).catch(() => undefined);
    localStorage.removeItem('trampolim_token');
    localStorage.removeItem('trampolim_account_type');
    localStorage.removeItem('trampolim_profile_step');
    sessionStorage.removeItem('trampolim_diagnostic_result');
    sessionStorage.removeItem('trampolim_diagnostic_retake');
    setProfile(null);
    setToken(null);
    setAuthMode(null);
  }

  function logoutCompany() {
    const activeToken = localStorage.getItem('trampolim_empresas_token');
    if (activeToken) void revokeTrailSession('empresas', activeToken).catch(() => undefined);
    localStorage.removeItem('trampolim_empresas_token');
    setCompanyToken(null);
    navigatePublicArea('empresas');
  }

  function logoutStudent() {
    const activeToken = localStorage.getItem('trampolim_capacita_token');
    if (activeToken) void revokeTrailSession('capacita', activeToken).catch(() => undefined);
    localStorage.removeItem('trampolim_capacita_token');
    setStudentToken(null);
    navigatePublicArea('capacita');
  }

  if (shareId) return <PublicProfilePage shareId={shareId} />;
  if (companySlug) return <CompanyPublicPage slug={companySlug} />;
  if (verifyToken && !emailConfirmation) return <><RibbonStrip /><main className="grid min-h-screen place-items-center bg-paper p-6"><section className="max-w-lg rounded-3xl border border-white/10 bg-brand-white p-8 text-center shadow-sm"><h1 className="text-3xl font-black">Confirmando seu e-mail…</h1><p className="mt-3 text-ink/65">Estamos validando o link de confirmação.</p></section></main></>;
  if (authMode || emailConfirmation) return <AuthScreen key={authTrail} trail={authTrail} initialMode={authMode || 'login'} initialCompanySignup={companySignupIntent} initialNotice={emailConfirmation?.error ? '' : emailConfirmation?.message || ''} initialError={emailConfirmation?.error ? emailConfirmation.message : ''} onSuccess={onSuccess} onBack={() => { setAuthMode(null); setCompanySignupIntent(false); }} />;
  if (publicArea === 'empresas') return companyToken
    ? <CompanyPortal token={companyToken} onLogout={logoutCompany} />
    : <CompanyEntryPage onLogin={() => openAuth('empresas', 'login')} onRegister={() => openAuth('empresas', 'cadastro')} onNavigate={area => navigatePublicArea(area)} />;
  if (publicArea === 'capacita') return <CapacitaPage token={studentToken || undefined} onNavigate={area => navigatePublicArea(area)} onAuth={mode => openAuth('capacita', mode)} onLogout={studentToken ? logoutStudent : undefined} />;
  if (publicArea === 'home' && !token) return <EcosystemHome onNavigate={navigatePublicArea} />;
  if (token) {
    if (profileReady && !profile) return <main className="grid min-h-screen place-items-center bg-paper p-6"><section role="alert" className="max-w-lg rounded-3xl border border-ink/20 p-8 text-center"><h1 className="text-2xl font-bold">Não foi possível carregar seu perfil</h1><p className="mt-4">Sua sessão foi preservada. Tente novamente em alguns instantes.</p><button onClick={() => window.location.reload()} className="mt-6 rounded-full bg-brand-yellow px-6 py-3 font-bold text-[#101725]">Tentar novamente</button></section></main>;
    if (!profileReady || !profile) return <main className="min-h-screen p-10 text-center">Carregando sua sessão...</main>;
    const openDetails = () => { localStorage.setItem('trampolim_profile_step', 'details'); setProfileView('details'); };
    const openWelcome = () => { localStorage.setItem('trampolim_profile_step', 'welcome'); setProfileView('welcome'); };
    const openOpportunities = (savedProfile: Profile = profile.profile) => {
      setProfile(current => current ? { ...current, profile: savedProfile } : current);
      localStorage.setItem('trampolim_profile_step', 'opportunities');
      setProfileView('opportunities');
    };
    const saveDiagnostic = (result: DiagnosticResult) => {
      sessionStorage.setItem('trampolim_diagnostic_result', JSON.stringify(result));
      void api<DiagnosticResult>('/profile/diagnostic', token, { method: 'PUT', body: JSON.stringify(result) }).catch(() => undefined);
    };
    const openCatalog = (kind: 'course' | 'job') => {
      const nextView = kind === 'course' ? 'catalog-courses' : 'catalog-jobs';
      localStorage.setItem('trampolim_profile_step', nextView);
      setProfileView(nextView);
    };
    const backToOpportunities = () => {
      localStorage.setItem('trampolim_profile_step', 'opportunities');
      setProfileView('opportunities');
    };
    const retakeDiagnostic = () => {
      sessionStorage.setItem('trampolim_diagnostic_retake', '1');
      localStorage.setItem('trampolim_profile_step', 'diagnostic');
      setProfileView('diagnostic');
    };
    const openSavedDiagnostic = () => {
      sessionStorage.removeItem('trampolim_diagnostic_retake');
      localStorage.setItem('trampolim_profile_step', 'diagnostic');
      setProfileView('diagnostic');
    };
    const adminEmails = (import.meta.env.VITE_ADMIN_EMAILS || '').split(',').map((email: string) => email.trim().toLowerCase());
    const canManageCatalog = adminEmails.includes(profile.profile.email.toLowerCase()) || canManageSaas;
    const navigateAccount = (destination: AccountDestination) => {
      if (destination === 'home') { setProfileView('ecosystem'); return; }
      if (destination === 'companies') { setProfileView('company-entry'); return; }
      if (destination === 'capacita') { setProfileView('capacita'); return; }
      if (destination === 'association' && managedAssociations.length) { setProfileView('association'); return; }
      if (destination === 'opportunities') { openOpportunities(); return; }
      if (destination === 'courses') { openCatalog('course'); return; }
      if (destination === 'jobs') { openCatalog('job'); return; }
      if (destination === 'profile') { openDetails(); return; }
      if (destination === 'premium') {
        localStorage.setItem('trampolim_profile_step', 'premium');
        setProfileView('premium');
        return;
      }
      if (destination === 'diagnostic') {
        if (profileView !== 'diagnostic') openSavedDiagnostic();
        return;
      }
      if (destination === 'admin' && canManageCatalog) {
        localStorage.setItem('trampolim_profile_step', 'admin');
        setProfileView('admin');
      }
      if (destination === 'admin-capacita' && canManageCatalog) setProfileView('admin-capacita');
      if (destination === 'admin-saas' && canManageCatalog) setProfileView('admin-saas');
    };
    if (profileView === 'ecosystem') return <EcosystemHome onNavigate={area => area === 'home' ? setProfileView('ecosystem') : area === 'oportunidades' ? navigateAccount('opportunities') : area === 'capacita' ? setProfileView('capacita') : setProfileView('company-entry')} />;
    if (profileView === 'company-entry') return companyToken
      ? <CompanyPortal token={companyToken} onLogout={logoutCompany} />
      : <CompanyEntryPage onLogin={() => openAuth('empresas', 'login')} onRegister={() => openAuth('empresas', 'cadastro')} onNavigate={area => area === 'home' ? setProfileView('ecosystem') : area === 'oportunidades' ? navigateAccount('opportunities') : setProfileView('capacita')} />;
    if (profileView === 'capacita') return <CapacitaPage token={studentToken || undefined} onNavigate={area => area === 'oportunidades' ? navigateAccount('opportunities') : area === 'capacita' ? setProfileView('capacita') : area === 'home' ? setProfileView('ecosystem') : setProfileView('company-entry')} onAuth={mode => openAuth('capacita', mode)} onLogout={studentToken ? logoutStudent : undefined} />;
    if (profileView === 'admin-capacita' && canManageCatalog) return <AdminCapacitaPage token={token} onBack={() => setProfileView('admin')} />;
    if (profileView === 'admin-saas' && canManageCatalog) return <SaasAdminPage token={token} onBack={() => setProfileView('admin')} />;
    if (profileView === 'association' && managedAssociations.length && companyToken) return <AssociationDashboardPage token={companyToken} onBack={() => setProfileView('welcome')} />;
    if (profileView === 'admin' && canManageCatalog) return <AdminCatalogPage token={token} onBack={backToOpportunities} onLogout={logout} onProfile={() => navigateAccount('profile')} navigation={<AccountNavigation active="admin" onNavigate={navigateAccount} canManage hasAssociation={managedAssociations.length > 0} />} />;
    if (profileView === 'diagnostic') return <Home isAuthenticated hasPreviousDiagnostic={Boolean(loadDiagnosticResult())} startNewTest={sessionStorage.getItem('trampolim_diagnostic_retake') === '1'} onAuth={() => { sessionStorage.removeItem('trampolim_diagnostic_retake'); openOpportunities(); }} onDiagnosticComplete={result => { sessionStorage.removeItem('trampolim_diagnostic_retake'); saveDiagnostic(result); }} onProfile={() => navigateAccount('profile')} onLogout={logout} accountNavigation={<AccountNavigation active="diagnostic" onNavigate={navigateAccount} canManage={canManageCatalog} hasAssociation={managedAssociations.length > 0} />} />;
    if (profileView === 'welcome') return <WelcomeScreen token={token} profile={profile.profile} onProfileChange={next => setProfile(current => current ? { ...current, profile: next } : current)} onContinue={openDetails} onLogout={logout} onNavigate={navigateAccount} canManage={canManageCatalog} />;
    if (profileView === 'opportunities') return <OpportunitiesPage token={token} profile={profile.profile} onEditProfile={openDetails} onLogout={logout} onRetakeDiagnostic={retakeDiagnostic} onOpenCatalog={openCatalog} canManage={canManageCatalog} onNavigate={navigateAccount} />;
    if (profileView === 'premium') return <PremiumPage token={token} profile={profile} onLogout={logout} onNavigate={navigateAccount} />;
    if (profileView === 'catalog-courses' || profileView === 'catalog-jobs') return <CatalogPage kind={profileView === 'catalog-courses' ? 'course' : 'job'} token={token} profile={profile.profile} onLogout={logout} onRetakeDiagnostic={retakeDiagnostic} onNavigate={navigateAccount} canManage={canManageCatalog} />;
    return <ProfilePage token={token} onLogout={logout} onBack={openWelcome} onComplete={openOpportunities} onNavigate={navigateAccount} canManage={canManageCatalog} />;
  }
  if (publicArea === 'oportunidades') return <Home onAuth={mode => openAuth('oportunidades', mode)} />;
  return <EcosystemHome onNavigate={navigatePublicArea} />;
}

export default App;
