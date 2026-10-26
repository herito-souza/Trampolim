export type AccountDestination = 'opportunities' | 'courses' | 'jobs' | 'profile' | 'diagnostic' | 'premium' | 'admin';

interface AccountNavigationProps {
  active: AccountDestination;
  onNavigate: (destination: AccountDestination) => void;
  canManage?: boolean;
}

const links: { destination: AccountDestination; label: string }[] = [
  { destination: 'diagnostic', label: 'Diagnóstico' },
  { destination: 'opportunities', label: 'Oportunidades' },
  { destination: 'courses', label: 'Cursos' },
  { destination: 'jobs', label: 'Vagas' },
  { destination: 'premium', label: 'Premium' },
];

export function AccountNavigation({ active, onNavigate, canManage = false }: AccountNavigationProps) {
  const destinations = canManage ? [...links, { destination: 'admin' as const, label: 'Administração' }] : links;

  return (
    <nav aria-label="Navegação da conta" className="border-b border-white/10 bg-[#0b1421] px-0 py-3">
      <div className="mobile-nav-scroll mx-auto flex max-w-7xl gap-2 overflow-x-auto overscroll-x-contain px-[6vw] pb-1">
        {destinations.map(({ destination, label }) => (
          <button
            key={destination}
            type="button"
            aria-current={active === destination ? 'page' : undefined}
            onClick={() => onNavigate(destination)}
            className={`min-h-11 shrink-0 whitespace-nowrap rounded-full border px-4 py-2 text-sm font-bold transition-colors ${active === destination ? 'border-ink bg-ink text-brand-white' : 'border-ink/15 bg-transparent text-ink hover:border-ink/40 hover:bg-paper'}`}
          >
            {label}
          </button>
        ))}
      </div>
    </nav>
  );
}
