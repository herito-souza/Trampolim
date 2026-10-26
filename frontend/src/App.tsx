import { useEffect, useState } from 'react';
import { Home } from './pages/Home';
import { AuthScreen } from './features/account/AuthScreen';
import { api, confirmEmail, API_URL } from './features/account/api';
import type { AuthResponse, Profile, ProfileResponse } from './features/account/api';
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
import { CompanyDashboard } from './features/account/CompanyDashboard';
import { CompanyPublicPage } from './features/account/CompanyPublicPage';
import { PremiumPage } from './features/account/PremiumPage';

function App() {
  const [token, setToken] = useState(() => localStorage.getItem('trampolim_token'));
  const [profileReady, setProfileReady] = useState(false);
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [profileView, setProfileView] = useState<'welcome' | 'details' | 'opportunities' | 'diagnostic' | 'catalog-courses' | 'catalog-jobs' | 'premium' | 'admin'>(() => {
    const step = localStorage.getItem('trampolim_profile_step');
    return step === 'details' || step === 'opportunities' || step === 'diagnostic' || step === 'catalog-courses' || step === 'catalog-jobs' || step === 'premium' || step === 'admin' ? step : 'welcome';
  });
  const [authMode, setAuthMode] = useState<'login' | 'cadastro' | null>(() => {
    const params = new URLSearchParams(window.location.search);
    return params.has('resetToken') ? 'login' : params.has('companyInvite') ? 'cadastro' : null;
  });
  const shareId = new URLSearchParams(window.location.search).get('perfil');
  const currentParams = new URLSearchParams(window.location.search);
  const companySlug = currentParams.get('empresa');
  const companyInviteToken = currentParams.get('companyInvite');
  const verifyToken = new URLSearchParams(window.location.search).get('verifyToken');
  const [emailConfirmation, setEmailConfirmation] = useState<{ message: string; error: boolean } | null>(null);
  const [companyAccessGranted, setCompanyAccessGranted] = useState(false);
  const [companyInviteError, setCompanyInviteError] = useState('');

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
    confirmEmail(verifyToken).then(message => setEmailConfirmation({ message, error: false })).catch(cause => setEmailConfirmation({ message: cause instanceof Error ? cause.message : 'Não foi possível confirmar este e-mail.', error: true }));
    window.history.replaceState({}, '', window.location.pathname);
  }, [verifyToken]);

  useEffect(() => {
    if (!token || !companyInviteToken) return;
    api<{ accountType: string }>('/company/invites/accept', token, { method: 'POST', body: JSON.stringify({ token: companyInviteToken }) })
      .then(() => {
        localStorage.setItem('trampolim_account_type', 'COMPANY');
        setCompanyAccessGranted(true);
        const nextUrl = new URL(window.location.href); nextUrl.searchParams.delete('companyInvite');
        window.history.replaceState({}, '', nextUrl);
      })
      .catch(cause => setCompanyInviteError(cause instanceof Error ? cause.message : 'Não foi possível aceitar o convite empresarial.'));
  }, [token, companyInviteToken]);

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
      .catch(() => { localStorage.removeItem('trampolim_token'); setProfile(null); setToken(null); setProfileReady(true); });
  }, [token]);

  function onSuccess(result: AuthResponse, mode: 'login' | 'cadastro') {
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
    localStorage.removeItem('trampolim_token');
    localStorage.removeItem('trampolim_account_type');
    localStorage.removeItem('trampolim_profile_step');
    sessionStorage.removeItem('trampolim_diagnostic_result');
    sessionStorage.removeItem('trampolim_diagnostic_retake');
    setProfile(null);
    setToken(null);
    setAuthMode(null);
  }

  if (shareId) return <PublicProfilePage shareId={shareId} />;
  if (companySlug) return <CompanyPublicPage slug={companySlug} />;
  if (verifyToken && !emailConfirmation) return <><RibbonStrip /><main className="grid min-h-screen place-items-center bg-paper p-6"><section className="max-w-lg rounded-3xl border border-white/10 bg-brand-white p-8 text-center shadow-sm"><h1 className="text-3xl font-black">Confirmando seu e-mail…</h1><p className="mt-3 text-ink/65">Estamos validando o link de confirmação.</p></section></main></>;
  if (token) {
    if (companyInviteError && !companyAccessGranted) return <main className="grid min-h-screen place-items-center px-6"><section className="max-w-lg rounded-3xl border border-red-400/20 bg-[#121c2b] p-8 text-center"><p role="alert" className="font-bold">{companyInviteError}</p><button onClick={() => { logout(); setAuthMode('cadastro'); setCompanyInviteError(''); }} className="mt-5 rounded-full bg-brand-yellow px-6 py-3 font-extrabold text-[#101725]">Sair e entrar com o e-mail do convite</button></section></main>;
    if (companyAccessGranted || localStorage.getItem('trampolim_account_type') === 'COMPANY') return <><CompanyDashboard token={token} onLogout={logout} />{companyInviteError && <p role="alert" className="fixed bottom-4 left-4 right-4 rounded-xl bg-red-400/15 p-4 text-center text-sm font-bold">{companyInviteError}</p>}</>;
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
    const canManageCatalog = adminEmails.includes(profile.profile.email.toLowerCase());
    const navigateAccount = (destination: AccountDestination) => {
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
    };
    if (profileView === 'admin' && canManageCatalog) return <AdminCatalogPage token={token} onBack={backToOpportunities} onLogout={logout} onProfile={() => navigateAccount('profile')} navigation={<AccountNavigation active="admin" onNavigate={navigateAccount} canManage />} />;
    if (profileView === 'diagnostic') return <Home isAuthenticated hasPreviousDiagnostic={Boolean(loadDiagnosticResult())} startNewTest={sessionStorage.getItem('trampolim_diagnostic_retake') === '1'} onAuth={() => { sessionStorage.removeItem('trampolim_diagnostic_retake'); openOpportunities(); }} onDiagnosticComplete={result => { sessionStorage.removeItem('trampolim_diagnostic_retake'); saveDiagnostic(result); }} onProfile={() => navigateAccount('profile')} onLogout={logout} accountNavigation={<AccountNavigation active="diagnostic" onNavigate={navigateAccount} canManage={canManageCatalog} />} />;
    if (profileView === 'welcome') return <WelcomeScreen token={token} profile={profile.profile} onProfileChange={next => setProfile(current => current ? { ...current, profile: next } : current)} onContinue={openDetails} onLogout={logout} onNavigate={navigateAccount} canManage={canManageCatalog} />;
    if (profileView === 'opportunities') return <OpportunitiesPage token={token} profile={profile.profile} onEditProfile={openDetails} onLogout={logout} onRetakeDiagnostic={retakeDiagnostic} onOpenCatalog={openCatalog} canManage={canManageCatalog} onNavigate={navigateAccount} />;
    if (profileView === 'premium') return <PremiumPage token={token} profile={profile} onLogout={logout} onNavigate={navigateAccount} />;
    if (profileView === 'catalog-courses' || profileView === 'catalog-jobs') return <CatalogPage kind={profileView === 'catalog-courses' ? 'course' : 'job'} token={token} profile={profile.profile} onLogout={logout} onRetakeDiagnostic={retakeDiagnostic} onNavigate={navigateAccount} canManage={canManageCatalog} />;
    return <ProfilePage token={token} onLogout={logout} onBack={openWelcome} onComplete={openOpportunities} onNavigate={navigateAccount} canManage={canManageCatalog} />;
  }
  if (authMode || emailConfirmation) return <AuthScreen initialMode={authMode || 'login'} initialNotice={emailConfirmation?.error ? '' : emailConfirmation?.message || ''} initialError={emailConfirmation?.error ? emailConfirmation.message : ''} onSuccess={onSuccess} onBack={() => setAuthMode(null)} />;
  return <Home onAuth={setAuthMode} />;
}

export default App;
