import { RibbonStrip } from '../components/ui/RibbonStrip';
import { Header } from '../components/sections/Header';
import { Hero } from '../components/sections/Hero';
import { Stats } from '../components/sections/Stats';
import { HowItWorks } from '../components/sections/HowItWorks';
import { DiagnosticQuiz } from '../features/diagnostic/DiagnosticQuiz';
import { JobsList } from '../components/sections/JobsList';
import { Footer } from '../components/sections/Footer';
import type { DiagnosticResult } from '../features/diagnostic/recommendations';
import type { ReactNode } from 'react';

export function Home({ onAuth, onDiagnosticComplete, isAuthenticated = false, hasPreviousDiagnostic = false, startNewTest = false, accountNavigation, onProfile, onLogout }: { onAuth: (mode: 'login' | 'cadastro') => void; onDiagnosticComplete?: (result: DiagnosticResult) => void; isAuthenticated?: boolean; hasPreviousDiagnostic?: boolean; startNewTest?: boolean; accountNavigation?: ReactNode; onProfile?: () => void; onLogout?: () => void }) {
  return (
    <>
      <RibbonStrip />
      <Header onLogin={() => onAuth('login')} isAuthenticated={isAuthenticated} onProfile={onProfile} onLogout={onLogout} />
      {accountNavigation}
      <a href="#conteudo-principal" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand-white focus:px-4 focus:py-3 focus:font-bold">Pular para o conteúdo</a>
      <main id="conteudo-principal">
        <Hero />
        <Stats />
        <HowItWorks />
        <DiagnosticQuiz onContinue={onAuth} onDiagnosticComplete={onDiagnosticComplete} isAuthenticated={isAuthenticated} hasPreviousDiagnostic={hasPreviousDiagnostic} startNewTest={startNewTest} />
        <JobsList />
      </main>
      <Footer showDiagnosticCta />
    </>
  );
}
