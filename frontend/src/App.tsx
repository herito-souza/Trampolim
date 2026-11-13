import {useProfilePhoto} from './features/account/useProfilePhoto';
import {AppLayout} from './components/workspace/AppLayout';
import {scheduleOnboarding} from './components/workspace/onboardingModel';
import type {UniversalSearchProvider} from './components/workspace/UniversalSearch';
import {useTerritory} from './features/geography/territoryContext';
import {territorialParams} from './features/geography/territory';
import {accountGroups,isAccountView,readAccountView} from './features/account/accountNavigationModel';
import type {AccountSurface} from './features/account/accountNavigationModel';
const RecruiterPortfolio=lazy(()=>import('./features/career/PortfolioPanel').then(module=>({default:module.RecruiterPortfolio})));
const CareerPanel = lazy(() => import('./features/career/CareerPanel').then(module => ({default:module.CareerPanel})));
const CredentialVerification=lazy(()=>import('./features/learning/PassportVerification').then(module=>({default:module.CredentialVerification})));
const RecruiterPassport=lazy(()=>import('./features/learning/PassportVerification').then(module=>({default:module.RecruiterPassport})));
import { ApiError } from './features/account/api';
import { lazy, Suspense, useEffect, useRef, useState } from 'react';
import { PageBoundary } from './components/ui/PageBoundary';
const Home = lazy(() => import('./pages/Home').then(module => ({ default: module.Home })));
const AuthScreen = lazy(() => import('./features/account/AuthScreen').then(module => ({ default: module.AuthScreen })));
import { api, confirmEmail, confirmTrailEmail, API_URL, revokeTrailSession } from './features/account/api';
import type { AuthResponse, AuthTrail, Profile, ProfileResponse } from './features/account/api';
const ProfilePage = lazy(() => import('./features/account/ProfilePage').then(module => ({ default: module.ProfilePage })));
const ApplicationCenterPage = lazy(() => import('./features/account/ApplicationCenterPage').then(module => ({ default: module.ApplicationCenterPage })));
const WelcomeScreen = lazy(() => import('./features/account/WelcomeScreen').then(module => ({ default: module.WelcomeScreen })));
const loadOpportunitiesPage = () => import('./features/account/OpportunitiesPage').then(module => ({ default: module.OpportunitiesPage }));
const OpportunitiesPage = lazy(loadOpportunitiesPage);
const CatalogPage = lazy(() => import('./features/account/CatalogPage').then(module => ({ default: module.CatalogPage })));
const AdminCatalogPage = lazy(() => import('./features/account/AdminCatalogPage').then(module => ({ default: module.AdminCatalogPage })));
const PublicProfilePage = lazy(() => import('./features/account/PublicProfilePage').then(module => ({ default: module.PublicProfilePage })));
import { AccountNavigation } from './features/account/AccountNavigation';
import type { AccountDestination } from './features/account/AccountNavigation';
import { loadDiagnosticResult } from './features/diagnostic/diagnosticStorage';
import type { DiagnosticResult } from './features/diagnostic/recommendations';
import { RibbonStrip } from './components/ui/RibbonStrip';
const CompanyPublicPage = lazy(() => import('./features/account/CompanyPublicPage').then(module => ({ default: module.CompanyPublicPage })));
const PremiumPage = lazy(() => import('./features/account/PremiumPage').then(module => ({ default: module.PremiumPage })));
import { EcosystemHome } from './pages/EcosystemHome';
const CapacitaPage = lazy(() => import('./pages/CapacitaPage').then(module => ({ default: module.CapacitaPage })));
const SharedCompetency = lazy(() => import('./features/learning/SharedCompetency').then(module => ({ default: module.SharedCompetency })));
const CompanyEntryPage = lazy(() => import('./pages/CompanyEntryPage').then(module => ({ default: module.CompanyEntryPage })));
const AdminCapacitaPage = lazy(() => import('./features/account/AdminCapacitaPage').then(module => ({ default: module.AdminCapacitaPage })));
import type { Organization } from './features/account/SaasAdminPage';
const SaasAdminPage = lazy(() => import('./features/account/SaasAdminPage').then(module => ({ default: module.SaasAdminPage })));
const AssociationDashboardPage = lazy(() => import('./features/account/AssociationDashboardPage').then(module => ({ default: module.AssociationDashboardPage })));
const CompanyPortal = lazy(() => import('./features/account/CompanyPortal').then(module => ({ default: module.CompanyPortal })));

const HubPublicPage = lazy(() => import('./features/hub/HubPublicPage').then(module => ({ default: module.HubPublicPage })));
const HubCompanyInvitation = lazy(() => import('./features/hub/HubCompanyInvitation').then(module => ({ default: module.HubCompanyInvitation })));
const FutureTalentPanel = lazy(() => import('./features/future/FutureTalentPanel').then(module => ({ default: module.FutureTalentPanel })));
const EmployabilityPlanPage = lazy(()=>import('./features/planning/EmployabilityPlanPage').then(module=>({default:module.EmployabilityPlanPage})));
const RadarPanel = lazy(() => import('./features/radar/RadarPanel').then(module => ({ default: module.RadarPanel })));

type PublicArea = 'home' | 'oportunidades' | 'empresas' | 'capacita';
function readPublicArea(): PublicArea {
  const hash = window.location.hash.replace(/^#/, '').split('/')[0];
  if (new URLSearchParams(window.location.search).has('portfolio')) return 'empresas';
  if (new URLSearchParams(window.location.search).has('passport')) return 'empresas';
  if (new URLSearchParams(window.location.search).has('hubCompanyInvite')) return 'empresas';
  if (new URLSearchParams(window.location.search).has('companyInvite')) return 'empresas';
  if (new URLSearchParams(window.location.search).has('organizationInvite')) return 'empresas';
  if (!hash && localStorage.getItem('trampolim_account_type') === 'COMPANY') return 'empresas';
  if (hash.startsWith('empresa-') || ['painel','divulgacoes','selecao','crescimento','perfil-publico','equipe'].includes(hash)) return 'empresas';
  if (hash === 'como-funciona' || hash === 'diagnostico' || hash === 'vagas') return 'oportunidades';
  return hash === 'oportunidades' || hash === 'empresas' || hash === 'capacita' ? hash : 'home';
}

function App() {
  const territory=useTerritory();
  const [token, setToken] = useState(() => localStorage.getItem('trampolim_account_type') === 'COMPANY' ? null : localStorage.getItem('trampolim_token'));
  const [companyToken, setCompanyToken] = useState(() => localStorage.getItem('trampolim_empresas_token'));
  const [studentToken, setStudentToken] = useState(() => localStorage.getItem('trampolim_capacita_token'));
  const [profileReady, setProfileReady] = useState(false);
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const verifiedProfile=useRef<{token:string;data:ProfileResponse}|null>(null);
  useEffect(()=>{if(profile&&verifiedProfile.current?.token===token&&profile.profile.email===verifiedProfile.current.data.profile.email)verifiedProfile.current.data=profile},[token,profile]);
  const [photoRevision,setPhotoRevision]=useState(0);
  const userPhoto=useProfilePhoto(token,profile?.profile.photoFileName,photoRevision);
  function updatePhoto(photoFileName:string|null){setProfile(value=>value?{...value,profile:{...value.profile,photoFileName}}:value);setPhotoRevision(value=>value+1)}
  const [profileView,setProfileViewState]=useState<AccountSurface>(()=>readAccountView(window.location.hash,localStorage.getItem('trampolim_profile_step')));
  function setProfileView(view:typeof profileView){
    setProfileViewState(view);
    const external:Record<string,PublicArea>={ecosystem:'home','company-entry':'empresas',capacita:'capacita'};
    const area=external[view]||'oportunidades';
    const hash=external[view]?(area==='home'?'#inicio':'#'+area):'#oportunidades/'+view;
    if(window.location.hash!==hash)window.history.pushState(null,'',window.location.pathname+window.location.search+hash);
    if(!external[view])localStorage.setItem('trampolim_profile_step',view);
    setPublicArea(area);
  }
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
  const [,refreshLocation]=useState(0);
  function finishCorporateLink(){const url=new URL(window.location.href);for(const key of ['portfolio','passport','hubCompanyInvite'])url.searchParams.delete(key);url.hash='empresas';window.history.pushState(null,'',url);setPublicArea('empresas');refreshLocation(value=>value+1)}
  const corporateLinkLayout=(content:React.ReactNode)=><AppLayout trail="empresas" title="Compartilhamento autorizado" onLogout={logoutCompany} groups={[{id:'principal',label:'Empresas',items:[{id:'overview',label:'Painel da empresa',onSelect:finishCorporateLink}]}]}>{content}</AppLayout>;
  const [companySignupIntent, setCompanySignupIntent] = useState(false);
  const [canManageSaas, setCanManageSaas] = useState(false);
  const [associationSnapshot,setAssociationSnapshot]=useState<{token:string;items:Organization[];error?:string}|null>(null);
  const associations = associationSnapshot?.token===companyToken?associationSnapshot:null;
  const managedAssociations=associations?.items||[];

  useEffect(() => {
    if (!token) { setCanManageSaas(false); return; }
    api<unknown>('/admin/saas/overview', token).then(() => setCanManageSaas(true)).catch(() => setCanManageSaas(false));
  }, [token]);

  useEffect(() => {
    if (!companyToken) { setAssociationSnapshot(null); return; }
    const abort=new AbortController();
    api<Organization[]>('/empresas/institutional/organizations', companyToken,{signal:abort.signal}).then(items=>{if(!abort.signal.aborted)setAssociationSnapshot({token:companyToken,items})}).catch(cause=>{if(!abort.signal.aborted)setAssociationSnapshot({token:companyToken,items:[],error:cause instanceof Error?cause.message:'Não foi possível consultar sua organização.'})});
    return()=>abort.abort();
  }, [companyToken]);

  useEffect(() => { const id = Number(new URLSearchParams(window.location.search).get('hubProgram')); if (Number.isSafeInteger(id) && id > 0) sessionStorage.setItem('trampolim_hub_program_intent', String(id)); }, []);

  useEffect(() => {
    const syncArea = () => {const area=readPublicArea();setPublicArea(area);if(area==='home'){setProfileViewState('ecosystem');return}const view=window.location.hash.split('/')[1];if(area==='oportunidades'&&isAccountView(view)){setProfileViewState(view);localStorage.setItem('trampolim_profile_step',view)}else if(window.location.hash==='#diagnostico')setProfileViewState('diagnostic');};
    window.addEventListener('hashchange', syncArea);
    window.addEventListener('popstate', syncArea);
    return () => { window.removeEventListener('hashchange', syncArea); window.removeEventListener('popstate', syncArea); };
  }, []);

  useEffect(()=>{if(token&&profileReady&&publicArea==='oportunidades'&&window.location.hash==='#oportunidades'&&isAccountView(profileView))window.history.replaceState(null,'',window.location.pathname+window.location.search+'#oportunidades/'+profileView)},[token,profileReady,publicArea,profileView]);

  function navigatePublicArea(area: PublicArea) {
    const hash = area === 'home' ? '#inicio' : `#${area}`;
    window.history.pushState(null, '', `${window.location.pathname}${window.location.search}${hash}`);
    setPublicArea(area);
    if(area==='oportunidades')setProfileViewState('opportunities');
    if(area==='home')setProfileViewState('ecosystem');
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
    if (!token) { verifiedProfile.current=null;setProfile(null); setProfileReady(true); return; }
    if (localStorage.getItem('trampolim_account_type') === 'COMPANY') { setProfile(null); setProfileReady(true); return; }
    if(verifiedProfile.current?.token===token){setProfile(verifiedProfile.current.data);setProfileReady(true);return;}
    const abort=new AbortController();setProfileReady(false);
    // Warm only the selected candidate dashboard, without fetching private data.
    if(readPublicArea()==='oportunidades'&&readAccountView(window.location.hash,localStorage.getItem('trampolim_profile_step'))==='opportunities')void loadOpportunitiesPage().catch(()=>undefined);
    // Both endpoints enforce this same candidate token. Consume the diagnostic only
    // after the profile is validated; aborted/failed sessions never update storage.
    const diagnosticRequest=api<DiagnosticResult|null>('/profile/diagnostic',token,{signal:abort.signal})
      .then(value=>({ok:true as const,value}),()=>({ok:false as const}));
    api<ProfileResponse>('/profile', token,{signal:abort.signal})
      .then(async result => {
        if(abort.signal.aborted)return;
        try {
          const diagnosticResponse = await diagnosticRequest;
          if(!diagnosticResponse.ok)throw new Error('Diagnostic temporarily unavailable');
          const savedDiagnostic = diagnosticResponse.value;
          if(abort.signal.aborted)return;
          if (savedDiagnostic) {
            sessionStorage.setItem('trampolim_diagnostic_result', JSON.stringify(savedDiagnostic));
          } else {
            const pendingDiagnostic = loadDiagnosticResult();
            if (pendingDiagnostic) {
              await api<DiagnosticResult>('/profile/diagnostic', token, { method: 'PUT', body: JSON.stringify(pendingDiagnostic),signal:abort.signal });
            }
          }
        } catch {
          // O perfil continua acessível se a consulta ao diagnóstico falhar.
        }
        if(abort.signal.aborted)return;
        verifiedProfile.current={token,data:result};
        setProfile(result);setProfileReady(true);
      })
      .catch((error: unknown) => {
        if(abort.signal.aborted)return;
        if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
          localStorage.removeItem('trampolim_token'); setToken(null);
        }
        setProfile(null); setProfileReady(true);
      });
    return()=>abort.abort();
  }, [token]);

  function onSuccess(result: AuthResponse, mode: 'login' | 'cadastro') {
    const onboardingTrail = authTrail;
    if (result.user?.email) {
      if (mode === 'cadastro') scheduleOnboarding(onboardingTrail, result.user.email);
      try { localStorage.setItem(`trampolim_${onboardingTrail}_email`, result.user.email.trim().toLowerCase()); } catch { /* This session remains usable without saved onboarding identity. */ }
    }
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

  if (currentParams.has('credential')) return <CredentialVerification id={currentParams.get('credential')||''}/>;
  if (currentParams.has('portfolio') && companyToken && !authMode) return corporateLinkLayout(<main><RecruiterPortfolio token={companyToken} initial={currentParams.get('portfolio')||''} onDone={finishCorporateLink}/></main>);
  if (currentParams.has('passport') && companyToken && !authMode) return corporateLinkLayout(<main><RecruiterPassport token={companyToken} initial={currentParams.get('passport')||''} onDone={finishCorporateLink}/></main>);
  if (currentParams.has('hub')) return <HubPublicPage slug={currentParams.get('hub') || ''}/>;
  if (currentParams.has('hubCompanyInvite') && companyToken && !authMode) return corporateLinkLayout(<HubCompanyInvitation token={companyToken} invite={currentParams.get('hubCompanyInvite') || ''} onDone={finishCorporateLink}/>);
  if (currentParams.has('competencia')) return <SharedCompetency shareToken={currentParams.get('competencia') || ''} />;
  if (shareId) return <PublicProfilePage shareId={shareId} />;
  if (companySlug) return <CompanyPublicPage slug={companySlug} />;
  if (verifyToken && !emailConfirmation) return <><RibbonStrip /><main className="grid min-h-screen place-items-center bg-paper p-6"><section className="max-w-lg rounded-3xl border border-white/10 bg-brand-white p-8 text-center shadow-sm"><h1 className="text-3xl font-black">Confirmando seu e-mail…</h1><p className="mt-3 text-ink/65">Estamos validando o link de confirmação.</p></section></main></>;
  if (authMode || emailConfirmation) return <AuthScreen key={authTrail} trail={authTrail} initialMode={authMode || 'login'} initialCompanySignup={companySignupIntent} initialNotice={emailConfirmation?.error ? '' : emailConfirmation?.message || ''} initialError={emailConfirmation?.error ? emailConfirmation.message : ''} onSuccess={onSuccess} onBack={() => { setAuthMode(null); setCompanySignupIntent(false); }} />;
  if (publicArea === 'empresas') return companyToken
    ? <CompanyPortal key={companyToken} token={companyToken} organizations={associations?.items} organizationsError={associations?.error} onLogout={logoutCompany} />
    : <CompanyEntryPage onLogin={() => openAuth('empresas', 'login')} onRegister={() => openAuth('empresas', 'cadastro')} onNavigate={area => navigatePublicArea(area)} />;
  if (publicArea === 'capacita') return <CapacitaPage token={studentToken || undefined} onNavigate={area => navigatePublicArea(area)} onAuth={mode => openAuth('capacita', mode)} onLogout={studentToken ? logoutStudent : undefined} />;
  if (publicArea === 'home' && !token) return <EcosystemHome onDiagnostic={() => { window.location.hash = 'diagnostico'; setPublicArea('oportunidades'); }} onNavigate={navigatePublicArea} />;
  if (token) {
    if (profileReady && !profile) return <main className="grid min-h-screen place-items-center bg-paper p-6"><section role="alert" className="max-w-lg rounded-3xl border border-ink/20 p-8 text-center"><h1 className="text-2xl font-bold">Não foi possível carregar seu perfil</h1><p className="mt-4">Sua sessão foi preservada. Tente novamente em alguns instantes.</p><button onClick={() => window.location.reload()} className="mt-6 rounded-full bg-brand-yellow px-6 py-3 font-bold text-[#101725]">Tentar novamente</button></section></main>;
    if (!profileReady || !profile) return <main className="min-h-screen p-10 text-center"><p role="status" aria-live="polite">Carregando sua sessão...</p></main>;
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
      if (destination === 'association' && managedAssociations.length) { navigatePublicArea('empresas'); return; }
      if (destination === 'opportunities') { openOpportunities(); return; }
      if (destination === 'courses') { openCatalog('course'); return; }
      if (destination === 'jobs') { openCatalog('job'); return; }
      if (destination === 'applications') { localStorage.setItem('trampolim_profile_step','applications'); setProfileView('applications'); return; }
      if (destination === 'career') { localStorage.setItem('trampolim_profile_step','career'); setProfileView('career'); return; }
      if (destination === 'future') { localStorage.setItem('trampolim_profile_step', 'future'); setProfileView('future'); return; }
      if (destination === 'plan') { setProfileView('plan'); return; }
      if (destination === 'radar') { localStorage.setItem('trampolim_profile_step', 'radar'); setProfileView('radar'); return; }
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
    const searchOpportunityResources:UniversalSearchProvider=async(query,signal)=>{
      const params=territorialParams(territory.choice);params.set('q',query);params.set('limit','8');
      const requests=await Promise.allSettled([
        api<{items:{id:number;title:string;companyName:string;area:string;description:string;municipalityLabel?:string}[]}>('/public/companies/regional-opportunities?'+params,token,{signal}),
        api<{items:{id:number;title:string;provider:string;area:string;description:string}[]}>('/opportunities/course/regional?'+params,token,{signal})
      ]);
      const results:import('./components/workspace/UniversalSearch').UniversalSearchResult[]=[];
      const jobs=requests[0];if(jobs.status==='fulfilled')for(const item of jobs.value.items||[])results.push({id:`job-${item.id}`,category:'Vagas',title:item.title,description:[item.companyName,item.area,item.municipalityLabel].filter(Boolean).join(' · '),onSelect:()=>{try{sessionStorage.setItem('trampolim_universal_catalog_search',JSON.stringify({kind:'job',query:item.title}))}catch{}navigateAccount('jobs');window.dispatchEvent(new CustomEvent('trampolim:catalog-search',{detail:{kind:'job',query:item.title}}));}});
      const courses=requests[1];if(courses.status==='fulfilled')for(const item of courses.value.items||[])results.push({id:`catalog-course-${item.id}`,category:'Cursos',title:item.title,description:[item.provider,item.area].filter(Boolean).join(' · '),onSelect:()=>{try{sessionStorage.setItem('trampolim_universal_catalog_search',JSON.stringify({kind:'course',query:item.title}))}catch{}navigateAccount('courses');window.dispatchEvent(new CustomEvent('trampolim:catalog-search',{detail:{kind:'course',query:item.title}}));}});
      if(requests.every(result=>result.status==='rejected'))throw new Error('Busca indisponível');
      return results;
    };
    const accountContent=(()=>{
    if(['admin','admin-capacita','admin-saas'].includes(profileView)&&!canManageCatalog)return <main className="p-6"><section role="alert"><h1 className="text-2xl font-bold">Acesso restrito</h1><p>Sua conta não tem permissão para esta área.</p><button className="mt-4 rounded-full border border-white/20 p-3" onClick={()=>navigateAccount('opportunities')}>Voltar às oportunidades</button></section></main>;
    if (profileView === 'career') return <CareerPanel token={token} onNavigate={navigateAccount} onLogout={logout} canManage={canManageCatalog}/>;
    if (profileView === 'future') return <FutureTalentPanel token={token} onNavigate={navigateAccount} onLogout={logout} canManage={canManageCatalog} />;
    if (profileView === 'plan') return <EmployabilityPlanPage key={token} token={token} onNavigate={navigateAccount}/>;
    if (profileView === 'radar') return <RadarPanel token={token} onNavigate={navigateAccount} onLogout={logout} canManage={canManageCatalog} />;
    if (profileView === 'ecosystem') return <EcosystemHome onDiagnostic={() => { localStorage.setItem('trampolim_profile_step', 'diagnostic'); setProfileView('diagnostic'); window.location.hash = 'diagnostico'; }} onNavigate={area => area === 'home' ? setProfileView('ecosystem') : area === 'oportunidades' ? navigateAccount('opportunities') : area === 'capacita' ? setProfileView('capacita') : setProfileView('company-entry')} />;
    if (profileView === 'company-entry') return companyToken
      ? <CompanyPortal key={companyToken} token={companyToken} organizations={associations?.items} organizationsError={associations?.error} onLogout={logoutCompany} />
      : <CompanyEntryPage onLogin={() => openAuth('empresas', 'login')} onRegister={() => openAuth('empresas', 'cadastro')} onNavigate={area => area === 'home' ? setProfileView('ecosystem') : area === 'oportunidades' ? navigateAccount('opportunities') : setProfileView('capacita')} />;
    if (profileView === 'capacita') return <CapacitaPage token={studentToken || undefined} onNavigate={area => area === 'oportunidades' ? navigateAccount('opportunities') : area === 'capacita' ? setProfileView('capacita') : area === 'home' ? setProfileView('ecosystem') : setProfileView('company-entry')} onAuth={mode => openAuth('capacita', mode)} onLogout={studentToken ? logoutStudent : undefined} />;
    if (profileView === 'admin-capacita' && canManageCatalog) return <AdminCapacitaPage token={token} onBack={() => setProfileView('admin')} />;
    if (profileView === 'admin-saas' && canManageCatalog) return <SaasAdminPage token={token} onBack={() => setProfileView('admin')} />;
    if (profileView === 'association' && managedAssociations.length && companyToken) return <AssociationDashboardPage token={companyToken} onBack={() => setProfileView('welcome')} />;
    if (profileView === 'admin' && canManageCatalog) return <AdminCatalogPage token={token} onBack={backToOpportunities} onLogout={logout} onProfile={() => navigateAccount('profile')} navigation={<AccountNavigation active="admin" onNavigate={navigateAccount} canManage hasAssociation={managedAssociations.length > 0} />} />;
    if (profileView === 'diagnostic') return <Home isAuthenticated hasPreviousDiagnostic={Boolean(loadDiagnosticResult())} startNewTest={sessionStorage.getItem('trampolim_diagnostic_retake') === '1'} onAuth={() => { sessionStorage.removeItem('trampolim_diagnostic_retake'); openOpportunities(); }} onDiagnosticComplete={result => { sessionStorage.removeItem('trampolim_diagnostic_retake'); saveDiagnostic(result); }} onProfile={() => navigateAccount('profile')} onLogout={logout} accountNavigation={<AccountNavigation active="diagnostic" onNavigate={navigateAccount} canManage={canManageCatalog} hasAssociation={managedAssociations.length > 0} />} />;
    if (profileView === 'welcome') return <WelcomeScreen token={token} profile={profile.profile} onProfileChange={next => setProfile(current => current ? { ...current, profile: next } : current)} onContinue={openDetails} onLogout={logout} onNavigate={navigateAccount} canManage={canManageCatalog} />;
    if (profileView === 'opportunities') return <OpportunitiesPage token={token} profile={profile.profile} profileDetails={profile} onEditProfile={openDetails} onLogout={logout} onRetakeDiagnostic={retakeDiagnostic} onOpenCatalog={openCatalog} canManage={canManageCatalog} onNavigate={navigateAccount} />;
    if (profileView === 'applications') return <ApplicationCenterPage token={token} onEditPersonalTracking={() => navigateAccount('profile')} />;
    if (profileView === 'premium') return <PremiumPage token={token} profile={profile} onLogout={logout} onNavigate={navigateAccount} />;
    if (profileView === 'catalog-courses' || profileView === 'catalog-jobs') return <CatalogPage key={profileView} kind={profileView === 'catalog-courses' ? 'course' : 'job'} token={token} profile={profile.profile} onLogout={logout} onRetakeDiagnostic={retakeDiagnostic} onNavigate={navigateAccount} canManage={canManageCatalog} />;
    return <ProfilePage token={token} onSnapshot={snapshot=>setProfile(current=>current?.profile.email===snapshot.profile.email?snapshot:current)} photoUrl={userPhoto} onPhotoChange={updatePhoto} onLogout={logout} onBack={openWelcome} onComplete={openOpportunities} onNavigate={navigateAccount} canManage={canManageCatalog} />;
    })();
    if(['ecosystem','company-entry','capacita','association'].includes(profileView))return accountContent;
    const active=({'catalog-courses':'courses','catalog-jobs':'jobs',details:'profile',welcome:'profile'} as Record<string,string>)[profileView]||profileView;
    const onboardingRole=canManageCatalog?'Administrador':'Candidato';
    const onboardingSteps=canManageCatalog?[{title:'Conheça o painel administrativo',description:'Use a navegação lateral para acessar os recursos administrativos disponíveis para sua conta.',actionLabel:'Abrir administração',onAction:()=>navigateAccount('admin')},{title:'Confira as informações antes de publicar',description:'Revise cursos e oportunidades pelas telas de gestão já disponíveis. As permissões da sua conta continuam valendo.',actionLabel:'Ver oportunidades',onAction:()=>navigateAccount('opportunities')},{title:'Seu perfil e sua conta',description:'Atualize seus dados e consulte as opções de privacidade no seu perfil.',actionLabel:'Abrir meu perfil',onAction:()=>navigateAccount('profile')}]:[{title:'Complete seu perfil profissional',description:'Adicione suas informações e experiências para deixar seu perfil pronto para explorar oportunidades.',actionLabel:'Completar perfil',onAction:()=>navigateAccount('profile')},{title:'Descubra seu caminho',description:'O diagnóstico ajuda a organizar interesses e recomendações profissionais. Você pode refazê-lo quando quiser.',actionLabel:'Fazer diagnóstico',onAction:()=>navigateAccount('diagnostic')},{title:'Explore oportunidades e cursos',description:'Pesquise vagas, acompanhe candidaturas e encontre cursos disponíveis para desenvolver suas competências.',actionLabel:'Buscar oportunidades',onAction:()=>navigateAccount('jobs')}];
    return <AppLayout trail="oportunidades" title={profileView==='plan'?'Meu Plano de Empregabilidade':profileView==='applications'?'Central de Candidaturas':'Trampolim Oportunidades'} userName={profile.profile.name} userPhoto={userPhoto} active={active} groups={accountGroups(navigateAccount,canManageCatalog,managedAssociations.length>0)} onboarding={{identity:profile.profile.email,role:onboardingRole,steps:onboardingSteps}} searchProvider={searchOpportunityResources} onProfile={()=>navigateAccount('profile')} onNotice={()=>navigateAccount('future')} onLogout={logout}>{accountContent}</AppLayout>;

  }
  if (publicArea === 'oportunidades') return <Home onAuth={mode => openAuth('oportunidades', mode)} />;
  return <EcosystemHome onDiagnostic={() => { window.location.hash = 'diagnostico'; setPublicArea('oportunidades'); }} onNavigate={navigatePublicArea} />;
}

export default function Application() {
  return <PageBoundary><Suspense fallback={<main className="grid min-h-screen place-items-center p-8"><p role="status">Carregando sua área…</p></main>}><App /></Suspense></PageBoundary>;
}
