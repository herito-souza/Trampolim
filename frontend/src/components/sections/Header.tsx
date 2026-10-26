import { AccountHeaderActions } from '../../features/account/AccountHeaderActions';

export function Header({ onLogin, isAuthenticated = false, onProfile, onLogout }: { onLogin: () => void; isAuthenticated?: boolean; onProfile?: () => void; onLogout?: () => void }) {
  return (
    <header className="px-[6vw] py-5 flex justify-between items-center">
      <div className="font-serif font-black text-[22px] tracking-[-0.5px]">
        Trampol<span className="text-brand-red">i</span>m
      </div>
      <div className="flex items-center gap-4">
        {!isAuthenticated && <nav className="hidden md:block">
          <a href="#como-funciona" className="text-ink no-underline font-semibold text-[14px] ml-7">Como funciona</a>
          <a href="#diagnostico" className="text-ink no-underline font-semibold text-[14px] ml-7">Diagnóstico</a>
          <a href="#vagas" className="text-ink no-underline font-semibold text-[14px] ml-7">Vagas</a>
        </nav>}
        {isAuthenticated && onProfile && onLogout ? <AccountHeaderActions onProfile={onProfile} onLogout={onLogout} /> : <button onClick={onLogin} className="rounded-full border border-ink/20 bg-transparent px-4 py-2 text-sm font-bold text-ink transition-colors hover:bg-ink hover:text-brand-white cursor-pointer">Entrar</button>}
      </div>
    </header>
  );
}
