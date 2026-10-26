interface AccountHeaderActionsProps {
  onProfile: () => void;
  onLogout: () => void;
}

const buttonClass = 'rounded-full border border-ink/20 bg-transparent px-4 py-2 text-sm font-bold text-ink transition-colors hover:bg-ink hover:text-brand-white';

export function AccountHeaderActions({ onProfile, onLogout }: AccountHeaderActionsProps) {
  return (
    <div className="flex items-center gap-2">
      <button type="button" onClick={onProfile} className={buttonClass}>Meu perfil</button>
      <button type="button" onClick={onLogout} className={buttonClass}>Sair</button>
    </div>
  );
}
